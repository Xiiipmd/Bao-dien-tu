package ptit.tmdt.lop6nhom7.baodientu.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "articles", schema = "pthttmdt")
public class Article {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;
  
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;
  
  @Size(max = 500)
  @NotNull
  @Column(name = "cover_image", nullable = false, length = 500)
  private String coverImage;
  
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;
  
  @Size(max = 255)
  @NotNull
  @Column(name = "title", nullable = false)
  private String title;
  
  @NotNull
  @Column(name = "sapo", nullable = false, columnDefinition = "TEXT")
  private String sapo;
  
  @NotNull
  @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
  private String content;
  
  @NotNull
  @Column(name = "type", nullable = false)
  @Enumerated(EnumType.STRING)
  private ArticleType type;
  
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private ArticleStatus status;
  
  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private String rejectionReason;
  
  @ColumnDefault("0")
  @Column(name = "view_count")
  private Integer viewCount;
  
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;
  
  @OneToMany(mappedBy = "article")
  private Set<Comment> comments = new LinkedHashSet<>();
  
}
