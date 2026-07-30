package ptit.tmdt.lop6nhom7.baodientu.dto;

import ptit.tmdt.lop6nhom7.baodientu.enums.NotificationType;

import java.time.Instant;

public record NewsNotificationResponse(
    Long id,
    Integer articleId,
    String articleImage,
    String categoryName,
    NotificationType type,
    String title,
    String message,
    boolean read,
    Instant createdAt
) {}
