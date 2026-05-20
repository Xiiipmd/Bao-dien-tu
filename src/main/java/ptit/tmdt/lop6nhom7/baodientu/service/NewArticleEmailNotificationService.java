package ptit.tmdt.lop6nhom7.baodientu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.entity.Subscription;
import ptit.tmdt.lop6nhom7.baodientu.enums.SubscriptionTargetType;
import ptit.tmdt.lop6nhom7.baodientu.repository.SubscriptionRepo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewArticleEmailNotificationService {
  private final SubscriptionRepo subscriptionRepo;
  private final ObjectProvider<JavaMailSender> mailSenderProvider;

  @Transactional(readOnly = true)
  public int notifySubscribersAboutNewArticle(Article article) {
    Map<String, Subscription> recipients = collectRecipients(article);
    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

    if (mailSender == null) {
      log.info("Mail sender is not configured; skipped {} new-article emails for articleId={}",
          recipients.size(), article.getId());
      return 0;
    }

    AtomicInteger sentCount = new AtomicInteger();
    recipients.forEach((email, subscription) -> {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(email);
      message.setSubject("Bao dien tu co bai viet moi: " + article.getTitle());
      message.setText("""
          Xin chao,

          He thong bao dien tu vua co bai viet moi phu hop voi dang ky theo doi cua ban:
          %s

          Vui long truy cap website de doc chi tiet.
          """.formatted(article.getTitle()));
      try {
        mailSender.send(message);
        sentCount.incrementAndGet();
      } catch (MailException ex) {
        log.warn("Could not send new-article email to {} for articleId={}", email, article.getId(), ex);
      }
    });
    return sentCount.get();
  }

  private Map<String, Subscription> collectRecipients(Article article) {
    Map<String, Subscription> recipients = new LinkedHashMap<>();
    addRecipients(recipients, subscriptionRepo.findByTargetTypeAndTargetId(
        SubscriptionTargetType.AUTHOR,
        article.getAuthor().getId()
    ));
    addRecipients(recipients, subscriptionRepo.findByTargetTypeAndTargetId(
        SubscriptionTargetType.CATEGORY,
        article.getCategory().getId()
    ));
    return recipients;
  }

  private void addRecipients(Map<String, Subscription> recipients, List<Subscription> subscriptions) {
    for (Subscription subscription : subscriptions) {
      String email = subscription.getUser().getEmail();
      if (email != null && !email.isBlank()) {
        recipients.putIfAbsent(email, subscription);
      }
    }
  }
}
