package ptit.tmdt.lop6nhom7.baodientu.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminOverviewStatDTO {
    private Long totalArticles;
    private Long totalViews;
    private Long totalRevenue;
    private String periodUnit;
    private Long freeViewPrice;
    private Long vipViewPrice;
    private List<AuthorStatPointDTO> chart;
    private List<AdminStatDetailDTO> details;
}
