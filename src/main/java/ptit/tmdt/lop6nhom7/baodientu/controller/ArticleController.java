package ptit.tmdt.lop6nhom7.baodientu.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticlePreviewResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleReadResponse;
import ptit.tmdt.lop6nhom7.baodientu.service.AnonymousReadMeterService;
import ptit.tmdt.lop6nhom7.baodientu.service.ArticleService;


@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {
    private static final String ANONYMOUS_READER_COOKIE = "bdt_reader_key";

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

    @GetMapping("/summary")
    @PreAuthorize("hasRole('VIP')")
    public ResponseEntity<ArticleDTO> getAIArticleSummary(@RequestParam("articleId") int articleId) throws Exception {
        return ResponseEntity.ok(articleService.summarizeArticle(articleId));
    }

    private String resolveReaderKey(HttpServletRequest request, HttpServletResponse response) {
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
