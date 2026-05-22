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
import ptit.tmdt.lop6nhom7.baodientu.exception.ForbiddenException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationArticleService {
  private static final List<ArticleStatus> ADMIN_VISIBILITY_STATUSES = List.of(
      ArticleStatus.PUBLISHED,
      ArticleStatus.HIDDEN
  );

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
  public List<ArticleDTO> getVisibilityManagedArticles(String keyword) {
    String normalizedKeyword = keyword == null ? "" : keyword.trim();
    List<Article> articles;

    if (normalizedKeyword.isBlank()) {
      articles = articleRepo.findByStatusInOrderByCreatedAtDesc(ADMIN_VISIBILITY_STATUSES);
    } else if (normalizedKeyword.matches("\\d+")) {
      Integer articleId = Integer.valueOf(normalizedKeyword);
      articles = articleRepo.findById(articleId)
          .filter(article -> ADMIN_VISIBILITY_STATUSES.contains(article.getStatus()))
          .map(List::of)
          .orElseGet(List::of);
    } else {
      articles = articleRepo.findByStatusInAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
          ADMIN_VISIBILITY_STATUSES,
          normalizedKeyword
      );
    }

    return articles.stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public ArticleDTO getPendingArticleDetail(Integer articleId) {
    return toDto(getPendingArticle(articleId));
  }

  @Transactional(readOnly = true)
  public ArticleDTO getVisibilityManagedArticleDetail(Integer articleId) {
    return toDto(getVisibilityManagedArticle(articleId));
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

  @Transactional
  public ArticleDTO hideArticle(Integer articleId) {
    Article article = getVisibilityManagedArticle(articleId);
    if (article.getStatus() != ArticleStatus.PUBLISHED) {
      throw new BadRequestException("Chỉ có thể ẩn bài viết đang ở trạng thái xuất bản");
    }

    article.setStatus(ArticleStatus.HIDDEN);
    Article savedArticle = articleRepo.save(article);
    return toDto(savedArticle);
  }

  @Transactional
  public ArticleDTO restoreArticle(Integer articleId) {
    Article article = getVisibilityManagedArticle(articleId);
    if (article.getStatus() != ArticleStatus.HIDDEN) {
      throw new BadRequestException("Chỉ có thể hiển thị lại bài viết đang ở trạng thái ẩn");
    }

    article.setStatus(ArticleStatus.PUBLISHED);
    Article savedArticle = articleRepo.save(article);
    return toDto(savedArticle);
  }

  private Article getPendingArticle(Integer articleId) {
    return articleRepo.findByIdAndStatus(articleId, ArticleStatus.PENDING)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết đang chờ duyệt"));
  }

  private Article getVisibilityManagedArticle(Integer articleId) {
    Article article = articleRepo.findById(articleId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));

    if (!ADMIN_VISIBILITY_STATUSES.contains(article.getStatus())) {
      throw new ForbiddenException("Bài viết này không nằm trong luồng ẩn/hiển thị của Admin");
    }

    return article;
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