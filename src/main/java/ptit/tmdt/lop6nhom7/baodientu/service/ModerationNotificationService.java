package ptit.tmdt.lop6nhom7.baodientu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationNotificationService {
  private final ObjectProvider<JavaMailSender> mailSenderProvider;

  public void notifyAuthorAboutDecision(Article article, boolean approved) {
    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null) {
      log.info("Mail sender is not configured; skipped moderation email for articleId={}", article.getId());
      return;
    }

    String authorEmail = article.getAuthor().getEmail();
    if (authorEmail == null || authorEmail.isBlank()) {
      log.info("Author email is empty; skipped moderation email for articleId={}", article.getId());
      return;
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(authorEmail);
    message.setSubject(approved
        ? "Bai viet da duoc duyet: " + article.getTitle()
        : "Bai viet bi tu choi: " + article.getTitle());
    message.setText(approved
        ? """
          Xin chao %s,

          Bai viet cua ban da duoc duyet va xuat ban tren he thong:
          %s

          Vui long truy cap website de kiem tra noi dung da xuat ban.
          """.formatted(article.getAuthor().getFullName(), article.getTitle())
        : """
          Xin chao %s,

          Bai viet cua ban hien chua du dieu kien xuat ban:
          %s

          Ly do tu choi:
          %s

          Vui long chinh sua va gui lai bai viet.
          """.formatted(
            article.getAuthor().getFullName(),
            article.getTitle(),
            article.getRejectionReason() == null ? "Khong co ly do cu the" : article.getRejectionReason()
        ));

    try {
      mailSender.send(message);
    } catch (MailException ex) {
      log.warn("Could not send moderation email to {} for articleId={}", authorEmail, article.getId(), ex);
    }
  }
}