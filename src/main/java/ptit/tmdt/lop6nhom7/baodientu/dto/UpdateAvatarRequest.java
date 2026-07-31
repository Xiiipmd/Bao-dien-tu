package ptit.tmdt.lop6nhom7.baodientu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAvatarRequest(
    @NotBlank(message = "Ảnh đại diện không được để trống")
    @Size(max = 500, message = "Đường dẫn ảnh đại diện không được vượt quá 500 ký tự")
    String avatar
) {}
