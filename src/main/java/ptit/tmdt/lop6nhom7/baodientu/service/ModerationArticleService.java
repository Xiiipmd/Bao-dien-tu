package ptit.tmdt.lop6nhom7.baodientu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.ModerateArticleDecisionRequest;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationArticleService {
  private final ArticleRepo articleRepo;
  private final NewArticleEmailNotificationService newArticleEmailNotificationService;
  private final ModerationNotificationService moderationNotificationService;

  @Transactional(readOnly = true)
  public List<ArticleDTO> getPendingArticles() {
    return articleRepo.findByStatusOrderByCreatedAtDesc(ArticleStatus.PENDING)
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public ArticleDTO getPendingArticleDetail(Integer articleId) {
    return toDto(getPendingArticle(articleId));
  }

  @Transactional
  public ArticleDTO moderateArticle(Integer articleId, ModerateArticleDecisionRequest request) {
    if (request == null) {
      throw new BadRequestException("Moderation data is required");
    }

    Article article = getPendingArticle(articleId);

    if (request.isApproved()) {
      article.setStatus(ArticleStatus.PUBLISHED);
      article.setRejectionReason(null);
      Article savedArticle = articleRepo.save(article);
      moderationNotificationService.notifyAuthorAboutDecision(savedArticle, true);
      int notifiedSubscribers = newArticleEmailNotificationService.notifySubscribersAboutNewArticle(savedArticle);
      log.info("Approved articleId={} and notified {} subscribers", savedArticle.getId(), notifiedSubscribers);
      return toDto(savedArticle);
    }

    String rejectionReason = request.getRejectionReason() == null ? "" : request.getRejectionReason().trim();
    if (rejectionReason.isBlank()) {
      throw new BadRequestException("Lý do từ chối không được để trống");
    }

    article.setStatus(ArticleStatus.REJECTED);
    article.setRejectionReason(rejectionReason);
    Article savedArticle = articleRepo.save(article);
    moderationNotificationService.notifyAuthorAboutDecision(savedArticle, false);
    log.info("Rejected articleId={} with reason length={}", savedArticle.getId(), rejectionReason.length());
    return toDto(savedArticle);
  }

  private Article getPendingArticle(Integer articleId) {
    return articleRepo.findByIdAndStatus(articleId, ArticleStatus.PENDING)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết đang chờ duyệt"));
  }

  private ArticleDTO toDto(Article article) {
    return ArticleDTO.builder()
        .id(article.getId())
        .authorId(article.getAuthor().getId())
        .authorName(article.getAuthor().getFullName())
        .categoryId(article.getCategory().getId())
        .categoryName(article.getCategory().getName())
        .coverImage(article.getCoverImage())
        .title(article.getTitle())
        .sapo(article.getSapo())
        .content(article.getContent())
        .type(article.getType())
        .status(article.getStatus())
        .rejectionReason(article.getRejectionReason())
        .viewCount(article.getViewCount())
        .createdAt(article.getCreatedAt())
        .build();
  }
}