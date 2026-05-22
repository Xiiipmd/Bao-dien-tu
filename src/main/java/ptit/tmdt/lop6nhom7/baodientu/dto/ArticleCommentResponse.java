package ptit.tmdt.lop6nhom7.baodientu.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArticleCommentResponse {
  private Integer id;
  private Integer articleId;
  private Integer userId;
  private String userName;
  private String content;
  private Instant createdAt;
}