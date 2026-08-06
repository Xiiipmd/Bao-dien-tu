package ptit.tmdt.lop6nhom7.baodientu.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticlePreviewResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleReadResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleSearchResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleTranslationRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleTranslationResponse;
import ptit.tmdt.lop6nhom7.baodientu.service.AnonymousReadMeterService;
import ptit.tmdt.lop6nhom7.baodientu.service.ArticleService;


@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {
    private static final String ANONYMOUS_READER_COOKIE = "bdt_reader_key";
    private static final String ANONYMOUS_READER_HEADER = "X-Anonymous-Reader-Key";

    private final ArticleService articleService;
    private final AnonymousReadMeterService anonymousReadMeterService;

    @GetMapping("/{articleId}/preview")
    public ResponseEntity<ArticlePreviewResponse> getVipArticlePreview(@PathVariable int articleId) {
        return ResponseEntity.ok(articleService.getVipArticlePreview(articleId));
    }

    @GetMapping("/{articleId}/read")
    public ResponseEntity<ArticleReadResponse> readArticle(
        @PathVariable int articleId,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        return ResponseEntity.ok(articleService.readArticle(articleId, resolveReaderKey(request, response)));
    }

    @GetMapping("/search")
    public ResponseEntity<Object> searchArticles(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "categoryId", required = false) Integer categoryId,
        @RequestParam(value = "authorId", required = false) Integer authorId,
        @RequestParam(value = "authorName", required = false) String authorName,
        @RequestParam(value = "sourceName", required = false) String sourceName,
        @RequestParam(value = "origin", required = false) ptit.tmdt.lop6nhom7.baodientu.enums.ArticleOrigin origin,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        if (page == null && size == null) {
            return ResponseEntity.ok(articleService.searchArticles(keyword, categoryId, authorId, authorName, sourceName, origin));
        }
        int pageVal = page == null ? 0 : page;
        int sizeVal = size == null ? 15 : size;
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(pageVal, sizeVal);
        return ResponseEntity.ok(articleService.searchArticlesPage(keyword, categoryId, authorId, authorName, sourceName, origin, pageable));
    }

    @GetMapping("/personalized")
    public ResponseEntity<java.util.List<ArticleSearchResponse>> getPersonalizedArticles(
        Authentication authentication
    ) {
        return ResponseEntity.ok(articleService.getPersonalizedArticles((Integer) authentication.getPrincipal()));
    }

    @GetMapping("/home")
    public ResponseEntity<java.util.List<ArticleSearchResponse>> getHomeArticles(
        Authentication authentication
    ) {
        Integer userId = authentication == null ? null : (Integer) authentication.getPrincipal();
        return ResponseEntity.ok(articleService.getHomeArticles(userId));
    }

    @GetMapping("/trending")
    public ResponseEntity<java.util.List<ArticleSearchResponse>> getTrendingArticles() {
        return ResponseEntity.ok(articleService.getTrendingArticles());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('VIP', 'ADMIN')")
    public ResponseEntity<ArticleDTO> getAIArticleSummary(@RequestParam("articleId") int articleId) throws Exception {
        return ResponseEntity.ok(articleService.summarizeArticle(articleId));
    }

    @PostMapping("/translate/en")
    public ResponseEntity<ArticleTranslationResponse> translateArticleToEnglish(
        @Valid @RequestBody ArticleTranslationRequest request
    ) throws Exception {
        return ResponseEntity.ok(articleService.translateArticleToEnglish(request.text()));
    }

    private String resolveReaderKey(HttpServletRequest request, HttpServletResponse response) {
        String headerKey = request.getHeader(ANONYMOUS_READER_HEADER);
        if (headerKey != null && !headerKey.isBlank()) {
            return headerKey.trim();
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ANONYMOUS_READER_COOKIE.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }

        String readerKey = anonymousReadMeterService.createReaderKey();
        Cookie cookie = new Cookie(ANONYMOUS_READER_COOKIE, readerKey);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 365);
        response.addCookie(cookie);
        return readerKey;
    }
}
