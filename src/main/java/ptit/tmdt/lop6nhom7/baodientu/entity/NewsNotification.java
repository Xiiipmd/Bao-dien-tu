package ptit.tmdt.lop6nhom7.baodientu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import ptit.tmdt.lop6nhom7.baodientu.enums.NotificationType;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
    name = "news_notifications",
    schema = "pthttmdt",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_user_article_type",
        columnNames = {"user_id", "article_id", "type"}
    )
)
public class NewsNotification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30)
  private NotificationType type;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "message", nullable = false, length = 500)
  private String message;

  @ColumnDefault("false")
  @Column(name = "is_read", nullable = false)
  private boolean read;

  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
