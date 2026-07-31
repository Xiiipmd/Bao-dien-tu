package ptit.tmdt.lop6nhom7.baodientu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.tmdt.lop6nhom7.baodientu.dto.NewsNotificationResponse;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.entity.NewsNotification;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.NotificationType;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.NewsNotificationRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsNotificationService {
  private final NewsNotificationRepo notificationRepo;
  private final UserRepo userRepo;

  @Value("${app.notifications.hot-view-threshold:1000}")
  private int hotViewThreshold;

  @Transactional
  public int notifyNewArticle(Article article) {
    return createNotifications(article, NotificationType.NEW_ARTICLE);
  }

  @Transactional
  public int notifyIfHot(Article article) {
    int views = article.getViewCount() == null ? 0 : article.getViewCount();
    if (views < hotViewThreshold) {
      return 0;
    }
    int readerNotifications = createNotifications(article, NotificationType.HOT_ARTICLE);
    int authorNotifications = createAuthorNotification(
        article,
        NotificationType.AUTHOR_ARTICLE_HOT,
        "Bài viết của bạn đang được quan tâm",
        "Bài viết đã đạt " + views + " lượt xem: " + article.getTitle()
    );
    return readerNotifications + authorNotifications;
  }

  @Transactional
  public int notifyAuthorAboutDecision(Article article, boolean approved) {
    NotificationType type = approved
        ? NotificationType.ARTICLE_APPROVED
        : NotificationType.ARTICLE_REJECTED;
    String title = approved ? "Bài viết đã được duyệt" : "Bài viết cần chỉnh sửa";
    String message = approved
        ? "Bài viết đã được xuất bản: " + article.getTitle()
        : "Bài viết chưa được duyệt: " + article.getTitle()
            + ". Lý do: "
            + (article.getRejectionReason() == null ? "Chưa có lý do cụ thể" : article.getRejectionReason());
    return createAuthorNotification(article, type, title, message);
  }

  @Transactional
  public int notifyModeratorsAboutSubmission(Article article, boolean resubmitted) {
    List<User> recipients = userRepo.findModerationNotificationRecipients(
        List.of(UserRole.ADMIN, UserRole.CENSOR),
        UserStatus.LOCKED
    );
    if (recipients.isEmpty()) {
      return 0;
    }

    Instant now = Instant.now();
    String title = resubmitted ? "Bài viết đã được gửi lại" : "Có bài viết mới chờ duyệt";
    String message = article.getAuthor().getFullName() + " gửi bài: " + article.getTitle();
    List<NewsNotification> notifications = new ArrayList<>();

    for (User recipient : recipients) {
      NewsNotification notification = notificationRepo
          .findByUserIdAndArticleIdAndType(
              recipient.getId(),
              article.getId(),
              NotificationType.ADMIN_REVIEW_REQUIRED
          )
          .orElseGet(() -> {
            NewsNotification newNotification = new NewsNotification();
            newNotification.setUser(recipient);
            newNotification.setArticle(article);
            newNotification.setType(NotificationType.ADMIN_REVIEW_REQUIRED);
            return newNotification;
          });

      notification.setTitle(title);
      notification.setMessage(message);
      notification.setRead(false);
      notification.setCreatedAt(now);
      notifications.add(notification);
    }

    notificationRepo.saveAll(notifications);
    return notifications.size();
  }

  @Transactional
  public void markModerationNotificationsHandled(Integer articleId) {
    List<NewsNotification> notifications = notificationRepo.findByArticleIdAndTypeAndReadFalse(
        articleId,
        NotificationType.ADMIN_REVIEW_REQUIRED
    );
    notifications.forEach(notification -> notification.setRead(true));
    notificationRepo.saveAll(notifications);
  }

  @Transactional(readOnly = true)
  public List<NewsNotificationResponse> getNotifications(Integer userId) {
    return notificationRepo.findTop30ByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public long getUnreadCount(Integer userId) {
    return notificationRepo.countByUserIdAndReadFalse(userId);
  }

  @Transactional
  public void markRead(Integer userId, Long notificationId) {
    NewsNotification notification = notificationRepo.findByIdAndUserId(notificationId, userId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo"));
    notification.setRead(true);
    notificationRepo.save(notification);
  }

  @Transactional
  public void markAllRead(Integer userId) {
    List<NewsNotification> notifications = notificationRepo.findByUserIdAndReadFalse(userId);
    notifications.forEach(notification -> notification.setRead(true));
    notificationRepo.saveAll(notifications);
  }

  private int createNotifications(Article article, NotificationType type) {
    List<User> recipients = userRepo.findNewsNotificationRecipients(
        article.getCategory(),
        UserStatus.LOCKED,
        article.getAuthor().getId()
    );
    if (recipients.isEmpty()) {
      return 0;
    }

    List<Integer> recipientIds = recipients.stream().map(User::getId).toList();
    java.util.Set<Integer> existingRecipientIds = notificationRepo.findExistingRecipientIds(
        article.getId(),
        type,
        recipientIds
    );

    List<NewsNotification> notifications = new ArrayList<>();
    for (User user : recipients) {
      if (existingRecipientIds.contains(user.getId())) {
        continue;
      }

      NewsNotification notification = new NewsNotification();
      notification.setUser(user);
      notification.setArticle(article);
      notification.setType(type);
      notification.setTitle(type == NotificationType.HOT_ARTICLE ? "Tin đang hot" : "Tin mới dành cho bạn");
      notification.setMessage(article.getTitle());
      notification.setCreatedAt(Instant.now());
      notifications.add(notification);
    }
    notificationRepo.saveAll(notifications);
    return notifications.size();
  }

  private int createAuthorNotification(
      Article article,
      NotificationType type,
      String title,
      String message
  ) {
    User author = article.getAuthor();
    if (author.getStatus() == UserStatus.LOCKED
        || Boolean.FALSE.equals(author.getPushNotificationsEnabled())
        || notificationRepo.existsByUserIdAndArticleIdAndType(author.getId(), article.getId(), type)) {
      return 0;
    }

    NewsNotification notification = new NewsNotification();
    notification.setUser(author);
    notification.setArticle(article);
    notification.setType(type);
    notification.setTitle(title);
    notification.setMessage(message);
    notification.setCreatedAt(Instant.now());
    notificationRepo.save(notification);
    return 1;
  }

  private NewsNotificationResponse toResponse(NewsNotification notification) {
    Article article = notification.getArticle();
    return new NewsNotificationResponse(
        notification.getId(),
        article.getId(),
        article.getCoverImage(),
        article.getCategory().getName(),
        article.getType(),
        notification.getType(),
        notification.getTitle(),
        notification.getMessage(),
        notification.isRead(),
        notification.getCreatedAt()
    );
  }
}
