package ptit.tmdt.lop6nhom7.baodientu.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import ptit.tmdt.lop6nhom7.baodientu.dto.PublicUserProfileResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.UserCommentActivityResponse;
import ptit.tmdt.lop6nhom7.baodientu.service.PublicUserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class PublicUserController {

  private final PublicUserService publicUserService;

  @GetMapping("/{userId}/public-profile")
  public ResponseEntity<PublicUserProfileResponse> getPublicProfile(@PathVariable Integer userId) {
    return ResponseEntity.ok(publicUserService.getPublicProfile(userId));
  }

  @GetMapping("/{userId}/comments")
  public ResponseEntity<Page<UserCommentActivityResponse>> getUserComments(
      @PathVariable Integer userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(publicUserService.getUserComments(userId, PageRequest.of(page, size)));
  }
}
