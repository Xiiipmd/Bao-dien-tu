package ptit.tmdt.lop6nhom7.baodientu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleCommentResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.CreateArticleCommentRequest;
import ptit.tmdt.lop6nhom7.baodientu.service.ArticleCommentService;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleCommentController {
  private final ArticleCommentService articleCommentService;

  @GetMapping("/{articleId}/comments")
  public ResponseEntity<List<ArticleCommentResponse>> getArticleComments(@PathVariable Integer articleId) {
    return ResponseEntity.ok(articleCommentService.getArticleComments(articleId));
  }

  @PostMapping("/{articleId}/comments")
  public ResponseEntity<ArticleCommentResponse> createComment(
      @PathVariable Integer articleId,
      @Valid @RequestBody CreateArticleCommentRequest request) {
    return ResponseEntity.ok(articleCommentService.createComment(articleId, request));
  }
}