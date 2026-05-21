package ptit.tmdt.lop6nhom7.baodientu.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
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
import ptit.tmdt.lop6nhom7.baodientu.dto.TopArticleStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.ForbiddenException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleViewRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatService {
    private static final long REVENUE_PER_VIEW = 500L;
    private static final int TOP_ARTICLE_LIMIT = 5;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final ArticleRepo articleRepo;
    private final ArticleViewRepo articleViewRepo;
    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    public AuthorStatDTO getAuthorStat(int authorId, String startDate, String endDate) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
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
        long totalRevenue = totalViews * REVENUE_PER_VIEW;

        List<AuthorStatPointDTO> chart = articleViewRepo
            .findDailyViewCounts(authorId, ArticleStatus.PUBLISHED.name(), startInstant, endInstant)
            .stream()
            .map(item -> {
                long views = item.getViews() == null ? 0L : item.getViews();
                String date = item.getViewDate() == null ? null : item.getViewDate().format(DATE_FORMATTER);
                return AuthorStatPointDTO.builder()
                    .date(date)
                    .views(views)
                    .revenue(views * REVENUE_PER_VIEW)
                    .build();
            })
            .toList();

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
                .views(item.getViews())
                .build())
            .toList();

        return AuthorStatDTO.builder()
            .totalArticles(totalArticles)
            .totalViews(totalViews)
            .totalRevenue(totalRevenue)
            .chart(chart)
            .topArticles(topArticles)
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
}
