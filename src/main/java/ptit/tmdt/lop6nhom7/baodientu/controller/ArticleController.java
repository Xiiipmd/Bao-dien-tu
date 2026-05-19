package ptit.tmdt.lop6nhom7.baodientu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.service.ArticleService;


@Controller
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('VIP')")
    public ResponseEntity<ArticleDTO> getAIArticleSummary(@RequestParam("articleId") int articleId) throws Exception {
        return ResponseEntity.ok(articleService.summarizeArticle(articleId));
    }
}
