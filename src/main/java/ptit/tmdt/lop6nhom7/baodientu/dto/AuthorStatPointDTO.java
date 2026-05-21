package ptit.tmdt.lop6nhom7.baodientu.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthorStatPointDTO {
    private String date;
    private Long views;
    private Long revenue;
}
