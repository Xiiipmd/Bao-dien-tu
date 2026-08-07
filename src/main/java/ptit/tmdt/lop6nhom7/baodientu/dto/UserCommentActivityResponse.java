package ptit.tmdt.lop6nhom7.baodientu.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCommentActivityResponse {
  private Integer commentId;
  private String content;
  private Instant createdAt;
  private CommentArticleDto article;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CommentArticleDto {
    private Integer id;
    private String title;
    private String categoryName;
    private String thumbnailUrl;
  }
}
