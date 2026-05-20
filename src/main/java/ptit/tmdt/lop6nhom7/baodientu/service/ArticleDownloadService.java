package ptit.tmdt.lop6nhom7.baodientu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticlePdfDownload;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ArticleDownloadService {
  private final ArticleRepo articleRepo;
  private final ArticlePdfService articlePdfService;
  private final VipAccessService vipAccessService;

  @Transactional(readOnly = true)
  public ArticlePdfDownload downloadArticlePdf(Integer articleId) {
    vipAccessService.requireCurrentVipUser();
    Article article = articleRepo.findByIdAndStatus(articleId, ArticleStatus.PUBLISHED)
        .orElseThrow(() -> new NotFoundException("Khong tim thay bai bao da xuat ban voi id = " + articleId));

    try {
      byte[] pdfContent = articlePdfService.createPdf(article);
      return new ArticlePdfDownload(buildFileName(article), pdfContent);
    } catch (IOException ex) {
      throw new BadRequestException("Da co loi xay ra trong qua trinh tao file PDF. Vui long thu lai");
    }
  }

  private String buildFileName(Article article) {
    String title = article.getTitle() == null ? "bai-bao" : article.getTitle();
    String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
    if (normalized.isBlank()) {
      normalized = "bai-bao";
    }
    return "article-" + article.getId() + "-" + normalized + ".pdf";
  }
}
