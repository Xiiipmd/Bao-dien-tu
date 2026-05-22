package ptit.tmdt.lop6nhom7.baodientu.dto;

import lombok.Builder;
import lombok.Data;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;

@Data
@Builder
public class ArticlePreviewResponse {
  private Integer id;
  private String title;
  private String sapo;
  private String coverImage;
  private String previewContent;
  private String authorName;
  private String categoryName;
  private ArticleType type;
  private boolean paywallRequired;
}