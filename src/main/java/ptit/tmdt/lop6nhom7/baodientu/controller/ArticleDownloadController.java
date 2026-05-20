package ptit.tmdt.lop6nhom7.baodientu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticlePdfDownload;
import ptit.tmdt.lop6nhom7.baodientu.service.ArticleDownloadService;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleDownloadController {
  private final ArticleDownloadService articleDownloadService;

  @GetMapping("/{articleId}/download-pdf")
  public ResponseEntity<byte[]> downloadPdf(@PathVariable Integer articleId) {
    ArticlePdfDownload download = articleDownloadService.downloadArticlePdf(articleId);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(ContentDisposition.attachment()
        .filename(download.fileName())
        .build());
    headers.setContentLength(download.content().length);

    return ResponseEntity.ok()
        .headers(headers)
        .body(download.content());
  }
}
