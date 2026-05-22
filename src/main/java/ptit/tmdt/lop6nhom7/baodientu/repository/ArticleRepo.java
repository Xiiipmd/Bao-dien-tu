package ptit.tmdt.lop6nhom7.baodientu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepo extends JpaRepository<Article, Integer> {
  Optional<Article> findByIdAndStatus(Integer id, ArticleStatus status);
  List<Article> findByStatusOrderByCreatedAtDesc(ArticleStatus status);
  List<Article> findAllByOrderByCreatedAtDesc();
  List<Article> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);
  List<Article> findByAuthorIdOrderByCreatedAtDesc(Integer authorId);
  List<Article> findByAuthorIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(Integer authorId, String title);
  long countByAuthorIdAndStatusAndCreatedAtBetween(
      Integer authorId,
      ArticleStatus status,
      Instant startDate,
      Instant endDate
  );
}
