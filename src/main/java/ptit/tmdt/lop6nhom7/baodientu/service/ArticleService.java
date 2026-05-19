package ptit.tmdt.lop6nhom7.baodientu.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.genai.Client;

import io.micrometer.core.ipc.http.HttpSender.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
    
    private final ArticleRepo articleRepo;
    private final Client geminiClient = new Client();
    private final List<String> geminiModels = List.of(
        "gemini-3.1-pro-preview",
        "gemini-2.5-pro",
        "gemini-3-flash",
        "gemini-2.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash-lite"
    );

    public ArticleDTO summarizeArticle(int articleId) throws Exception {
        // find article text
        Article article = articleRepo.findById(articleId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài báo"));
        // build prompt
        String prompt = """
        Bạn là trợ lý tóm tắt văn bản chuyên nghiệp.

        Hãy đọc bài báo dưới đây và tóm tắt theo cấu trúc sau:
        - **Ý chính**: 1–2 câu nêu chủ đề trọng tâm
        - **Nội dung nổi bật**: 3–5 gạch đầu dòng, mỗi điểm 1 câu
        - **Kết luận / Thông điệp**: 1 câu chốt lại ý nghĩa hoặc tác động

        Yêu cầu:
        - Ngôn ngữ tóm tắt phải khớp với ngôn ngữ của bài báo gốc
        - Không thêm ý kiến cá nhân hay thông tin ngoài bài
        - Độ dài tóm tắt không vượt quá 30%% nội dung gốc

        <article>
        %s
        </article>
        """.formatted(article.getContent());
        String chatResponse = "";
        // call gemini api
        for (String model : geminiModels) {
            try {
                chatResponse = geminiClient.models.generateContent(model, prompt, null).text();
                break;
            }
            catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        if (chatResponse.equals("")) {
            throw new Exception("Không thể tóm tắt bài báo vào lúc này. Hãy thử lại sau");
        }
        return ArticleDTO.builder()
            .authorId(0)
            .categoryId(0)
            .coverImage("dummy")
            .title("AI Summary")
            .sapo("AI summary")
            .content(chatResponse)
            .type(ArticleType.FREE)
            .build();
    }

}
