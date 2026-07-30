package ptit.tmdt.lop6nhom7.baodientu.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;

@Data
@Builder
public class ArticleReadResponse {
  private Integer id;
  private String title;
  private String sapo;
  private String content;
  private String coverImage;
  private String authorName;
  private Integer authorId;
  private Integer categoryId;
  private String categoryName;
  private ArticleType type;
  private Integer viewCount;
  private Instant createdAt;
  private boolean vipAccessGranted;
  private boolean meteredAccessApplied;
  private Integer remainingFreeReads;
  private String accessMessage;
}
