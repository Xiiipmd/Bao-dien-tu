package ptit.tmdt.lop6nhom7.baodientu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateArticleCommentRequest {
  @NotBlank(message = "Nội dung bình luận không được để trống")
  @Size(max = 2000, message = "Nội dung bình luận không được vượt quá 2000 ký tự")
  private String content;
}