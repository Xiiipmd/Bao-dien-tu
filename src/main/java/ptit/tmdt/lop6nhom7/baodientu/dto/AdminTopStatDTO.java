package ptit.tmdt.lop6nhom7.baodientu.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminTopStatDTO {
    private Integer rank;
    private String targetType;
    private Integer targetId;
    private String targetName;
    private Long articles;
    private Long views;
    private Long revenue;
}
