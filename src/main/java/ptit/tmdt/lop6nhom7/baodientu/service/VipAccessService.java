package ptit.tmdt.lop6nhom7.baodientu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.ForbiddenException;
import ptit.tmdt.lop6nhom7.baodientu.exception.UnauthorizedException;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VipAccessService {
  private final UserRepo userRepo;

  public User requireCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new UnauthorizedException("Vui long dang nhap de thuc hien chuc nang nay");
    }

    Integer userId = resolveUserId(authentication.getName());
    return userRepo.findById(userId)
        .orElseThrow(() -> new UnauthorizedException("Khong tim thay thong tin nguoi dung"));
  }

  public User requireCurrentVipUser() {
    User user = requireCurrentUser();
    if (user.getStatus() == UserStatus.LOCKED) {
      throw new ForbiddenException("Nguoi dung bi khoa tai khoan");
    }
    if (user.getVipExpiryDate() == null || !user.getVipExpiryDate().isAfter(Instant.now())) {
      throw new ForbiddenException("Tinh nang nay chi danh cho thanh vien VIP con hieu luc");
    }
    return user;
  }

  private Integer resolveUserId(String value) {
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException ex) {
      throw new UnauthorizedException("Token dang nhap khong hop le");
    }
  }
}
