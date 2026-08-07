package ptit.tmdt.lop6nhom7.baodientu.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ptit.tmdt.lop6nhom7.baodientu.entity.Comment;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;

@Repository
public interface CommentRepo extends JpaRepository<Comment, Integer> {
  List<Comment> findByArticleIdOrderByCreatedAtAsc(Integer articleId);

  long countByUserIdAndArticleStatus(Integer userId, ArticleStatus status);

  Page<Comment> findByUserIdAndArticleStatusOrderByCreatedAtDesc(Integer userId, ArticleStatus status, Pageable pageable);
}