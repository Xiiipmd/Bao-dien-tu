package ptit.tmdt.lop6nhom7.baodientu.service;

import org.springframework.stereotype.Service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.entity.Category;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.CategoryRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffArticleService {
    private final ArticleRepo articleRepo;
    private final CategoryRepo categoryRepo;
    private final UserRepo userRepo;

    public void createArticle(ArticleDTO articleDTO) {
        if (articleDTO == null) {
            throw new BadRequestException("Article data is required");
        }
        if (articleDTO.getId() != null) {
            throw new BadRequestException("Article id must be null for creation");
        }

        User author = userRepo.findById(articleDTO.getAuthorId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        Category category = categoryRepo.findById(articleDTO.getCategoryId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thể loại"));

        Article article = articleDTO.toArticle();
        article.setAuthor(author);
        article.setCategory(category);
        if (article.getStatus() == null) {
            article.setStatus(ArticleStatus.PENDING);
        }
        if (article.getViewCount() == null) {
            article.setViewCount(0);
        }
        if (article.getCreatedAt() == null) {
            article.setCreatedAt(Instant.now());
        }

        articleRepo.save(article);
    }
    
}
