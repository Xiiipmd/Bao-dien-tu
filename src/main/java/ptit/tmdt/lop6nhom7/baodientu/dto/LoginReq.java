package ptit.tmdt.lop6nhom7.baodientu.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginReq {
  @NotBlank(message = "Email khong duoc bo trong")
  @Email
  private String email;
  @NotBlank(message = "Password khong duoc bo trong")
  private String password;

  public void setEmail(String email) {
    this.email = email == null ? null : email.trim();
  }

  public void setPassword(String password) {
    this.password = password == null ? null : password.trim();
  }
}
