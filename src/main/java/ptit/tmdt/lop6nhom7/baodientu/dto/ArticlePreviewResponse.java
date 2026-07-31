package ptit.tmdt.lop6nhom7.baodientu.dto;

import lombok.Builder;
import lombok.Data;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;
import ptit.tmdt.lop6nhom7.baodientu.enums.VipPreviewAccessMode;

@Data
@Builder
public class ArticlePreviewResponse {
  private Integer id;
  private String title;
  private String sapo;
  private String coverImage;
  private String previewContent;
  private Integer authorId;
  private String authorName;
  private Integer categoryId;
  private String categoryName;
  private ArticleType type;
  private boolean paywallRequired;
  private VipPreviewAccessMode accessMode;
  private Integer remainingFreeReads;
  private boolean alreadyRead;
  private boolean willConsumeFreeRead;
}
