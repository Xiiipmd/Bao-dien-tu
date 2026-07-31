package ptit.tmdt.lop6nhom7.baodientu.dto;
 
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
 
import java.math.BigDecimal;
 
@Getter
@Setter
@Builder
public class VipPackageResponse {
 
    private Integer    id;
    private String     name;
    private Integer    durationDays;
    private BigDecimal price;
    private Integer    discountPercent;
    private String     description;
}
