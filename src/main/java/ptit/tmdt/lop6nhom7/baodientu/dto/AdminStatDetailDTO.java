package ptit.tmdt.lop6nhom7.baodientu.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminStatDetailDTO {
    private String period;
    private Integer authorId;
    private String authorName;
    private Integer categoryId;
    private String categoryName;
    private Long articles;
    private Long views;
    private Long revenue;
}
