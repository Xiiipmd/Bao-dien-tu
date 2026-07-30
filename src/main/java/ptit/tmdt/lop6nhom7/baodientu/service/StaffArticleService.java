package ptit.tmdt.lop6nhom7.baodientu.service;

import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleUpdateRequest;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.entity.Category;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.ForbiddenException;
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
    private final NewsNotificationService newsNotificationService;

    @Transactional
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
        article.setStatus(ArticleStatus.PENDING);
        if (article.getViewCount() == null) {
            article.setViewCount(0);
        }
        if (article.getCreatedAt() == null) {
            article.setCreatedAt(Instant.now());
        }

        Article savedArticle = articleRepo.save(article);
        newsNotificationService.notifyModeratorsAboutSubmission(savedArticle, false);
    }

    @Transactional(readOnly = true)
    public List<ArticleDTO> getManageableArticles(String keyword) {
        User currentUser = getCurrentUser();
        String normalizedKeyword = keyword == null ? "" : keyword.trim();

        List<Article> articles;
        if (currentUser.getRole() == UserRole.ADMIN) {
            articles = normalizedKeyword.isBlank()
                ? articleRepo.findAllByOrderByCreatedAtDesc()
                : articleRepo.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(normalizedKeyword);
        } else {
            articles = normalizedKeyword.isBlank()
                ? articleRepo.findByAuthorIdOrderByCreatedAtDesc(currentUser.getId())
                : articleRepo.findByAuthorIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(currentUser.getId(), normalizedKeyword);
        }

        return articles.stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public ArticleDTO getArticleDetail(Integer articleId) {
        Article article = articleRepo.findById(articleId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));

        User currentUser = getCurrentUser();
        if (currentUser.getRole() == UserRole.AUTHOR
                && !Objects.equals(article.getAuthor().getId(), currentUser.getId())) {
            throw new ForbiddenException("Tác giả chỉ được xem bài viết của chính mình trong khu vực chỉnh sửa");
        }

        if (article.getStatus() == ArticleStatus.HIDDEN) {
            throw new ForbiddenException("Bạn không thể chỉnh sửa bài viết này do đã bị khóa hoặc gỡ khỏi hệ thống");
        }

        return toDto(article);
    }

    @Transactional
    public ArticleDTO updateArticle(Integer articleId, ArticleUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Article data is required");
        }

        Article article = articleRepo.findById(articleId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));

        User currentUser = getCurrentUser();
        if (currentUser.getRole() == UserRole.AUTHOR
                && !Objects.equals(article.getAuthor().getId(), currentUser.getId())) {
            throw new ForbiddenException("Tác giả chỉ được sửa bài viết của chính mình");
        }

        if (article.getStatus() == ArticleStatus.HIDDEN) {
            throw new ForbiddenException("Bạn không thể chỉnh sửa bài viết này do đã bị khóa hoặc gỡ khỏi hệ thống");
        }

        Category category = categoryRepo.findById(request.getCategoryId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thể loại"));

        article.setCategory(category);
        article.setCoverImage(request.getCoverImage().trim());
        article.setTitle(request.getTitle().trim());
        article.setSapo(request.getSapo().trim());
        article.setContent(request.getContent().trim());
        article.setType(request.getType());

        boolean resubmitted = article.getStatus() == ArticleStatus.REJECTED
                && currentUser.getRole() == UserRole.AUTHOR;
        if (resubmitted) {
            article.setStatus(ArticleStatus.PENDING);
            article.setRejectionReason(null);
        }

        Article savedArticle = articleRepo.save(article);
        if (resubmitted) {
            newsNotificationService.notifyModeratorsAboutSubmission(savedArticle, true);
        }
        return toDto(savedArticle);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ForbiddenException("Không xác định được người dùng hiện tại");
        }

        Integer currentUserId;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Integer userId) {
            currentUserId = userId;
        } else {
            currentUserId = Integer.valueOf(authentication.getName());
        }

        return userRepo.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }

    private ArticleDTO toDto(Article article) {
        return ArticleDTO.builder()
            .id(article.getId())
            .authorId(article.getAuthor().getId())
            .authorName(article.getAuthor().getFullName())
            .categoryId(article.getCategory().getId())
            .categoryName(article.getCategory().getName())
            .coverImage(article.getCoverImage())
            .title(article.getTitle())
            .sapo(article.getSapo())
            .content(article.getContent())
            .type(article.getType())
            .status(article.getStatus())
            .rejectionReason(article.getRejectionReason())
            .viewCount(article.getViewCount())
            .createdAt(article.getCreatedAt())
            .build();
    }
    
}
