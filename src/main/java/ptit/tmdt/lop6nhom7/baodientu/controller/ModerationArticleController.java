package ptit.tmdt.lop6nhom7.baodientu.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.ModerateArticleDecisionRequest;
import ptit.tmdt.lop6nhom7.baodientu.service.ModerationArticleService;

import java.util.List;

@RestController
@RequestMapping("/api/moderation/articles")
@RequiredArgsConstructor
@Slf4j
public class ModerationArticleController {
  private final ModerationArticleService moderationArticleService;

  @GetMapping("/pending")
  @PreAuthorize("hasAnyRole('CENSOR', 'ADMIN')")
  public ResponseEntity<List<ArticleDTO>> getPendingArticles() {
    log.info("Fetching pending articles for moderation");
    return ResponseEntity.ok(moderationArticleService.getPendingArticles());
  }

  @GetMapping("/{articleId}")
  @PreAuthorize("hasAnyRole('CENSOR', 'ADMIN')")
  public ResponseEntity<ArticleDTO> getPendingArticleDetail(@PathVariable Integer articleId) {
    log.info("Fetching pending article detail articleId={}", articleId);
    return ResponseEntity.ok(moderationArticleService.getPendingArticleDetail(articleId));
  }

  @PostMapping("/{articleId}/decision")
  @PreAuthorize("hasAnyRole('CENSOR', 'ADMIN')")
  public ResponseEntity<ArticleDTO> moderateArticle(
      @PathVariable Integer articleId,
      @Valid @RequestBody ModerateArticleDecisionRequest request) {
    log.info("Moderating articleId={} approved={}", articleId, request.isApproved());
    return ResponseEntity.ok(moderationArticleService.moderateArticle(articleId, request));
  }
}