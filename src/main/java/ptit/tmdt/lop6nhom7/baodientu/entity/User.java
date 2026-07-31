package ptit.tmdt.lop6nhom7.baodientu.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "users", schema = "pthttmdt")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;
  
  @Size(max = 255)
  @NotNull
  @Column(name = "full_name", nullable = false)
  private String fullName;
  
  @Size(max = 255)
  @NotNull
  @Column(name = "email", nullable = false)
  private String email;
  
  @Size(max = 255)
  @NotNull
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;
  
  @NotNull
  @Column(name = "role", nullable = false)
  @Enumerated(EnumType.STRING)
  private UserRole role;
  
  @Column(name = "vip_expiry_date")
  private Instant vipExpiryDate;

  @Size(max = 500)
  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;
  
  @ColumnDefault("3")
  @Column(name = "free_articles_left")
  private Integer freeArticlesLeft;
  
  @ColumnDefault("'ACTIVE'")
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private UserStatus status;
  
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at")
  private Instant createdAt;

  @ColumnDefault("true")
  @Column(name = "push_notifications_enabled", nullable = false)
  private Boolean pushNotificationsEnabled = true;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "user_topic_preferences",
      schema = "pthttmdt",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "category_id")
  )
  private Set<Category> preferredCategories = new LinkedHashSet<>();
  
}
