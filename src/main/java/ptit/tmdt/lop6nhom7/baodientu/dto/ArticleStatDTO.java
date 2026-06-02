package ptit.tmdt.lop6nhom7.baodientu.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleStatDTO {
    private int views;
    private int estimatedEarning;
    private List<Integer> viewsByLevelOfGranularity;
}
