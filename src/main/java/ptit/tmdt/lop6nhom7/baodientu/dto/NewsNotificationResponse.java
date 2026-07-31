package ptit.tmdt.lop6nhom7.baodientu.dto;

import ptit.tmdt.lop6nhom7.baodientu.enums.NotificationType;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleType;

import java.time.Instant;

public record NewsNotificationResponse(
    Long id,
    Integer articleId,
    String articleImage,
    String categoryName,
    ArticleType articleType,
    NotificationType type,
    String title,
    String message,
    boolean read,
    Instant createdAt
) {}
