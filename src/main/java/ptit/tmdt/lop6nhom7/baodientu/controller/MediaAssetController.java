package ptit.tmdt.lop6nhom7.baodientu.controller;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ptit.tmdt.lop6nhom7.baodientu.dto.MediaUploadResponse;
import ptit.tmdt.lop6nhom7.baodientu.entity.MediaAsset;
import ptit.tmdt.lop6nhom7.baodientu.service.MediaAssetService;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaAssetController {
  private final MediaAssetService mediaAssetService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<MediaUploadResponse> uploadImage(
      @RequestPart("file") MultipartFile file) {
    return ResponseEntity.ok(mediaAssetService.uploadImage(file));
  }

  @GetMapping("/{assetId}")
  public ResponseEntity<byte[]> getAsset(@PathVariable Integer assetId) {
    MediaAsset asset = mediaAssetService.getAsset(assetId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(asset.getContentType()))
        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(asset.getFileSize()))
        .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
        .body(asset.getData());
  }
}
