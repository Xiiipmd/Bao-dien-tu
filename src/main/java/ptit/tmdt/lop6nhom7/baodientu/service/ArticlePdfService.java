package ptit.tmdt.lop6nhom7.baodientu.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ArticlePdfService {
  private static final float MARGIN = 56;
  private static final float BODY_FONT_SIZE = 12;
  private static final float TITLE_FONT_SIZE = 18;
  private static final float LEADING = 18;
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
      .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

  public byte[] createPdf(Article article) throws IOException {
    try (PDDocument document = new PDDocument();
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      FontChoice fontChoice = loadFont(document);
      PdfWriter writer = new PdfWriter(document, fontChoice);

      writer.writeParagraph(article.getTitle(), TITLE_FONT_SIZE, 24);
      writer.writeParagraph("Tac gia: " + article.getAuthor().getFullName(), BODY_FONT_SIZE, LEADING);
      writer.writeParagraph("Chuyen muc: " + article.getCategory().getName(), BODY_FONT_SIZE, LEADING);
      if (article.getCreatedAt() != null) {
        writer.writeParagraph("Ngay dang: " + DATE_FORMAT.format(article.getCreatedAt()), BODY_FONT_SIZE, LEADING);
      }
      writer.blankLine();
      writer.writeParagraph(article.getSapo(), BODY_FONT_SIZE, LEADING);
      writer.blankLine();
      writer.writeParagraph(cleanContent(article.getContent()), BODY_FONT_SIZE, LEADING);
      writer.close();

      document.save(output);
      return output.toByteArray();
    }
  }

  private FontChoice loadFont(PDDocument document) throws IOException {
    for (String path : List.of(
        "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/calibri.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    )) {
      File fontFile = new File(path);
      if (fontFile.exists()) {
        return new FontChoice(PDType0Font.load(document, fontFile), true);
      }
    }
    return new FontChoice(PDType1Font.HELVETICA, false);
  }

  private String cleanContent(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replaceAll("<[^>]+>", " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replaceAll("[ \\t]+", " ")
        .trim();
  }

  private record FontChoice(PDFont font, boolean unicode) {
    String safeText(String value) {
      String safe = value == null ? "" : value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
      if (unicode) {
        return safe;
      }
      return Normalizer.normalize(safe, Normalizer.Form.NFD)
          .replaceAll("\\p{M}", "")
          .replaceAll("[^\\x20-\\x7E]", "");
    }
  }

  private static class PdfWriter {
    private final PDDocument document;
    private final FontChoice fontChoice;
    private PDPageContentStream stream;
    private float y;

    PdfWriter(PDDocument document, FontChoice fontChoice) throws IOException {
      this.document = document;
      this.fontChoice = fontChoice;
      newPage();
    }

    void writeParagraph(String paragraph, float fontSize, float leading) throws IOException {
      for (String rawParagraph : normalizeParagraphs(paragraph)) {
        if (rawParagraph.isBlank()) {
          blankLine();
          continue;
        }
        for (String line : wrap(rawParagraph, fontSize)) {
          writeLine(line, fontSize, leading);
        }
        blankLine();
      }
    }

    void blankLine() throws IOException {
      ensureSpace(LEADING);
      stream.newLineAtOffset(0, -LEADING);
      y -= LEADING;
    }

    void close() throws IOException {
      if (stream != null) {
        stream.endText();
        stream.close();
        stream = null;
      }
    }

    private void newPage() throws IOException {
      if (stream != null) {
        stream.endText();
        stream.close();
      }
      PDPage page = new PDPage(PDRectangle.A4);
      document.addPage(page);
      y = page.getMediaBox().getHeight() - MARGIN;
      stream = new PDPageContentStream(document, page);
      stream.beginText();
      stream.newLineAtOffset(MARGIN, y);
    }

    private void writeLine(String text, float fontSize, float leading) throws IOException {
      ensureSpace(leading);
      stream.setFont(fontChoice.font(), fontSize);
      stream.showText(fontChoice.safeText(text));
      stream.newLineAtOffset(0, -leading);
      y -= leading;
    }

    private void ensureSpace(float requiredHeight) throws IOException {
      if (y - requiredHeight < MARGIN) {
        newPage();
      }
    }

    private List<String> normalizeParagraphs(String text) {
      return List.of((text == null ? "" : text).replace("\r", "").split("\n"));
    }

    private List<String> wrap(String text, float fontSize) throws IOException {
      float maxWidth = PDRectangle.A4.getWidth() - (MARGIN * 2);
      List<String> lines = new ArrayList<>();
      StringBuilder current = new StringBuilder();
      for (String word : text.split("\\s+")) {
        String candidate = current.isEmpty() ? word : current + " " + word;
        if (textWidth(candidate, fontSize) <= maxWidth) {
          current.setLength(0);
          current.append(candidate);
        } else {
          if (!current.isEmpty()) {
            lines.add(current.toString());
          }
          current.setLength(0);
          current.append(word);
        }
      }
      if (!current.isEmpty()) {
        lines.add(current.toString());
      }
      return lines;
    }

    private float textWidth(String text, float fontSize) throws IOException {
      return fontChoice.font().getStringWidth(fontChoice.safeText(text)) / 1000 * fontSize;
    }
  }
}
