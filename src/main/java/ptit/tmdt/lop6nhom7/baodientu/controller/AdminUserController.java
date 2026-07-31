package ptit.tmdt.lop6nhom7.baodientu.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ptit.tmdt.lop6nhom7.baodientu.dto.AdminUserResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.AdminUserRoleRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.AdminUserStatusRequest;
import ptit.tmdt.lop6nhom7.baodientu.service.AdminUserService;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {
  private final AdminUserService adminUserService;

  @GetMapping
  public ResponseEntity<List<AdminUserResponse>> getUsers(
      @RequestParam(value = "q", required = false) String keyword) {
    return ResponseEntity.ok(adminUserService.getUsers(keyword));
  }

  @PatchMapping("/{userId}/role")
  public ResponseEntity<AdminUserResponse> updateRole(
      @PathVariable Integer userId,
      @Valid @RequestBody AdminUserRoleRequest request) {
    return ResponseEntity.ok(adminUserService.updateRole(userId, request.role()));
  }

  @PatchMapping("/{userId}/status")
  public ResponseEntity<AdminUserResponse> updateStatus(
      @PathVariable Integer userId,
      @Valid @RequestBody AdminUserStatusRequest request) {
    return ResponseEntity.ok(adminUserService.updateStatus(userId, request.status()));
  }
}
