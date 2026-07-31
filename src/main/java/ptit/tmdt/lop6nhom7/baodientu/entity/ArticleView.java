package ptit.tmdt.lop6nhom7.baodientu.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "article_views", schema = "pthttmdt")
public class ArticleView {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;
  
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  /**
   * Stable, privacy-safe identity for an anonymous reader. Authenticated
   * readers are deduplicated through user_id, so this value stays null.
   */
  @Column(name = "reader_identity", length = 80)
  private String readerIdentity;
  
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "viewed_at")
  private Instant viewedAt;
  
}
