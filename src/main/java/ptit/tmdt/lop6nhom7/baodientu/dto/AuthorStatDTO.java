package ptit.tmdt.lop6nhom7.baodientu.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthorStatDTO {
	private Long totalArticles;
	private Long totalViews;
	private Long totalRevenue;
	private List<AuthorStatPointDTO> chart;
	private List<TopArticleStatDTO> topArticles;
}
