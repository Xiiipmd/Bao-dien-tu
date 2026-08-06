package ptit.tmdt.lop6nhom7.baodientu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.entity.Category;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class ArticleDTO {
	private Integer id;
	@NotNull
	private Integer authorId;
	private String authorName;
	@NotBlank
	@Size(max = 500)
	private String coverImage;
	@NotNull
	private Integer categoryId;
	private String categoryName;
	@NotBlank
	@Size(max = 255)
	private String title;
	@NotBlank
	private String sapo;
	@NotBlank
	private String content;
	@NotNull
	private ArticleType type;
	private ArticleStatus status;
	private String rejectionReason;
	private Integer viewCount;
	private Instant createdAt;
	private ptit.tmdt.lop6nhom7.baodientu.enums.ArticleOrigin origin;
	private String originalUrl;
	private String sourceName;
	private String externalId;

	public Article toArticle() {
		Article article = new Article();
		article.setId(id);
		if (authorId != null) {
			User author = new User();
			author.setId(authorId);
			article.setAuthor(author);
		}
		if (categoryId != null) {
			Category category = new Category();
			category.setId(categoryId);
			article.setCategory(category);
		}
		article.setCoverImage(coverImage);
		article.setTitle(title);
		article.setSapo(sapo);
		article.setContent(content);
		article.setType(type);
		article.setStatus(status);
		article.setRejectionReason(rejectionReason);
		article.setViewCount(viewCount);
		article.setCreatedAt(createdAt);
		article.setOrigin(origin);
		article.setOriginalUrl(originalUrl);
		article.setSourceName(sourceName);
		article.setExternalId(externalId);
		return article;
	}

}
