package ptit.tmdt.lop6nhom7.baodientu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.event.ArticleModerationDecidedEvent;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationNotificationEventListener {
  private final ArticleRepo articleRepo;
  private final ModerationNotificationService moderationNotificationService;
  private final NewArticleEmailNotificationService newArticleEmailNotificationService;
  private final NewsNotificationService newsNotificationService;

  @Async("pushNotificationTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void deliverPushNotifications(ArticleModerationDecidedEvent event) {
    long startedAt = System.currentTimeMillis();
    try {
      Article article = loadArticle(event.articleId());
      newsNotificationService.notifyAuthorAboutDecision(article, event.approved());

      int readerNotifications = 0;
      if (event.approved()) {
        readerNotifications = newsNotificationService.notifyNewArticle(article);
      }

      log.info(
          "Created push notifications for articleId={} in {} ms; readerNotifications={}",
          event.articleId(),
          System.currentTimeMillis() - startedAt,
          readerNotifications
      );
    } catch (Exception ex) {
      log.error("Push notification creation failed for articleId={}", event.articleId(), ex);
    }
  }

  @Async("emailNotificationTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void deliverEmailNotifications(ArticleModerationDecidedEvent event) {
    long startedAt = System.currentTimeMillis();
    try {
      Article article = loadArticle(event.articleId());
      moderationNotificationService.notifyAuthorAboutDecision(article, event.approved());

      int subscriberEmails = 0;
      if (event.approved()) {
        subscriberEmails = newArticleEmailNotificationService.notifySubscribersAboutNewArticle(article);
      }

      log.info(
          "Delivered moderation emails for articleId={} in {} ms; subscriberEmails={}",
          event.articleId(),
          System.currentTimeMillis() - startedAt,
          subscriberEmails
      );
    } catch (Exception ex) {
      log.error("Moderation email delivery failed for articleId={}", event.articleId(), ex);
    }
  }

  private Article loadArticle(Integer articleId) {
    return articleRepo.findWithAuthorAndCategoryById(articleId)
        .orElseThrow(() -> new IllegalStateException("Article no longer exists: " + articleId));
  }
}
