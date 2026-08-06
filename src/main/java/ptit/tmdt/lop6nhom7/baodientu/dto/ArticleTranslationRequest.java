package ptit.tmdt.lop6nhom7.baodientu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArticleTranslationRequest(
    @NotBlank(message = "Nội dung cần dịch không được để trống")
    @Size(max = 50000, message = "Nội dung cần dịch không được vượt quá 50000 ký tự")
    String text
) {
}
