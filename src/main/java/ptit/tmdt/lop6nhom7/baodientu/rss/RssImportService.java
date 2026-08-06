package ptit.tmdt.lop6nhom7.baodientu.rss;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.tmdt.lop6nhom7.baodientu.entity.Article;
import ptit.tmdt.lop6nhom7.baodientu.entity.Category;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleOrigin;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;
import ptit.tmdt.lop6nhom7.baodientu.repository.ArticleRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.CategoryRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RssImportService {

    private final ArticleRepo articleRepo;
    private final CategoryRepo categoryRepo;
    private final UserRepo userRepo;
    private final RssSourceProperties rssSourceProperties;

    @Transactional
    public void importAll() {
        if (!rssSourceProperties.isEnabled()) {
            log.info("RSS import is disabled by configuration");
            return;
        }

        // Retrieve or create the default RSS author
        User rssAuthor = userRepo.findByEmail("rss-author@thedaily.com").orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail("rss-author@thedaily.com");
            newUser.setFullName("RSS Automated Ingestion");
            newUser.setPasswordHash("$2a$10$dummyhashForSecurityRssParserCompatibilityOnly");
            newUser.setRole(UserRole.AUTHOR);
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setFreeArticlesLeft(9999);
            newUser.setCreatedAt(Instant.now());
            return userRepo.save(newUser);
        });

        List<RssSourceProperties.FeedSource> sources = rssSourceProperties.getSources();
        if (sources == null || sources.isEmpty()) {
            log.warn("No RSS sources configured");
            return;
        }

        int importedCount = 0;
        int duplicateCount = 0;

        for (RssSourceProperties.FeedSource source : sources) {
            try {
                log.info("Importing RSS feed from source: {} ({})", source.getName(), source.getUrl());
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed;
                try (InputStream feedStream = URI.create(source.getUrl()).toURL().openStream();
                     XmlReader reader = new XmlReader(feedStream)) {
                    feed = input.build(reader);
                }

                // Find or create category
                final String catName = source.getDefaultCategory() != null ? source.getDefaultCategory().trim() : "Tin tức";
                List<Category> categories = categoryRepo.findAll();
                Category category = categories.stream()
                        .filter(c -> c.getName().equalsIgnoreCase(catName))
                        .findFirst()
                        .orElseGet(() -> {
                            Category newCat = new Category();
                            newCat.setName(catName);
                            return categoryRepo.save(newCat);
                        });

                for (SyndEntry entry : feed.getEntries()) {
                    String originalUrl = entry.getLink();
                    if (originalUrl == null || originalUrl.isBlank()) {
                        continue;
                    }

                    // Generate a unique externalId using SHA-256 of the URL
                    String externalId = sha256(originalUrl.trim());

                    // Check for duplicate
                    if (articleRepo.existsByExternalId(externalId) || articleRepo.existsByOriginalUrl(originalUrl)) {
                        duplicateCount++;
                        continue;
                    }

                    String title = entry.getTitle();
                    if (title == null || title.isBlank()) {
                        continue;
                    }

                    // Extract cover image and description from description block (CDATA HTML)
                    String sapo = "";
                    String coverImage = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800"; // default newspaper placeholder
                    if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
                        String rawDescription = entry.getDescription().getValue();
                        Document doc = Jsoup.parse(rawDescription);
                        
                        // Extract img tag src
                        Element img = doc.selectFirst("img");
                        if (img != null) {
                            String src = img.attr("src");
                            if (src != null && !src.isBlank()) {
                                coverImage = src;
                            }
                        }
                        
                        // Sapo text is the plain text of the description
                        sapo = doc.text();
                        if (sapo.length() > 500) {
                            sapo = sapo.substring(0, 497) + "...";
                        }
                    }

                    if (sapo.isBlank()) {
                        sapo = title;
                    }

                    Article article = new Article();
                    article.setAuthor(rssAuthor);
                    article.setCategory(category);
                    article.setTitle(title);
                    article.setSapo(sapo);
                    article.setContent("Đọc bài viết gốc tại: " + originalUrl); // Stub content
                    article.setCoverImage(coverImage);
                    article.setType(ArticleType.FREE);
                    article.setStatus(ArticleStatus.PUBLISHED);
                    article.setViewCount(0);
                    
                    Instant pubDate = entry.getPublishedDate() != null ? entry.getPublishedDate().toInstant() : Instant.now();
                    article.setCreatedAt(pubDate);
                    article.setPublishedAt(pubDate);
                    
                    article.setOrigin(ArticleOrigin.EXTERNAL);
                    article.setOriginalUrl(originalUrl);
                    article.setSourceName(source.getName());
                    article.setExternalId(externalId);

                    articleRepo.save(article);
                    importedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to import RSS feed from: {}", source.getUrl(), e);
            }
        }

        log.info("RSS Ingestion completed: {} new articles imported, {} duplicates skipped", importedCount, duplicateCount);
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }
}
