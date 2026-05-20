package ptit.tmdt.lop6nhom7.baodientu.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterReq {
  @NotBlank(message = "Email khong duoc bo trong")
  @Email
  private String email;
  
  @NotBlank(message = "Ten khong duoc bo trong")
  private String name;

  @NotBlank(message = "Password khong duoc bo trong")
  private String password;
  
  @NotBlank(message = "Password khong duoc bo trong")
  private String confirmation;

  public void setEmail(String email) {
    this.email = email == null ? null : email.trim();
  }

  public void setName(String name) {
    this.name = name == null ? null : name.trim();
  }

  public void setPassword(String password) {
    this.password = password == null ? null : password.trim();
  }

  public void setConfirmation(String confirmation) {
    this.confirmation = confirmation == null ? null : confirmation.trim();
  }
}
