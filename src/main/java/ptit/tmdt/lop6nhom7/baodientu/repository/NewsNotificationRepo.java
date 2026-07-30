package ptit.tmdt.lop6nhom7.baodientu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ptit.tmdt.lop6nhom7.baodientu.entity.NewsNotification;
import ptit.tmdt.lop6nhom7.baodientu.enums.NotificationType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NewsNotificationRepo extends JpaRepository<NewsNotification, Long> {
  List<NewsNotification> findTop30ByUserIdOrderByCreatedAtDesc(Integer userId);
  long countByUserIdAndReadFalse(Integer userId);
  boolean existsByUserIdAndArticleIdAndType(Integer userId, Integer articleId, NotificationType type);
  Optional<NewsNotification> findByUserIdAndArticleIdAndType(
      Integer userId,
      Integer articleId,
      NotificationType type
  );
  List<NewsNotification> findByArticleIdAndTypeAndReadFalse(
      Integer articleId,
      NotificationType type
  );
  Optional<NewsNotification> findByIdAndUserId(Long id, Integer userId);
  List<NewsNotification> findByUserIdAndReadFalse(Integer userId);

  @org.springframework.data.jpa.repository.Query("""
      select n.user.id
      from NewsNotification n
      where n.article.id = :articleId
        and n.type = :type
        and n.user.id in :userIds
      """)
  Set<Integer> findExistingRecipientIds(
      @org.springframework.data.repository.query.Param("articleId") Integer articleId,
      @org.springframework.data.repository.query.Param("type") NotificationType type,
      @org.springframework.data.repository.query.Param("userIds") List<Integer> userIds
  );
}
