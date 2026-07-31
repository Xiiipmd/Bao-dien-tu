package ptit.tmdt.lop6nhom7.baodientu.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.tmdt.lop6nhom7.baodientu.dto.AdminUserResponse;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

@Service
@RequiredArgsConstructor
public class AdminUserService {
  private static final Set<UserRole> ASSIGNABLE_ROLES = EnumSet.of(
      UserRole.MEMBER,
      UserRole.AUTHOR,
      UserRole.CENSOR,
      UserRole.ADMIN
  );

  private final UserRepo userRepo;

  @Transactional(readOnly = true)
  public List<AdminUserResponse> getUsers(String keyword) {
    String normalizedKeyword = keyword == null ? "" : keyword.trim();
    List<User> users = normalizedKeyword.isBlank()
        ? userRepo.findAllByOrderByCreatedAtDesc()
        : userRepo.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc(
            normalizedKeyword,
            normalizedKeyword
        );
    return users.stream().map(this::toResponse).toList();
  }

  @Transactional
  public AdminUserResponse updateRole(Integer userId, UserRole role) {
    if (role == null || !ASSIGNABLE_ROLES.contains(role)) {
      throw new BadRequestException("Vai trò không hợp lệ");
    }
    assertNotCurrentUser(userId, "Không thể tự thay đổi vai trò quản trị");
    User user = getUser(userId);
    user.setRole(role);
    return toResponse(userRepo.save(user));
  }

  @Transactional
  public AdminUserResponse updateStatus(Integer userId, UserStatus status) {
    if (status == null) {
      throw new BadRequestException("Trạng thái tài khoản không hợp lệ");
    }
    assertNotCurrentUser(userId, "Không thể tự khóa tài khoản đang đăng nhập");
    User user = getUser(userId);
    user.setStatus(status);
    return toResponse(userRepo.save(user));
  }

  private User getUser(Integer userId) {
    return userRepo.findById(userId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
  }

  private void assertNotCurrentUser(Integer userId, String message) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getPrincipal() == null) {
      return;
    }
    Integer currentUserId = authentication.getPrincipal() instanceof Integer principalId
        ? principalId
        : Integer.valueOf(authentication.getName());
    if (currentUserId.equals(userId)) {
      throw new BadRequestException(message);
    }
  }

  private AdminUserResponse toResponse(User user) {
    return new AdminUserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getRole(),
        user.getStatus(),
        user.getCreatedAt()
    );
  }
}
