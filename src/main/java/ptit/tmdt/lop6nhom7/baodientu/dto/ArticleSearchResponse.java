package ptit.tmdt.lop6nhom7.baodientu.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;

@Data
@Builder
public class ArticleSearchResponse {
  private Integer id;
  private String title;
  private String sapo;
  private String coverImage;
  private Integer authorId;
  private String authorName;
  private Integer categoryId;
  private String categoryName;
  private ArticleType type;
  private Integer viewCount;
  private Instant createdAt;
}
