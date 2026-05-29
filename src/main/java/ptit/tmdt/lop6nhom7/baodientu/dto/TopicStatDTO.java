package ptit.tmdt.lop6nhom7.baodientu.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TopicStatDTO {
    private Integer categoryId;
    private String categoryName;
    private Long articles;
    private Long followers;
    private Long views;
    private Long freeViews;
    private Long vipViews;
    private Long revenue;
}
