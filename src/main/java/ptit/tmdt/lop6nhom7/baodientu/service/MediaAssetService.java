package ptit.tmdt.lop6nhom7.baodientu.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ptit.tmdt.lop6nhom7.baodientu.dto.MediaUploadResponse;
import ptit.tmdt.lop6nhom7.baodientu.entity.MediaAsset;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.ForbiddenException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.MediaAssetRepo;

@Service
@RequiredArgsConstructor
public class MediaAssetService {
  private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/webp"
  );

  private final MediaAssetRepo mediaAssetRepo;

  @Transactional
  public MediaUploadResponse uploadImage(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("Ảnh tải lên không được để trống");
    }
    if (file.getSize() > MAX_IMAGE_BYTES) {
      throw new BadRequestException("Ảnh bìa không được vượt quá 5 MB");
    }
    String contentType = file.getContentType() == null
        ? ""
        : file.getContentType().toLowerCase();
    if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
      throw new BadRequestException("Chỉ hỗ trợ ảnh JPEG, PNG hoặc WebP");
    }

    MediaAsset asset = new MediaAsset();
    asset.setUploaderId(getCurrentUserId());
    asset.setOriginalName(safeFileName(file.getOriginalFilename()));
    asset.setContentType(contentType);
    asset.setFileSize(file.getSize());
    asset.setCreatedAt(Instant.now());
    try {
      asset.setData(file.getBytes());
    } catch (IOException exception) {
      throw new BadRequestException("Không thể đọc dữ liệu ảnh tải lên");
    }

    MediaAsset saved = mediaAssetRepo.save(asset);
    return new MediaUploadResponse(
        saved.getId(),
        "/api/media/" + saved.getId(),
        saved.getContentType(),
        saved.getFileSize()
    );
  }

  @Transactional(readOnly = true)
  public MediaAsset getAsset(Integer assetId) {
    return mediaAssetRepo.findById(assetId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy ảnh"));
  }

  private Integer getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new ForbiddenException("Không xác định được người dùng hiện tại");
    }
    return authentication.getPrincipal() instanceof Integer userId
        ? userId
        : Integer.valueOf(authentication.getName());
  }

  private String safeFileName(String originalName) {
    String name = originalName == null || originalName.isBlank()
        ? "article-cover"
        : originalName;
    name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
    return name.length() > 255 ? name.substring(name.length() - 255) : name;
  }
}
