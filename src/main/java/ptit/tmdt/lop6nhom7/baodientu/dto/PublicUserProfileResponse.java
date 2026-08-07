package ptit.tmdt.lop6nhom7.baodientu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserProfileResponse {
  private Integer id;
  private String displayName;
  private String avatarUrl;
  private String role;
  private long commentCount;
}
