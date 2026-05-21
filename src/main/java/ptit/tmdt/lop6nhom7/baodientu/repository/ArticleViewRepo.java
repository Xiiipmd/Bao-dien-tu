package ptit.tmdt.lop6nhom7.baodientu.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ptit.tmdt.lop6nhom7.baodientu.entity.ArticleView;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;

@Repository
public interface ArticleViewRepo extends JpaRepository<ArticleView, Integer> {
    long countByArticleAuthorIdAndArticleStatusAndViewedAtBetween(
        Integer authorId,
        ArticleStatus status,
        Instant startDate,
        Instant endDate
    );

    @Query("""
        select av.article.id as articleId,
            av.article.title as title,
            av.article.createdAt as createdAt,
            count(av) as views
        from ArticleView av
        where av.article.author.id = :authorId
            and av.article.status = :status
            and av.viewedAt between :startDate and :endDate
        group by av.article.id, av.article.title, av.article.createdAt
        order by count(av) desc, av.article.createdAt desc
        """)
    List<TopArticleView> findTopArticlesByAuthor(
        @Param("authorId") Integer authorId,
        @Param("status") ArticleStatus status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    @Query(value = """
        select date(av.viewed_at) as viewDate, count(*) as views
        from article_views av
        join articles a on a.id = av.article_id
        where a.author_id = :authorId
            and a.status = :status
            and av.viewed_at between :startDate and :endDate
        group by date(av.viewed_at)
        order by date(av.viewed_at)
        """, nativeQuery = true)
    List<DailyViewCount> findDailyViewCounts(
        @Param("authorId") Integer authorId,
        @Param("status") String status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    interface TopArticleView {
        Integer getArticleId();
        String getTitle();
        Instant getCreatedAt();
        Long getViews();
    }

    interface DailyViewCount {
        LocalDate getViewDate();
        Long getViews();
    }
}
