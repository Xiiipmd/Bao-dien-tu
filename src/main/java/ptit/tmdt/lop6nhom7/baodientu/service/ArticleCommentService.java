package ptit.tmdt.lop6nhom7.baodientu.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleCommentResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.CreateArticleCommentRequest;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.entity.Comment;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.CommentRepo;

@Service
@RequiredArgsConstructor
public class ArticleCommentService {
  private final CommentRepo commentRepo;
  private final ArticleRepo articleRepo;
  private final VipAccessService vipAccessService;

  @Transactional(readOnly = true)
  public List<ArticleCommentResponse> getArticleComments(Integer articleId) {
    Article article = findPublishedArticle(articleId);
    return commentRepo.findByArticleIdOrderByCreatedAtAsc(article.getId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public ArticleCommentResponse createComment(Integer articleId, CreateArticleCommentRequest request) {
    Article article = findPublishedArticle(articleId);
    User currentUser = vipAccessService.requireCurrentVipUser();

    Comment comment = new Comment();
    comment.setArticle(article);
    comment.setUser(currentUser);
    comment.setContent(request.getContent().trim());
    comment.setCreatedAt(Instant.now());

    return toResponse(commentRepo.save(comment));
  }

  private Article findPublishedArticle(Integer articleId) {
    return articleRepo.findByIdAndStatus(articleId, ArticleStatus.PUBLISHED)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy bài báo đang xuất bản"));
  }

  private ArticleCommentResponse toResponse(Comment comment) {
    return ArticleCommentResponse.builder()
        .id(comment.getId())
        .articleId(comment.getArticle().getId())
        .userId(comment.getUser().getId())
        .userName(comment.getUser().getFullName())
        .content(comment.getContent())
        .createdAt(comment.getCreatedAt())
        .build();
  }
}