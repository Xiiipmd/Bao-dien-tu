package ptit.tmdt.lop6nhom7.baodientu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.entity.ArticleView;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleViewRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;
import ptit.tmdt.lop6nhom7.baodientu.service.AnonymousReadMeterService;
import ptit.tmdt.lop6nhom7.baodientu.service.ArticleService;
import ptit.tmdt.lop6nhom7.baodientu.service.NewsNotificationService;

@ExtendWith(MockitoExtension.class)
class ArticleReaderCountTests {

  @Mock
  private ArticleRepo articleRepo;
  @Mock
  private ArticleViewRepo articleViewRepo;
  @Mock
  private UserRepo userRepo;
  @Mock
  private AnonymousReadMeterService anonymousReadMeterService;
  @Mock
  private NewsNotificationService newsNotificationService;
  @InjectMocks
  private ArticleService articleService;

  private Article article;

  @BeforeEach
  void setUp() {
    article = new Article();
    article.setId(31);
    article.setTitle("Bai kiem thu");
    article.setSapo("Tom tat");
    article.setContent("Noi dung");
    article.setType(ArticleType.FREE);
    article.setStatus(ArticleStatus.PUBLISHED);
    article.setViewCount(7);
    when(articleRepo.findByIdAndStatus(31, ArticleStatus.PUBLISHED))
        .thenReturn(Optional.of(article));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void firstAnonymousReaderIncrementsPublicCounter() {
    when(articleViewRepo.existsByArticleIdAndReaderIdentity(
        org.mockito.ArgumentMatchers.eq(31),
        anyString()
    )).thenReturn(false);

    var response = articleService.readArticle(31, "mobile-device-a");

    assertEquals(8, response.getViewCount());
    verify(articleRepo).save(article);
    verify(newsNotificationService).notifyIfHot(article);

    ArgumentCaptor<ArticleView> event = ArgumentCaptor.forClass(ArticleView.class);
    verify(articleViewRepo).save(event.capture());
    assertNotNull(event.getValue().getViewedAt());
    assertTrue(event.getValue().getReaderIdentity().startsWith("DEVICE:"));
  }

  @Test
  void repeatedAnonymousReaderKeepsPageViewButNotPublicCounter() {
    when(articleViewRepo.existsByArticleIdAndReaderIdentity(
        org.mockito.ArgumentMatchers.eq(31),
        anyString()
    )).thenReturn(true);

    var response = articleService.readArticle(31, "mobile-device-a");

    assertEquals(7, response.getViewCount());
    verify(articleViewRepo).save(any(ArticleView.class));
    verify(articleRepo, never()).save(any(Article.class));
    verify(newsNotificationService, never()).notifyIfHot(any(Article.class));
  }

  @Test
  void repeatedAuthenticatedReaderDoesNotIncrementPublicCounter() {
    Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("5");
    SecurityContextHolder.getContext().setAuthentication(authentication);

    User reader = new User();
    reader.setId(5);
    when(userRepo.findById(5)).thenReturn(Optional.of(reader));
    when(articleViewRepo.existsByUserIdAndArticleId(5, 31)).thenReturn(true);

    articleService.readArticle(31, "ignored-for-account");

    ArgumentCaptor<ArticleView> event = ArgumentCaptor.forClass(ArticleView.class);
    verify(articleViewRepo).save(event.capture());
    assertEquals(reader, event.getValue().getUser());
    assertEquals(null, event.getValue().getReaderIdentity());
    verify(articleRepo, never()).save(any(Article.class));
  }
}
