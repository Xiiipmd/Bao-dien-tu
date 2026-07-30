package ptit.tmdt.lop6nhom7.baodientu.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ptit.tmdt.lop6nhom7.baodientu.dto.NewsNotificationResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.UserPreferencesRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.UserPreferencesResponse;
import ptit.tmdt.lop6nhom7.baodientu.service.NewsNotificationService;
import ptit.tmdt.lop6nhom7.baodientu.service.UserPreferenceService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserExperienceController {
  private final UserPreferenceService preferenceService;
  private final NewsNotificationService notificationService;

  @GetMapping("/preferences")
  public ResponseEntity<UserPreferencesResponse> getPreferences(Authentication authentication) {
    return ResponseEntity.ok(preferenceService.getPreferences(userId(authentication)));
  }

  @PutMapping("/preferences")
  public ResponseEntity<UserPreferencesResponse> updatePreferences(
      Authentication authentication,
      @Valid @RequestBody UserPreferencesRequest request
  ) {
    return ResponseEntity.ok(preferenceService.updatePreferences(userId(authentication), request));
  }

  @GetMapping("/notifications")
  public ResponseEntity<List<NewsNotificationResponse>> getNotifications(Authentication authentication) {
    return ResponseEntity.ok(notificationService.getNotifications(userId(authentication)));
  }

  @GetMapping("/notifications/unread-count")
  public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
    return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId(authentication))));
  }

  @PatchMapping("/notifications/{notificationId}/read")
  public ResponseEntity<Void> markRead(
      Authentication authentication,
      @PathVariable Long notificationId
  ) {
    notificationService.markRead(userId(authentication), notificationId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/notifications/read-all")
  public ResponseEntity<Void> markAllRead(Authentication authentication) {
    notificationService.markAllRead(userId(authentication));
    return ResponseEntity.noContent().build();
  }

  private Integer userId(Authentication authentication) {
    return (Integer) authentication.getPrincipal();
  }
}
