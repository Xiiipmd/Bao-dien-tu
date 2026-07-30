package ptit.tmdt.lop6nhom7.baodientu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    String currentPassword,

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 100, message = "Mật khẩu mới phải có từ 8 đến 100 ký tự")
    String newPassword,

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    String confirmation
) {}
