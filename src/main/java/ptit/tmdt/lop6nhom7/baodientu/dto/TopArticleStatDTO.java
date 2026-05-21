package ptit.tmdt.lop6nhom7.baodientu.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TopArticleStatDTO {
    private Integer articleId;
    private String title;
    private Instant publishedAt;
    private Long views;
}
