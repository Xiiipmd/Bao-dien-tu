package ptit.tmdt.lop6nhom7.baodientu.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.AuthorStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.AuthorStatPointDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.TopicStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.TopArticleStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;
import ptit.tmdt.lop6nhom7.baodientu.enums.SubscriptionTargetType;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.ForbiddenException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleViewRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.SubscriptionRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatService {
    private static final long FREE_REVENUE_PER_VIEW = 200L;
    private static final long VIP_REVENUE_PER_VIEW = 500L;
    private static final int TOP_ARTICLE_LIMIT = 5;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final ArticleRepo articleRepo;
    private final ArticleViewRepo articleViewRepo;
    private final SubscriptionRepo subscriptionRepo;
    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    public AuthorStatDTO getAuthorStat(int authorId, String startDate, String endDate, String groupBy) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        StatPeriodUnit periodUnit = parsePeriodUnit(groupBy);
        if (start.isAfter(end)) {
            throw new BadRequestException("Khoang thoi gian khong hop le");
        }

        User currentUser = getCurrentUser();
        if (!Objects.equals(currentUser.getId(), authorId)) {
            throw new ForbiddenException("Chi duoc xem thong ke cua chinh minh");
        }

        Instant startInstant = start.atStartOfDay(ZONE_ID).toInstant();
        Instant endInstant = end.atTime(LocalTime.MAX).atZone(ZONE_ID).toInstant();

        long totalArticles = articleRepo.countByAuthorIdAndStatusAndCreatedAtBetween(
            authorId,
            ArticleStatus.PUBLISHED,
            startInstant,
            endInstant
        );
        long totalViews = articleViewRepo.countByArticleAuthorIdAndArticleStatusAndViewedAtBetween(
            authorId,
            ArticleStatus.PUBLISHED,
            startInstant,
            endInstant
        );
        long totalFollowers = subscriptionRepo.countByTargetTypeAndTargetId(
            SubscriptionTargetType.AUTHOR,
            authorId
        );

        List<AuthorStatPointDTO> chart = articleViewRepo
            .findViewRevenuePoints(
                authorId,
                ArticleStatus.PUBLISHED.name(),
                startInstant,
                endInstant,
                periodUnit.getMysqlDateFormat(),
                FREE_REVENUE_PER_VIEW,
                VIP_REVENUE_PER_VIEW
            )
            .stream()
            .map(item -> AuthorStatPointDTO.builder()
                .date(item.getPeriod())
                .views(safeLong(item.getViews()))
                .revenue(safeLong(item.getRevenue()))
                .build())
            .toList();

        List<TopicStatDTO> topicStats = buildTopicStats(authorId, startInstant, endInstant);
        long totalRevenue = topicStats.stream()
            .mapToLong(item -> safeLong(item.getRevenue()))
            .sum();

        List<TopArticleStatDTO> topArticles = articleViewRepo
            .findTopArticlesByAuthor(
                authorId,
                ArticleStatus.PUBLISHED,
                startInstant,
                endInstant,
                PageRequest.of(0, TOP_ARTICLE_LIMIT)
            )
            .stream()
            .map(item -> TopArticleStatDTO.builder()
                .articleId(item.getArticleId())
                .title(item.getTitle())
                .publishedAt(item.getCreatedAt())
                .articleType(item.getArticleType() == null ? null : item.getArticleType().name())
                .views(safeLong(item.getViews()))
                .revenue(safeLong(item.getViews()) * revenueRate(item.getArticleType()))
                .build())
            .toList();

        return AuthorStatDTO.builder()
            .totalArticles(totalArticles)
            .totalViews(totalViews)
            .totalRevenue(totalRevenue)
            .totalFollowers(totalFollowers)
            .periodUnit(periodUnit.getApiValue())
            .freeViewPrice(FREE_REVENUE_PER_VIEW)
            .vipViewPrice(VIP_REVENUE_PER_VIEW)
            .chart(chart)
            .topArticles(topArticles)
            .topicStats(topicStats)
            .build();
    }

    private List<TopicStatDTO> buildTopicStats(Integer authorId, Instant startInstant, Instant endInstant) {
        Map<Integer, TopicStatAccumulator> topicMap = new HashMap<>();

        articleRepo.countPublishedArticlesByTopic(
                authorId,
                ArticleStatus.PUBLISHED,
                startInstant,
                endInstant
            )
            .forEach(item -> {
                TopicStatAccumulator accumulator = topicMap.computeIfAbsent(
                    item.getCategoryId(),
                    categoryId -> new TopicStatAccumulator(categoryId, item.getCategoryName())
                );
                accumulator.categoryName = item.getCategoryName();
                accumulator.articles = safeLong(item.getArticles());
            });

        articleViewRepo.findTopicViewRevenue(
                authorId,
                ArticleStatus.PUBLISHED.name(),
                startInstant,
                endInstant,
                FREE_REVENUE_PER_VIEW,
                VIP_REVENUE_PER_VIEW
            )
            .forEach(item -> {
                TopicStatAccumulator accumulator = topicMap.computeIfAbsent(
                    item.getCategoryId(),
                    categoryId -> new TopicStatAccumulator(categoryId, item.getCategoryName())
                );
                accumulator.categoryName = item.getCategoryName();
                accumulator.views = safeLong(item.getViews());
                accumulator.freeViews = safeLong(item.getFreeViews());
                accumulator.vipViews = safeLong(item.getVipViews());
                accumulator.revenue = safeLong(item.getRevenue());
            });

        return topicMap.values()
            .stream()
            .sorted(
                Comparator.comparingLong(TopicStatAccumulator::getRevenue).reversed()
                    .thenComparing(Comparator.comparingLong(TopicStatAccumulator::getViews).reversed())
                    .thenComparing(item -> item.categoryName == null ? "" : item.categoryName)
            )
            .map(this::toTopicStatDTO)
            .toList();
    }

    private TopicStatDTO toTopicStatDTO(TopicStatAccumulator item) {
        long followers = subscriptionRepo.countByTargetTypeAndTargetId(
            SubscriptionTargetType.CATEGORY,
            item.categoryId
        );

        return TopicStatDTO.builder()
            .categoryId(item.categoryId)
            .categoryName(item.categoryName)
            .articles(item.articles)
            .followers(followers)
            .views(item.views)
            .freeViews(item.freeViews)
            .vipViews(item.vipViews)
            .revenue(item.revenue)
            .build();
    }

    private LocalDate parseDate(String dateValue) {
        if (dateValue == null || dateValue.isBlank()) {
            throw new BadRequestException("Ngay bat dau va ket thuc la bat buoc");
        }
        try {
            return LocalDate.parse(dateValue.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Dinh dang ngay khong hop le, dung yyyyMMdd");
        }
    }

    private StatPeriodUnit parsePeriodUnit(String rawGroupBy) {
        if (rawGroupBy == null || rawGroupBy.isBlank()) {
            return StatPeriodUnit.DAY;
        }

        return switch (rawGroupBy.trim().toLowerCase(Locale.ROOT)) {
            case "hour", "gio" -> StatPeriodUnit.HOUR;
            case "day", "ngay" -> StatPeriodUnit.DAY;
            case "month", "thang" -> StatPeriodUnit.MONTH;
            default -> throw new BadRequestException("Don vi thong ke chi chap nhan hour, day hoac month");
        };
    }

    private long revenueRate(ArticleType articleType) {
        return articleType == ArticleType.VIP ? VIP_REVENUE_PER_VIEW : FREE_REVENUE_PER_VIEW;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ForbiddenException("Khong xac dinh duoc nguoi dung hien tai");
        }

        Integer currentUserId;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Integer userId) {
            currentUserId = userId;
        } else {
            currentUserId = Integer.valueOf(authentication.getName());
        }

        return userRepo.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Khong tim thay nguoi dung"));
    }

    private enum StatPeriodUnit {
        HOUR("hour", "%Y%m%d%H"),
        DAY("day", "%Y%m%d"),
        MONTH("month", "%Y%m");

        private final String apiValue;
        private final String mysqlDateFormat;

        StatPeriodUnit(String apiValue, String mysqlDateFormat) {
            this.apiValue = apiValue;
            this.mysqlDateFormat = mysqlDateFormat;
        }

        public String getApiValue() {
            return apiValue;
        }

        public String getMysqlDateFormat() {
            return mysqlDateFormat;
        }
    }

    private static class TopicStatAccumulator {
        private final Integer categoryId;
        private String categoryName;
        private long articles;
        private long views;
        private long freeViews;
        private long vipViews;
        private long revenue;

        private TopicStatAccumulator(Integer categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        private long getViews() {
            return views;
        }

        private long getRevenue() {
            return revenue;
        }
    }
}
