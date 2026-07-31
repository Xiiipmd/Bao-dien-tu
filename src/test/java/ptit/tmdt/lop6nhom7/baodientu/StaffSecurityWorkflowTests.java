package ptit.tmdt.lop6nhom7.baodientu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleUpdateRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.RegisterReq;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.entity.Category;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.exception.ConflictException;
import ptit.tmdt.lop6nhom7.baodientu.exception.ForbiddenException;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.CategoryRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;
import ptit.tmdt.lop6nhom7.baodientu.security.JwtService;
import ptit.tmdt.lop6nhom7.baodientu.service.AuthService;
import ptit.tmdt.lop6nhom7.baodientu.service.NewsNotificationService;
import ptit.tmdt.lop6nhom7.baodientu.service.StaffArticleService;

@ExtendWith(MockitoExtension.class)
class StaffSecurityWorkflowTests {
  @Mock private ArticleRepo articleRepo;
  @Mock private CategoryRepo categoryRepo;
  @Mock private UserRepo userRepo;
  @Mock private NewsNotificationService newsNotificationService;
  @InjectMocks private StaffArticleService staffArticleService;

  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private ObjectMapper objectMapper;
  @InjectMocks private AuthService authService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void draftUsesAuthenticatedAuthorAndDoesNotNotifyModerators() {
    User author = author(10);
    Category category = category(3);
    authenticateAs(10);
    when(userRepo.findById(10)).thenReturn(Optional.of(author));
    when(categoryRepo.findById(3)).thenReturn(Optional.of(category));
    when(articleRepo.save(any(Article.class))).thenAnswer(invocation -> {
      Article saved = invocation.getArgument(0);
      saved.setId(44);
      return saved;
    });

    ArticleDTO result = staffArticleService.createArticle(articleRequest(), false);

    assertEquals(ArticleStatus.DRAFT, result.getStatus());
    ArgumentCaptor<Article> savedArticle = ArgumentCaptor.forClass(Article.class);
    verify(articleRepo).save(savedArticle.capture());
    assertSame(author, savedArticle.getValue().getAuthor());
    verify(newsNotificationService, never())
        .notifyModeratorsAboutSubmission(any(Article.class), anyBoolean());
  }

  @Test
  void authorCannotEditPublishedArticleDirectly() {
    User author = author(10);
    Article article = new Article();
    article.setId(51);
    article.setAuthor(author);
    article.setCategory(category(3));
    article.setStatus(ArticleStatus.PUBLISHED);
    authenticateAs(10);
    when(userRepo.findById(10)).thenReturn(Optional.of(author));
    when(articleRepo.findById(51)).thenReturn(Optional.of(article));

    assertThrows(
        ForbiddenException.class,
        () -> staffArticleService.updateArticle(51, articleRequest())
    );
  }

  @Test
  void publicRegistrationCannotCreateAuthorAccount() {
    RegisterReq request = new RegisterReq();
    request.setEmail("author@example.com");
    request.setName("Tác giả thử nghiệm");
    request.setPassword("password123");
    request.setConfirmation("password123");
    request.setRole("AUTHOR");

    assertThrows(ConflictException.class, () -> authService.register(request));
  }

  private void authenticateAs(Integer userId) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(userId, null)
    );
  }

  private User author(Integer id) {
    User user = new User();
    user.setId(id);
    user.setFullName("Phạm Minh Đức");
    user.setRole(UserRole.AUTHOR);
    return user;
  }

  private Category category(Integer id) {
    Category category = new Category();
    category.setId(id);
    category.setName("Công nghệ");
    return category;
  }

  private ArticleUpdateRequest articleRequest() {
    ArticleUpdateRequest request = new ArticleUpdateRequest();
    request.setCoverImage("https://example.com/cover.jpg");
    request.setCategoryId(3);
    request.setTitle("Bài viết thử nghiệm");
    request.setSapo("Phần giới thiệu bài viết.");
    request.setContent("<p>Nội dung bài viết.</p>");
    request.setType(ArticleType.FREE);
    return request;
  }
}
