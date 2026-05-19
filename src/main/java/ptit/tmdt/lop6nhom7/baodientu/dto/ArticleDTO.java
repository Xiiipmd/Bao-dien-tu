package ptit.tmdt.lop6nhom7.baodientu.dto;

import lombok.Builder;
import lombok.Data;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;

import java.time.Instant;

@Data
@Builder
public class ArticleDTO {
	private Integer id;
	private Integer authorId;
	private String authorName;
	private String coverImage;
	private Integer categoryId;
	private String categoryName;
	private String title;
	private String sapo;
	private String content;
	private ArticleType type;
	private ArticleStatus status;
	private String rejectionReason;
	private Integer viewCount;
	private Instant createdAt;

}
