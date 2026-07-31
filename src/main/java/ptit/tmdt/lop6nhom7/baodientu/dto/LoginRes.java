package ptit.tmdt.lop6nhom7.baodientu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;

import java.time.Instant;

@Data
@AllArgsConstructor
public class LoginRes {
  private String jwtToken;
  private UserRole role;
  private String name;
  private Instant vipExpiryDate;
  private int freeArticlesLeft;
  private Integer id;
  private String email;
  private String avatar;
}
