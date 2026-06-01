package ptit.tmdt.lop6nhom7.baodientu.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ptit.tmdt.lop6nhom7.baodientu.entity.ArticleView;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;

@Repository
public interface ArticleViewRepo extends JpaRepository<ArticleView, Integer> {
    boolean existsByUserIdAndArticleIdAndViewedAtBetween(
        Integer userId,
        Integer articleId,
        Instant startDate,
        Instant endDate
    );

    @Query("""
        select count(distinct av.article.id)
        from ArticleView av
        where av.user.id = :userId
            and av.article.type = :articleType
            and av.viewedAt between :startDate and :endDate
        """)
    long countDistinctArticlesByUserAndTypeWithinPeriod(
        @Param("userId") Integer userId,
        @Param("articleType") ArticleType articleType,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

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
            av.article.type as articleType,
            count(av) as views
        from ArticleView av
        where av.article.author.id = :authorId
            and av.article.status = :status
            and av.viewedAt between :startDate and :endDate
        group by av.article.id, av.article.title, av.article.createdAt, av.article.type
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
        select date_format(av.viewed_at, :periodFormat) as period,
            count(*) as views,
            sum(case when a.type = 'VIP' then :vipRate else :freeRate end) as revenue
        from article_views av
        join articles a on a.id = av.article_id
        where a.author_id = :authorId
            and a.status = :status
            and av.viewed_at between :startDate and :endDate
        group by date_format(av.viewed_at, :periodFormat)
        order by min(av.viewed_at)
        """, nativeQuery = true)
    List<ViewRevenuePoint> findViewRevenuePoints(
        @Param("authorId") Integer authorId,
        @Param("status") String status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("periodFormat") String periodFormat,
        @Param("freeRate") Long freeRate,
        @Param("vipRate") Long vipRate
    );

    @Query(value = """
        select a.category_id as categoryId,
            c.name as categoryName,
            count(*) as views,
            sum(case when a.type = 'FREE' then 1 else 0 end) as freeViews,
            sum(case when a.type = 'VIP' then 1 else 0 end) as vipViews,
            sum(case when a.type = 'VIP' then :vipRate else :freeRate end) as revenue
        from article_views av
        join articles a on a.id = av.article_id
        join categories c on c.id = a.category_id
        where a.author_id = :authorId
            and a.status = :status
            and av.viewed_at between :startDate and :endDate
        group by a.category_id, c.name
        order by revenue desc, views desc, c.name asc
        """, nativeQuery = true)
    List<TopicViewRevenue> findTopicViewRevenue(
        @Param("authorId") Integer authorId,
        @Param("status") String status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("freeRate") Long freeRate,
        @Param("vipRate") Long vipRate
    );

    @Query(value = """
        select date_format(av.viewed_at, :periodFormat) as period,
            count(*) as views,
            sum(case when a.type = 'VIP' then :vipRate else :freeRate end) as revenue
        from article_views av
        join articles a on a.id = av.article_id
        where a.status = :status
            and (:authorId is null or a.author_id = :authorId)
            and (:categoryId is null or a.category_id = :categoryId)
            and av.viewed_at between :startDate and :endDate
        group by date_format(av.viewed_at, :periodFormat)
        order by min(av.viewed_at)
        """, nativeQuery = true)
    List<ViewRevenuePoint> findAdminViewRevenuePoints(
        @Param("authorId") Integer authorId,
        @Param("categoryId") Integer categoryId,
        @Param("status") String status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("periodFormat") String periodFormat,
        @Param("freeRate") Long freeRate,
        @Param("vipRate") Long vipRate
    );

    @Query(value = """
        select date_format(av.viewed_at, :periodFormat) as period,
            a.author_id as authorId,
            u.full_name as authorName,
            a.category_id as categoryId,
            c.name as categoryName,
            count(distinct a.id) as articles,
            count(*) as views,
            sum(case when a.type = 'VIP' then :vipRate else :freeRate end) as revenue
        from article_views av
        join articles a on a.id = av.article_id
        join users u on u.id = a.author_id
        join categories c on c.id = a.category_id
        where a.status = :status
            and (:authorId is null or a.author_id = :authorId)
            and (:categoryId is null or a.category_id = :categoryId)
            and av.viewed_at between :startDate and :endDate
        group by date_format(av.viewed_at, :periodFormat),
            a.author_id,
            u.full_name,
            a.category_id,
            c.name
        order by min(av.viewed_at), u.full_name asc, c.name asc
        """, nativeQuery = true)
    List<AdminStatDetail> findAdminStatDetails(
        @Param("authorId") Integer authorId,
        @Param("categoryId") Integer categoryId,
        @Param("status") String status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("periodFormat") String periodFormat,
        @Param("freeRate") Long freeRate,
        @Param("vipRate") Long vipRate
    );

    @Query(value = """
        select u.id as targetId,
            u.full_name as targetName,
            count(distinct a.id) as articles,
            count(*) as views,
            sum(case when a.type = 'VIP' then :vipRate else :freeRate end) as revenue
        from article_views av
        join articles a on a.id = av.article_id
        join users u on u.id = a.author_id
        where a.status = :status
            and av.viewed_at between :startDate and :endDate
        group by u.id, u.full_name
        """, nativeQuery = true)
    List<AdminTopStat> findTopAuthorsForAdmin(
        @Param("status") String status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("freeRate") Long freeRate,
        @Param("vipRate") Long vipRate
    );

    @Query(value = """
        select c.id as targetId,
            c.name as targetName,
            count(distinct a.id) as articles,
            count(*) as views,
            sum(case when a.type = 'VIP' then :vipRate else :freeRate end) as revenue
        from article_views av
        join articles a on a.id = av.article_id
        join categories c on c.id = a.category_id
        where a.status = :status
            and av.viewed_at between :startDate and :endDate
        group by c.id, c.name
        """, nativeQuery = true)
    List<AdminTopStat> findTopCategoriesForAdmin(
        @Param("status") String status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("freeRate") Long freeRate,
        @Param("vipRate") Long vipRate
    );

    interface TopArticleView {
        Integer getArticleId();
        String getTitle();
        Instant getCreatedAt();
        ArticleType getArticleType();
        Long getViews();
    }

    interface ViewRevenuePoint {
        String getPeriod();
        Long getViews();
        Long getRevenue();
    }

    interface TopicViewRevenue {
        Integer getCategoryId();
        String getCategoryName();
        Long getViews();
        Long getFreeViews();
        Long getVipViews();
        Long getRevenue();
    }

    interface AdminStatDetail {
        String getPeriod();
        Integer getAuthorId();
        String getAuthorName();
        Integer getCategoryId();
        String getCategoryName();
        Long getArticles();
        Long getViews();
        Long getRevenue();
    }

    interface AdminTopStat {
        Integer getTargetId();
        String getTargetName();
        Long getArticles();
        Long getViews();
        Long getRevenue();
    }
}
