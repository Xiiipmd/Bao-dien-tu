package ptit.tmdt.lop6nhom7.baodientu.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.AdminOverviewStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.AdminStatDetailDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.AdminTopStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.AuthorStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.AuthorStatPointDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.StatOptionDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.TopicStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.TopArticleStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;
import ptit.tmdt.lop6nhom7.baodientu.enums.SubscriptionTargetType;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
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
        validateDateRange(start, end);

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
                endInstant);
        long totalViews = articleViewRepo.countByArticleAuthorIdAndArticleStatusAndViewedAtBetween(
                authorId,
                ArticleStatus.PUBLISHED,
                startInstant,
                endInstant);
        long totalFollowers = subscriptionRepo.countByTargetTypeAndTargetId(
                SubscriptionTargetType.AUTHOR,
                authorId);

        List<AuthorStatPointDTO> chart = articleViewRepo
                .findViewRevenuePoints(
                        authorId,
                        ArticleStatus.PUBLISHED.name(),
                        startInstant,
                        endInstant,
                        periodUnit.getMysqlDateFormat(),
                        FREE_REVENUE_PER_VIEW,
                        VIP_REVENUE_PER_VIEW)
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
                        PageRequest.of(0, TOP_ARTICLE_LIMIT))
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

    @Transactional(readOnly = true)
    public AdminOverviewStatDTO getAdminOverviewStat(
            Integer authorId,
            Integer categoryId,
            String startDate,
            String endDate,
            String groupBy) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        StatPeriodUnit periodUnit = parsePeriodUnit(groupBy);
        validateDateRange(start, end);

        Integer normalizedAuthorId = normalizeFilterId(authorId);
        Integer normalizedCategoryId = normalizeFilterId(categoryId);
        Instant startInstant = start.atStartOfDay(ZONE_ID).toInstant();
        Instant endInstant = end.atTime(LocalTime.MAX).atZone(ZONE_ID).toInstant();

        List<AuthorStatPointDTO> chart = articleViewRepo
                .findAdminViewRevenuePoints(
                        normalizedAuthorId,
                        normalizedCategoryId,
                        ArticleStatus.PUBLISHED.name(),
                        startInstant,
                        endInstant,
                        periodUnit.getMysqlDateFormat(),
                        FREE_REVENUE_PER_VIEW,
                        VIP_REVENUE_PER_VIEW)
                .stream()
                .map(item -> AuthorStatPointDTO.builder()
                        .date(item.getPeriod())
                        .views(safeLong(item.getViews()))
                        .revenue(safeLong(item.getRevenue()))
                        .build())
                .toList();

        List<AdminStatDetailDTO> details = articleViewRepo
                .findAdminStatDetails(
                        normalizedAuthorId,
                        normalizedCategoryId,
                        ArticleStatus.PUBLISHED.name(),
                        startInstant,
                        endInstant,
                        periodUnit.getMysqlDateFormat(),
                        FREE_REVENUE_PER_VIEW,
                        VIP_REVENUE_PER_VIEW)
                .stream()
                .map(item -> AdminStatDetailDTO.builder()
                        .period(item.getPeriod())
                        .authorId(item.getAuthorId())
                        .authorName(item.getAuthorName())
                        .categoryId(item.getCategoryId())
                        .categoryName(item.getCategoryName())
                        .articles(safeLong(item.getArticles()))
                        .views(safeLong(item.getViews()))
                        .revenue(safeLong(item.getRevenue()))
                        .build())
                .toList();

        long totalArticles = articleRepo.countPublishedArticlesForAdminStats(
                normalizedAuthorId,
                normalizedCategoryId,
                ArticleStatus.PUBLISHED,
                startInstant,
                endInstant);
        long totalViews = chart.stream().mapToLong(point -> safeLong(point.getViews())).sum();
        long totalRevenue = chart.stream().mapToLong(point -> safeLong(point.getRevenue())).sum();

        return AdminOverviewStatDTO.builder()
                .totalArticles(totalArticles)
                .totalViews(totalViews)
                .totalRevenue(totalRevenue)
                .periodUnit(periodUnit.getApiValue())
                .freeViewPrice(FREE_REVENUE_PER_VIEW)
                .vipViewPrice(VIP_REVENUE_PER_VIEW)
                .chart(chart)
                .details(details)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminTopStatDTO> getAdminTopStats(
            String targetType,
            String sortBy,
            String sortDirection,
            String startDate,
            String endDate,
            Integer limit) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        validateDateRange(start, end);

        AdminTopTarget target = parseAdminTopTarget(targetType);
        AdminTopSort topSort = parseAdminTopSort(sortBy);
        AdminTopDirection direction = parseAdminTopDirection(sortDirection);
        int normalizedLimit = normalizeTopLimit(limit);
        Instant startInstant = start.atStartOfDay(ZONE_ID).toInstant();
        Instant endInstant = end.atTime(LocalTime.MAX).atZone(ZONE_ID).toInstant();

        List<ArticleViewRepo.AdminTopStat> rows = target == AdminTopTarget.AUTHOR
                ? articleViewRepo.findTopAuthorsForAdmin(
                        ArticleStatus.PUBLISHED.name(),
                        startInstant,
                        endInstant,
                        FREE_REVENUE_PER_VIEW,
                        VIP_REVENUE_PER_VIEW)
                : articleViewRepo.findTopCategoriesForAdmin(
                        ArticleStatus.PUBLISHED.name(),
                        startInstant,
                        endInstant,
                        FREE_REVENUE_PER_VIEW,
                        VIP_REVENUE_PER_VIEW);

        Comparator<ArticleViewRepo.AdminTopStat> comparator = buildAdminTopComparator(topSort, direction);

        List<ArticleViewRepo.AdminTopStat> sortedRows = rows.stream()
                .sorted(comparator)
                .limit(normalizedLimit)
                .toList();

        return java.util.stream.IntStream.range(0, sortedRows.size())
                .mapToObj(index -> toAdminTopStatDTO(sortedRows.get(index), target, index + 1))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StatOptionDTO> getAuthorOptions() {
        return userRepo.findByRoleOrderByFullNameAsc(UserRole.AUTHOR)
                .stream()
                .map(user -> StatOptionDTO.builder()
                        .id(user.getId())
                        .name(user.getFullName())
                        .build())
                .toList();
    }

    private List<TopicStatDTO> buildTopicStats(Integer authorId, Instant startInstant, Instant endInstant) {
        Map<Integer, TopicStatAccumulator> topicMap = new HashMap<>();

        articleRepo.countPublishedArticlesByTopic(
                authorId,
                ArticleStatus.PUBLISHED,
                startInstant,
                endInstant)
                .forEach(item -> {
                    TopicStatAccumulator accumulator = topicMap.computeIfAbsent(
                            item.getCategoryId(),
                            categoryId -> new TopicStatAccumulator(categoryId, item.getCategoryName()));
                    accumulator.categoryName = item.getCategoryName();
                    accumulator.articles = safeLong(item.getArticles());
                });

        articleViewRepo.findTopicViewRevenue(
                authorId,
                ArticleStatus.PUBLISHED.name(),
                startInstant,
                endInstant,
                FREE_REVENUE_PER_VIEW,
                VIP_REVENUE_PER_VIEW)
                .forEach(item -> {
                    TopicStatAccumulator accumulator = topicMap.computeIfAbsent(
                            item.getCategoryId(),
                            categoryId -> new TopicStatAccumulator(categoryId, item.getCategoryName()));
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
                                .thenComparing(item -> item.categoryName == null ? "" : item.categoryName))
                .map(this::toTopicStatDTO)
                .toList();
    }

    private TopicStatDTO toTopicStatDTO(TopicStatAccumulator item) {
        long followers = subscriptionRepo.countByTargetTypeAndTargetId(
                SubscriptionTargetType.CATEGORY,
                item.categoryId);

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

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new BadRequestException("Khoang thoi gian khong hop le");
        }
    }

    private Integer normalizeFilterId(Integer value) {
        return value == null || value <= 0 ? null : value;
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

    private AdminTopTarget parseAdminTopTarget(String rawTargetType) {
        if (rawTargetType == null || rawTargetType.isBlank()) {
            return AdminTopTarget.AUTHOR;
        }

        return switch (rawTargetType.trim().toLowerCase(Locale.ROOT)) {
            case "author", "authors", "tac-gia", "tacgia" -> AdminTopTarget.AUTHOR;
            case "category", "categories", "topic", "topics", "chu-de", "chude" -> AdminTopTarget.CATEGORY;
            default -> throw new BadRequestException("Doi tuong xep hang chi chap nhan author hoac category");
        };
    }

    private AdminTopSort parseAdminTopSort(String rawSortBy) {
        if (rawSortBy == null || rawSortBy.isBlank()) {
            return AdminTopSort.REVENUE;
        }

        return switch (rawSortBy.trim().toLowerCase(Locale.ROOT)) {
            case "revenue", "doanh-thu", "doanhthu" -> AdminTopSort.REVENUE;
            case "views", "view", "luot-xem", "luotxem" -> AdminTopSort.VIEWS;
            default -> throw new BadRequestException("Chi so xep hang chi chap nhan revenue hoac views");
        };
    }

    private AdminTopDirection parseAdminTopDirection(String rawSortDirection) {
        if (rawSortDirection == null || rawSortDirection.isBlank()) {
            return AdminTopDirection.DESC;
        }

        return switch (rawSortDirection.trim().toLowerCase(Locale.ROOT)) {
            case "asc", "ascending", "low", "lowest", "thap-nhat", "thapnhat" -> AdminTopDirection.ASC;
            case "desc", "descending", "high", "highest", "cao-nhat", "caonhat" -> AdminTopDirection.DESC;
            default -> throw new BadRequestException("Thu tu xep hang chi chap nhan asc hoac desc");
        };
    }

    private Comparator<ArticleViewRepo.AdminTopStat> buildAdminTopComparator(
            AdminTopSort topSort,
            AdminTopDirection direction) {
        Comparator<ArticleViewRepo.AdminTopStat> viewsComparator = Comparator
                .comparingLong((ArticleViewRepo.AdminTopStat item) -> safeLong(item.getViews()));
        Comparator<ArticleViewRepo.AdminTopStat> revenueComparator = Comparator
                .comparingLong((ArticleViewRepo.AdminTopStat item) -> safeLong(item.getRevenue()));

        if (direction == AdminTopDirection.DESC) {
            viewsComparator = viewsComparator.reversed();
            revenueComparator = revenueComparator.reversed();
        }

        Comparator<ArticleViewRepo.AdminTopStat> primary = topSort == AdminTopSort.VIEWS
                ? viewsComparator
                : revenueComparator;
        Comparator<ArticleViewRepo.AdminTopStat> secondary = topSort == AdminTopSort.VIEWS
                ? revenueComparator
                : viewsComparator;

        return primary
                .thenComparing(secondary)
                .thenComparing(item -> item.getTargetName() == null ? "" : item.getTargetName());
    }

    private int normalizeTopLimit(Integer limit) {
        if (limit == null) {
            return 10;
        }
        if (limit <= 0 || limit > 100) {
            throw new BadRequestException("So luong top phai trong khoang 1 den 100");
        }
        return limit;
    }

    private AdminTopStatDTO toAdminTopStatDTO(
            ArticleViewRepo.AdminTopStat item,
            AdminTopTarget target,
            int rank) {
        return AdminTopStatDTO.builder()
                .rank(rank)
                .targetType(target.getApiValue())
                .targetId(item.getTargetId())
                .targetName(item.getTargetName())
                .articles(safeLong(item.getArticles()))
                .views(safeLong(item.getViews()))
                .revenue(safeLong(item.getRevenue()))
                .build();
    }

    private long revenueRate(ArticleType articleType) {
        return articleType == ArticleType.VIP ? VIP_REVENUE_PER_VIEW : FREE_REVENUE_PER_VIEW;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private int safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private int safeInt(Long value) {
        return value == null ? 0 : safeInt(value.longValue());
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

    private enum AdminTopTarget {
        AUTHOR("author"),
        CATEGORY("category");

        private final String apiValue;

        AdminTopTarget(String apiValue) {
            this.apiValue = apiValue;
        }

        public String getApiValue() {
            return apiValue;
        }
    }

    private enum AdminTopSort {
        REVENUE,
        VIEWS
    }

    private enum AdminTopDirection {
        ASC,
        DESC
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

    private List<Integer> buildViewSeries(
            StatPeriodUnit periodUnit,
            Instant startInstant,
            Instant endInstant,
            Map<String, Long> viewsByPeriod) {
        List<Integer> series = new ArrayList<>();

        switch (periodUnit) {
            case HOUR -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
                ZonedDateTime cursor = startInstant.atZone(ZONE_ID).truncatedTo(ChronoUnit.HOURS);
                ZonedDateTime endCursor = endInstant.atZone(ZONE_ID).truncatedTo(ChronoUnit.HOURS);
                while (!cursor.isAfter(endCursor)) {
                    series.add(safeInt(viewsByPeriod.get(cursor.format(formatter))));
                    cursor = cursor.plusHours(1);
                }
            }
            case DAY -> {
                LocalDate cursor = startInstant.atZone(ZONE_ID).toLocalDate();
                LocalDate endCursor = endInstant.atZone(ZONE_ID).toLocalDate();
                while (!cursor.isAfter(endCursor)) {
                    series.add(safeInt(viewsByPeriod.get(cursor.format(DATE_FORMATTER))));
                    cursor = cursor.plusDays(1);
                }
            }
            case MONTH -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
                YearMonth cursor = YearMonth.from(startInstant.atZone(ZONE_ID));
                YearMonth endCursor = YearMonth.from(endInstant.atZone(ZONE_ID));
                while (!cursor.isAfter(endCursor)) {
                    series.add(safeInt(viewsByPeriod.get(cursor.format(formatter))));
                    cursor = cursor.plusMonths(1);
                }
            }
        }

        return series;
    }

    private void validateHourRange(Instant startInstant, Instant endInstant, StatPeriodUnit periodUnit) {
        if (periodUnit == StatPeriodUnit.HOUR) {
            long days = ChronoUnit.DAYS.between(startInstant, endInstant);
            if (days > 365) {
                throw new BadRequestException("Khoang thoi gian qua 1 nam, vui long chon theo ngay");
            }
        }
    }

    @Transactional(readOnly = true)
    public ArticleStatDTO calcArticleStat(int articleId, Instant startDate, Instant endDate, String granularity) {
        var article = articleRepo.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay bai bao"));

        User currentUser = getCurrentUser();
        if (currentUser.getRole() == UserRole.AUTHOR) {
            if (!Objects.equals(currentUser.getId(), article.getAuthor().getId())) {
                throw new ForbiddenException("Chi duoc xem thong ke bai bao cua chinh minh");
            }
        } else if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Khong co quyen xem thong ke");
        }

        Instant startInstant;
        Instant endInstant;
        if (startDate == null || endDate == null) {
            startInstant = article.getCreatedAt();
            endInstant = Instant.now();
        } else {
            startInstant = startDate;
            endInstant = endDate;
        }

        if (startInstant == null || endInstant == null) {
            throw new BadRequestException("Khong the xac dinh khoang thoi gian");
        }

        if (startInstant.isAfter(endInstant)) {
            throw new BadRequestException("Khoang thoi gian khong hop le");
        }

        StatPeriodUnit periodUnit = parsePeriodUnit(granularity);
        validateHourRange(startInstant, endInstant, periodUnit);

        List<ArticleViewRepo.ArticleViewPoint> rows = articleViewRepo.findArticleViewPoints(
                articleId,
                startInstant,
                endInstant,
                periodUnit.getMysqlDateFormat());

        Map<String, Long> viewsByPeriod = new HashMap<>();
        for (ArticleViewRepo.ArticleViewPoint item : rows) {
            viewsByPeriod.put(item.getPeriod(), safeLong(item.getViews()));
        }

        long totalViews = rows.stream()
                .mapToLong(item -> safeLong(item.getViews()))
                .sum();

        List<Integer> viewSeries = totalViews == 0
                ? List.of()
                : buildViewSeries(periodUnit, startInstant, endInstant, viewsByPeriod);

        long estimatedEarning = ArticleType.VIP.equals(article.getType())
                ? totalViews * VIP_REVENUE_PER_VIEW
                : 0L;

        ArticleStatDTO response = new ArticleStatDTO();
        response.setViews(safeInt(totalViews));
        response.setEstimatedEarning(safeInt(estimatedEarning));
        response.setViewsByLevelOfGranularity(viewSeries);
        return response;
    }
}
