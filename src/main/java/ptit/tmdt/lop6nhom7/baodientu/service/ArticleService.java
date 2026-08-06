package ptit.tmdt.lop6nhom7.baodientu.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import com.google.genai.Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticlePreviewResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleReadResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleSearchResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleTranslationResponse;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.entity.ArticleView;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.ForbiddenException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleViewRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
    private static final int PREVIEW_PARAGRAPH_LIMIT = 2;
    private static final int PREVIEW_CHARACTER_LIMIT = 700;
    private static final int MONTHLY_FREE_VIP_ARTICLE_LIMIT = 3;
    private static final int HOME_ARTICLE_LIMIT = 12;
    private static final int TRENDING_ARTICLE_LIMIT = 3;
    private static final int TRENDING_PERIOD_DAYS = 3;
    
    private final ArticleRepo articleRepo;
    private final ArticleViewRepo articleViewRepo;
    private final UserRepo userRepo;
    private final AnonymousReadMeterService anonymousReadMeterService;
    private final NewsNotificationService newsNotificationService;
    private final ObjectMapper objectMapper;
    private final Client geminiClient = new Client();
    private final HttpClient translationHttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    private final List<String> geminiModels = List.of(
        "gemini-3.1-pro-preview",
        "gemini-2.5-pro",
        "gemini-3-flash",
        "gemini-2.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash-lite"
    );

    public ArticleDTO summarizeArticle(int articleId) throws Exception {
        // find article text
        Article article = articleRepo.findById(articleId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài báo"));
        // build prompt
        String prompt = """
        Bạn là trợ lý tóm tắt văn bản chuyên nghiệp.

        Hãy đọc bài báo dưới đây và tóm tắt theo cấu trúc sau:
        - **Ý chính**: 1–2 câu nêu chủ đề trọng tâm
        - **Nội dung nổi bật**: 3–5 gạch đầu dòng, mỗi điểm 1 câu
        - **Kết luận / Thông điệp**: 1 câu chốt lại ý nghĩa hoặc tác động

        Yêu cầu:
        - Ngôn ngữ tóm tắt phải khớp với ngôn ngữ của bài báo gốc
        - Không thêm ý kiến cá nhân hay thông tin ngoài bài
        - Độ dài tóm tắt không vượt quá 30%% nội dung gốc

        <article>
        %s
        </article>
        """.formatted(article.getContent());
        String chatResponse = "";
        // call gemini api
        for (String model : geminiModels) {
            try {
                chatResponse = geminiClient.models.generateContent(model, prompt, null).text();
                break;
            }
            catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        if (chatResponse.equals("")) {
            throw new Exception("Không thể tóm tắt bài báo vào lúc này. Hãy thử lại sau");
        }
        return ArticleDTO.builder()
            .authorId(0)
            .categoryId(0)
            .coverImage("dummy")
            .title("AI Summary")
            .sapo("AI summary")
            .content(chatResponse)
            .type(ArticleType.FREE)
            .build();
    }

    public ArticleTranslationResponse translateArticleToEnglish(String articleText) throws Exception {
        String prompt = """
            Translate the Vietnamese news article below into natural, accurate English for text-to-speech.

            Requirements:
            - Translate every part of the supplied text without summarizing or adding information.
            - Preserve names, numbers, dates, quotations, and paragraph order.
            - Return plain English text only. Do not add a title, explanation, Markdown, or code fences.

            <article>
            %s
            </article>
            """.formatted(articleText);

        String geminiApiKey = System.getenv("GEMINI_API_KEY");
        if (geminiApiKey != null && !geminiApiKey.isBlank() && !geminiApiKey.startsWith("dummy-")) {
            for (String model : geminiModels) {
                try {
                    String translatedText = geminiClient.models.generateContent(model, prompt, null).text();
                    if (translatedText != null && !translatedText.isBlank()) {
                        return new ArticleTranslationResponse(translatedText.trim(), "en");
                    }
                } catch (Exception exception) {
                    log.warn("Gemini translation failed with model {}: {}", model, exception.getMessage());
                }
            }
        }

        try {
            return new ArticleTranslationResponse(translateWithMyMemory(articleText), "en");
        } catch (Exception exception) {
            log.error("Fallback translation failed", exception);
            throw new BadRequestException(
                "Không thể dịch bài báo. Hãy cấu hình GEMINI_API_KEY hợp lệ hoặc kiểm tra kết nối Internet của backend"
            );
        }
    }

    private String translateWithMyMemory(String articleText) throws Exception {
        List<String> chunks = splitByUtf8Bytes(articleText, 450);
        StringBuilder translation = new StringBuilder();

        for (String chunk : chunks) {
            String encodedText = URLEncoder.encode(chunk, StandardCharsets.UTF_8);
            URI uri = URI.create(
                "https://api.mymemory.translated.net/get?q=" + encodedText + "&langpair=vi%7Cen&mt=1"
            );
            HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "TMDT-NewsReader/1.0")
                .GET()
                .build();
            HttpResponse<String> response = translationHttpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Translation provider returned HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("responseStatus").asInt(200) != 200) {
                throw new IllegalStateException(root.path("responseDetails").asText("Translation provider rejected request"));
            }
            String translatedChunk = root.path("responseData").path("translatedText").asText("").trim();
            if (translatedChunk.isBlank()) {
                throw new IllegalStateException("Translation provider returned empty text");
            }
            if (!translation.isEmpty()) translation.append(' ');
            translation.append(decodeBasicHtmlEntities(translatedChunk));
        }

        return translation.toString();
    }

    private List<String> splitByUtf8Bytes(String text, int maxBytes) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : text.trim().split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (candidate.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            current.append(word);
        }
        if (!current.isEmpty()) chunks.add(current.toString());
        return chunks;
    }

    private String decodeBasicHtmlEntities(String value) {
        return value
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&");
    }

    public ArticlePreviewResponse getVipArticlePreview(int articleId) {
        Article article = articleRepo.findByIdAndStatus(articleId, ArticleStatus.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài báo đang xuất bản"));

        if (article.getType() != ArticleType.VIP) {
            throw new BadRequestException("Chức năng preview chỉ áp dụng cho bài viết VIP");
        }

        return ArticlePreviewResponse.builder()
            .id(article.getId())
            .title(article.getTitle())
            .sapo(article.getSapo())
            .coverImage(article.getCoverImage())
            .previewContent(buildPreviewContent(article.getContent()))
            .authorName(article.getAuthor().getFullName())
            .categoryName(article.getCategory().getName())
            .type(article.getType())
            .paywallRequired(true)
            .build();
    }

            @Transactional(readOnly = true)
            public List<ArticleSearchResponse> searchArticles(String keyword, Integer categoryId, String authorName) {
            return articleRepo.searchPublishedArticles(
                ArticleStatus.PUBLISHED,
                normalizeQueryParam(keyword),
                categoryId,
                normalizeQueryParam(authorName)
                )
                .stream()
                .map(this::toSearchResponse)
                .toList();
            }

    @Transactional(readOnly = true)
    public List<ArticleSearchResponse> getPersonalizedArticles(Integer userId) {
        User user = userRepo.findWithPreferredCategoriesById(userId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        Set<Integer> preferredCategoryIds = new HashSet<>();
        user.getPreferredCategories().forEach(category -> preferredCategoryIds.add(category.getId()));

        Comparator<Article> personalizedOrder = Comparator
            .comparing((Article article) -> !preferredCategoryIds.contains(article.getCategory().getId()))
            .thenComparing(
                Article::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
            );

        return articleRepo.findPublishedArticlesByRecency(ArticleStatus.PUBLISHED).stream()
            .sorted(personalizedOrder)
            .map(this::toSearchResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ArticleSearchResponse> getHomeArticles(Integer userId) {
        if (userId == null) {
            return articleRepo.findHomeArticles(
                    ArticleStatus.PUBLISHED,
                    PageRequest.of(0, HOME_ARTICLE_LIMIT)
                )
                .stream()
                .map(this::toSearchResponse)
                .toList();
        }

        User user = userRepo.findWithPreferredCategoriesById(userId).orElse(null);
        if (user == null || user.getPreferredCategories().isEmpty()) {
            return articleRepo.findHomeArticles(
                    ArticleStatus.PUBLISHED,
                    PageRequest.of(0, HOME_ARTICLE_LIMIT)
                )
                .stream()
                .map(this::toSearchResponse)
                .toList();
        }

        Set<Integer> categoryIds = user.getPreferredCategories().stream()
            .map(category -> category.getId())
            .collect(java.util.stream.Collectors.toSet());
        List<Article> preferred = articleRepo.findPreferredHomeArticles(
            ArticleStatus.PUBLISHED,
            categoryIds,
            PageRequest.of(0, HOME_ARTICLE_LIMIT)
        );
        int remaining = HOME_ARTICLE_LIMIT - preferred.size();
        if (remaining > 0) {
            preferred.addAll(articleRepo.findNonPreferredHomeArticles(
                ArticleStatus.PUBLISHED,
                categoryIds,
                PageRequest.of(0, remaining)
            ));
        }
        return preferred.stream().map(this::toSearchResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ArticleSearchResponse> getTrendingArticles() {
        Instant endDate = Instant.now();
        Instant startDate = endDate.minus(TRENDING_PERIOD_DAYS, ChronoUnit.DAYS);
        List<Integer> rankedIds = articleViewRepo.findTrendingArticles(
                ArticleStatus.PUBLISHED.name(),
                startDate,
                endDate,
                PageRequest.of(0, TRENDING_ARTICLE_LIMIT)
            )
            .stream()
            .map(ArticleViewRepo.TrendingArticleView::getArticleId)
            .toList();

        if (rankedIds.isEmpty()) {
            return List.of();
        }

        Map<Integer, Article> articlesById = articleRepo.findByIdIn(rankedIds).stream()
            .collect(Collectors.toMap(Article::getId, Function.identity()));

        return rankedIds.stream()
            .map(articlesById::get)
            .filter(java.util.Objects::nonNull)
            .map(this::toSearchResponse)
            .toList();
    }

    @Transactional
    public ArticleReadResponse readArticle(int articleId, String anonymousReaderKey) {
        Article article = articleRepo.findByIdAndStatus(articleId, ArticleStatus.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài báo đang xuất bản"));

        Optional<User> currentUser = getCurrentUser();
        MeteredDecision meteredDecision = MeteredDecision.none();
        boolean vipAccessGranted = false;

        if (article.getType() == ArticleType.VIP) {
            if (currentUser.isEmpty()) {
                throw new ForbiddenException("Vui lòng đăng nhập để sử dụng lượt đọc miễn phí hoặc đăng ký VIP để đọc toàn bộ bài viết này.");
            }

            if (hasActiveVip(currentUser.get())) {
                vipAccessGranted = true;
            } else {
                meteredDecision = resolveMeteredAccess(article, currentUser, anonymousReaderKey);
            }
        }

        recordView(article, currentUser.orElse(null));

        return ArticleReadResponse.builder()
            .id(article.getId())
            .title(article.getTitle())
            .sapo(article.getSapo())
            .content(article.getContent())
            .coverImage(article.getCoverImage())
            .authorName(article.getAuthor().getFullName())
            .authorId(article.getAuthor() != null ? article.getAuthor().getId() : null)
            .categoryName(article.getCategory().getName())
            .type(article.getType())
            .viewCount(article.getViewCount())
            .createdAt(article.getCreatedAt())
            .vipAccessGranted(vipAccessGranted)
            .meteredAccessApplied(meteredDecision.applied())
            .remainingFreeReads(meteredDecision.remainingFreeReads())
            .accessMessage(resolveAccessMessage(article.getType(), vipAccessGranted, meteredDecision))
            .build();
    }

    private String buildPreviewContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        List<String> paragraphs = Arrays.stream(content.split("\\R\\s*\\R"))
            .map(String::trim)
            .filter(part -> !part.isBlank())
            .limit(PREVIEW_PARAGRAPH_LIMIT)
            .collect(Collectors.toList());

        String preview = paragraphs.isEmpty()
            ? content.trim()
            : String.join(System.lineSeparator() + System.lineSeparator(), paragraphs);

        if (preview.length() > PREVIEW_CHARACTER_LIMIT) {
            return preview.substring(0, PREVIEW_CHARACTER_LIMIT).trim() + "...";
        }

        if (!preview.equals(content.trim())) {
            return preview + System.lineSeparator() + System.lineSeparator() + "...";
        }

        return preview;
    }

    private MeteredDecision resolveMeteredAccess(Article article, Optional<User> currentUser, String anonymousReaderKey) {
        if (currentUser.isPresent()) {
            User user = currentUser.get();
            if (user.getStatus() == UserStatus.LOCKED) {
                throw new ForbiddenException("Nguoi dung bi khoa tai khoan");
            }

            Instant startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant now = Instant.now();
            boolean alreadyRead = articleViewRepo.existsByUserIdAndArticleIdAndViewedAtBetween(
                user.getId(),
                article.getId(),
                startOfMonth,
                now
            );
            long usedReads = articleViewRepo.countDistinctArticlesByUserAndTypeWithinPeriod(
                user.getId(),
                ArticleType.VIP,
                startOfMonth,
                now
            );

            if (!alreadyRead && usedReads >= MONTHLY_FREE_VIP_ARTICLE_LIMIT) {
                throw new ForbiddenException("Bạn đã hết lượt đọc miễn phí trong tháng. Hãy đăng ký VIP để tiếp tục đọc không giới hạn.");
            }

            int remaining = alreadyRead
                ? Math.max(0, MONTHLY_FREE_VIP_ARTICLE_LIMIT - (int) usedReads)
                : Math.max(0, MONTHLY_FREE_VIP_ARTICLE_LIMIT - (int) usedReads - 1);
            user.setFreeArticlesLeft(remaining);
            userRepo.save(user);
            return MeteredDecision.metered(remaining, !alreadyRead);
        }

        AnonymousReadMeterService.MeteredAccessResult result = anonymousReadMeterService.consumeRead(
            normalizeAnonymousReaderKey(anonymousReaderKey),
            article.getId()
        );
        if (!result.allowed()) {
            throw new ForbiddenException("Bạn đã hết lượt đọc miễn phí trong tháng. Hãy đăng ký VIP để tiếp tục đọc không giới hạn.");
        }

        return MeteredDecision.metered(result.remainingReads(), result.newlyConsumed());
    }

    private void recordView(Article article, User user) {
        ArticleView articleView = new ArticleView();
        articleView.setArticle(article);
        articleView.setUser(user);
        articleView.setViewedAt(Instant.now());
        articleViewRepo.save(articleView);

        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        articleRepo.save(article);
        newsNotificationService.notifyIfHot(article);
    }

    private Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }

        try {
            Integer userId = Integer.valueOf(authentication.getName());
            return userRepo.findById(userId);
        } catch (NumberFormatException ex) {
            log.debug("Skipping authenticated reader resolution because principal is not numeric: {}", authentication.getName());
            return Optional.empty();
        }
    }

    private boolean hasActiveVip(User user) {
        return user.getStatus() != UserStatus.LOCKED
            && user.getVipExpiryDate() != null
            && user.getVipExpiryDate().isAfter(Instant.now());
    }

    private String resolveAccessMessage(ArticleType articleType, boolean vipAccessGranted, MeteredDecision meteredDecision) {
        if (articleType == ArticleType.FREE) {
            return "Bài báo này không giới hạn lượt đọc.";
        }
        if (vipAccessGranted) {
            return "Bạn đang đọc bài báo với quyền VIP không giới hạn.";
        }
        if (meteredDecision.applied()) {
            return meteredDecision.newlyConsumed()
                ? "Bạn vừa sử dụng 1 lượt đọc miễn phí. Bạn vẫn có thể tiếp tục đọc trong hạn mức tháng này."
                : "Bài viết này đã được tính trong hạn mức miễn phí của bạn trong tháng này.";
        }
        return null;
    }

    private String normalizeAnonymousReaderKey(String anonymousReaderKey) {
        if (anonymousReaderKey == null || anonymousReaderKey.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return anonymousReaderKey;
    }

    private String normalizeQueryParam(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ArticleSearchResponse toSearchResponse(Article article) {
        return ArticleSearchResponse.builder()
            .id(article.getId())
            .title(article.getTitle())
            .sapo(article.getSapo())
            .coverImage(article.getCoverImage())
            .authorId(article.getAuthor() != null ? article.getAuthor().getId() : null)
            .authorName(article.getAuthor() != null ? article.getAuthor().getFullName() : null)
            .categoryName(article.getCategory() != null ? article.getCategory().getName() : null)
            .type(article.getType())
            .createdAt(effectivePublishedAt(article))
            .build();
    }

    private Instant effectivePublishedAt(Article article) {
        return article.getPublishedAt() != null ? article.getPublishedAt() : article.getCreatedAt();
    }

    private record MeteredDecision(boolean applied, Integer remainingFreeReads, boolean newlyConsumed) {
        private static MeteredDecision none() {
            return new MeteredDecision(false, null, false);
        }

        private static MeteredDecision metered(int remainingFreeReads, boolean newlyConsumed) {
            return new MeteredDecision(true, remainingFreeReads, newlyConsumed);
        }
    }

}
