package ptit.tmdt.lop6nhom7.baodientu.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleUpdateRequest;
import ptit.tmdt.lop6nhom7.baodientu.service.StaffArticleService;


@RestController
@RequestMapping("/api/staff/articles")
@RequiredArgsConstructor
@Slf4j
public class StaffArticleController {

    private final StaffArticleService staffArticleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR')")
    public ResponseEntity<List<ArticleDTO>> getManageableArticles(
            @RequestParam(value = "q", required = false) String keyword) {
        log.info("Fetching manageable articles with keyword={}", keyword);
        return ResponseEntity.ok(staffArticleService.getManageableArticles(keyword));
    }

    @GetMapping("/{articleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR')")
    public ResponseEntity<ArticleDTO> getArticleDetail(@PathVariable Integer articleId) {
        log.info("Fetching editable article detail with id={}", articleId);
        return ResponseEntity.ok(staffArticleService.getArticleDetail(articleId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<ArticleDTO> createNewArticle(@Valid @RequestBody ArticleUpdateRequest request) {
        log.info("Creating and submitting a new article with title: {}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(staffArticleService.createArticle(request, true));
    }

    @PostMapping("/drafts")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<ArticleDTO> createDraft(@Valid @RequestBody ArticleUpdateRequest request) {
        log.info("Creating an article draft with title: {}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(staffArticleService.createArticle(request, false));
    }

    @PostMapping("/{articleId}/submit")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<ArticleDTO> submitArticle(@PathVariable Integer articleId) {
        log.info("Submitting article with id={}", articleId);
        return ResponseEntity.ok(staffArticleService.submitArticle(articleId));
    }

    @PutMapping("/{articleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR')")
    public ResponseEntity<ArticleDTO> updateArticle(
            @PathVariable Integer articleId,
            @Valid @RequestBody ArticleUpdateRequest request) {
        log.info("Updating article with id={}", articleId);
        return ResponseEntity.ok(staffArticleService.updateArticle(articleId, request));
    }
    
}
