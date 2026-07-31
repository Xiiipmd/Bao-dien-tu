package ptit.tmdt.lop6nhom7.baodientu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ArticleRepo extends JpaRepository<Article, Integer> {
  Optional<Article> findByIdAndStatus(Integer id, ArticleStatus status);
  @EntityGraph(attributePaths = {"author", "category"})
  Optional<Article> findWithAuthorAndCategoryById(Integer id);
  List<Article> findByStatusOrderByCreatedAtDesc(ArticleStatus status);
  List<Article> findByStatusInOrderByCreatedAtDesc(List<ArticleStatus> statuses);
  List<Article> findByStatusInAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(List<ArticleStatus> statuses, String title);
  List<Article> findAllByOrderByCreatedAtDesc();
  List<Article> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);
  List<Article> findByAuthorIdOrderByCreatedAtDesc(Integer authorId);
  List<Article> findByAuthorIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(Integer authorId, String title);
  @Query("""
      select a
      from Article a
      join fetch a.author
      join fetch a.category
      where a.status = :status
        and (:keyword is null or trim(:keyword) = '' or lower(a.title) like lower(concat('%', :keyword, '%')))
        and (:categoryId is null or a.category.id = :categoryId)
        and (:authorId is null or a.author.id = :authorId)
        and (:authorName is null or trim(:authorName) = '' or lower(a.author.fullName) like lower(concat('%', :authorName, '%')))
      order by a.publishedAt desc
      """)
  List<Article> searchPublishedArticles(
      @Param("status") ArticleStatus status,
      @Param("keyword") String keyword,
      @Param("categoryId") Integer categoryId,
      @Param("authorId") Integer authorId,
      @Param("authorName") String authorName
  );

  @Query("""
      select a
      from Article a
      where a.status = :status
      order by a.publishedAt desc
      """)
  @EntityGraph(attributePaths = {"author", "category"})
  List<Article> findPublishedArticlesByRecency(@Param("status") ArticleStatus status);

  @Query("""
      select a
      from Article a
      join fetch a.author
      join fetch a.category
      where a.status = :status
      order by a.publishedAt desc
      """)
  List<Article> findHomeArticles(
      @Param("status") ArticleStatus status,
      Pageable pageable
  );

  @Query("""
      select a
      from Article a
      join fetch a.author
      join fetch a.category
      where a.status = :status
        and a.category.id in :categoryIds
      order by a.publishedAt desc
      """)
  List<Article> findPreferredHomeArticles(
      @Param("status") ArticleStatus status,
      @Param("categoryIds") Set<Integer> categoryIds,
      Pageable pageable
  );

  @Query("""
      select a
      from Article a
      join fetch a.author
      join fetch a.category
      where a.status = :status
        and a.category.id not in :categoryIds
      order by a.publishedAt desc
      """)
  List<Article> findNonPreferredHomeArticles(
      @Param("status") ArticleStatus status,
      @Param("categoryIds") Set<Integer> categoryIds,
      Pageable pageable
  );
  long countByAuthorIdAndStatusAndCreatedAtBetween(
      Integer authorId,
      ArticleStatus status,
      Instant startDate,
      Instant endDate
  );

  @Query("""
      select count(a)
      from Article a
      where a.status = :status
        and (:authorId is null or a.author.id = :authorId)
        and (:categoryId is null or a.category.id = :categoryId)
        and a.createdAt between :startDate and :endDate
      """)
  long countPublishedArticlesForAdminStats(
      @Param("authorId") Integer authorId,
      @Param("categoryId") Integer categoryId,
      @Param("status") ArticleStatus status,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate
  );

  @Query("""
      select a.category.id as categoryId,
          a.category.name as categoryName,
          count(a) as articles
      from Article a
      where a.author.id = :authorId
        and a.status = :status
        and a.createdAt between :startDate and :endDate
      group by a.category.id, a.category.name
      order by count(a) desc, a.category.name asc
      """)
  List<TopicArticleCount> countPublishedArticlesByTopic(
      @Param("authorId") Integer authorId,
      @Param("status") ArticleStatus status,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate
  );

  interface TopicArticleCount {
    Integer getCategoryId();
    String getCategoryName();
    Long getArticles();
  }
}
