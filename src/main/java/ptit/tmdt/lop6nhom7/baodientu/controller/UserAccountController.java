package ptit.tmdt.lop6nhom7.baodientu.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ptit.tmdt.lop6nhom7.baodientu.dto.ChangePasswordRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.UpdateUserProfileRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.UserProfileResponse;
import ptit.tmdt.lop6nhom7.baodientu.service.UserAccountService;

@RestController
@RequestMapping("/api/me/account")
@RequiredArgsConstructor
public class UserAccountController {
  private final UserAccountService userAccountService;

  @GetMapping
  public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
    return ResponseEntity.ok(userAccountService.getProfile(userId(authentication)));
  }

  @PutMapping
  public ResponseEntity<UserProfileResponse> updateProfile(
      Authentication authentication,
      @Valid @RequestBody UpdateUserProfileRequest request
  ) {
    return ResponseEntity.ok(userAccountService.updateProfile(userId(authentication), request));
  }

  @PutMapping("/password")
  public ResponseEntity<Void> changePassword(
      Authentication authentication,
      @Valid @RequestBody ChangePasswordRequest request
  ) {
    userAccountService.changePassword(userId(authentication), request);
    return ResponseEntity.noContent().build();
  }

  private Integer userId(Authentication authentication) {
    return (Integer) authentication.getPrincipal();
  }
}
