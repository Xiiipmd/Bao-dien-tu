package ptit.tmdt.lop6nhom7.baodientu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ptit.tmdt.lop6nhom7.baodientu.entity.Comment;

@Repository
public interface CommentRepo extends JpaRepository<Comment, Integer> {
  List<Comment> findByArticleIdOrderByCreatedAtAsc(Integer articleId);
}