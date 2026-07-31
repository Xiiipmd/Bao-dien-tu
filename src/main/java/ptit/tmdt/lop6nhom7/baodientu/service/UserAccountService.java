package ptit.tmdt.lop6nhom7.baodientu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.tmdt.lop6nhom7.baodientu.dto.ChangePasswordRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.UpdateAvatarRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.UpdateUserProfileRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.UserProfileResponse;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.ConflictException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.exception.UnauthorizedException;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserAccountService {
  private final UserRepo userRepo;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(Integer userId) {
    return toResponse(getUser(userId));
  }

  @Transactional
  public UserProfileResponse updateProfile(Integer userId, UpdateUserProfileRequest request) {
    User user = getUser(userId);
    String fullName = request.fullName().trim();
    String email = request.email().trim().toLowerCase(Locale.ROOT);

    if (userRepo.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
      throw new ConflictException("Email này đã được sử dụng bởi tài khoản khác");
    }

    user.setFullName(fullName);
    user.setEmail(email);
    return toResponse(userRepo.save(user));
  }

  @Transactional
  public void changePassword(Integer userId, ChangePasswordRequest request) {
    User user = getUser(userId);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new UnauthorizedException("Mật khẩu hiện tại không chính xác");
    }
    if (!request.newPassword().equals(request.confirmation())) {
      throw new BadRequestException("Xác nhận mật khẩu mới không khớp");
    }
    if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
      throw new BadRequestException("Mật khẩu mới phải khác mật khẩu hiện tại");
    }

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepo.save(user);
  }

  @Transactional
  public UserProfileResponse updateAvatar(Integer userId, UpdateAvatarRequest request) {
    User user = getUser(userId);
    user.setAvatarUrl(request.avatar().trim());
    return toResponse(userRepo.save(user));
  }

  private User getUser(Integer userId) {
    return userRepo.findById(userId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));
  }

  private UserProfileResponse toResponse(User user) {
    return new UserProfileResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getRole(),
        user.getVipExpiryDate(),
        user.getCreatedAt(),
        user.getAvatarUrl()
    );
  }
}
