package ptit.tmdt.lop6nhom7.baodientu.dto;

public record MediaUploadResponse(
    Integer id,
    String path,
    String contentType,
    Long size
) {
}
