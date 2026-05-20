package ptit.tmdt.lop6nhom7.baodientu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;

@Data
public class ArticleUpdateRequest {
    @NotBlank
    @Size(max = 500)
    private String coverImage;

    @NotNull
    private Integer categoryId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String sapo;

    @NotBlank
    private String content;

    @NotNull
    private ArticleType type;
}