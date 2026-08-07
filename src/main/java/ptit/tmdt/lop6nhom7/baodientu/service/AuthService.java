package ptit.tmdt.lop6nhom7.baodientu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import ptit.tmdt.lop6nhom7.baodientu.dto.LoginReq;
import ptit.tmdt.lop6nhom7.baodientu.dto.LoginRes;
import ptit.tmdt.lop6nhom7.baodientu.dto.RegisterReq;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.ConflictException;
import ptit.tmdt.lop6nhom7.baodientu.exception.ForbiddenException;
import ptit.tmdt.lop6nhom7.baodientu.exception.UnauthorizedException;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;
import ptit.tmdt.lop6nhom7.baodientu.security.JwtService;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthService {
  private final UserRepo userRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final ObjectMapper objectMapper;
  
  public @Nullable LoginRes login(@Valid LoginReq request) {
    String email = request.getEmail().trim();
    String password = request.getPassword().trim();

    // Find user
    User u = userRepo.findByEmail(email)
        .orElseThrow(() -> new UnauthorizedException("Sai tên đăng nhập hoặc mật khẩu"));
    // Check status
    if (u.getStatus() == UserStatus.LOCKED) {
      throw new ForbiddenException("Tài khoản người dùng đã bị khóa");
    }
    // Verify password
    if (!passwordEncoder.matches(password, u.getPasswordHash())) {
      throw new UnauthorizedException("Sai tên đăng nhập hoặc mật khẩu");
    }
    // Generate JWT
    String jwtToken = jwtService.generateToken(u);
    // return
    return new LoginRes(
        jwtToken,
        u.getRole(),
        u.getFullName(),
        u.getVipExpiryDate(),
        u.getFreeArticlesLeft(),
        u.getId(),
        u.getEmail(),
        u.getAvatarUrl()
    );
  }
  
  public void register(RegisterReq request) {
    String email = request.getEmail().trim();
    String name = request.getName().trim();
    String password = request.getPassword().trim();
    String confirmation = request.getConfirmation().trim();
    UserRole requestedRole = parseSelfRegisterRole(request.getRole());

    //
    if (!password.equals(confirmation)) {
      throw new ConflictException("Passwords do not match");
    }
    // Check duplicate email
    if (userRepo.existsByEmail(email)) {
      throw new ConflictException("Email already exists");
    }
    //
    User newUser = new User();
    newUser.setEmail(email);
    newUser.setFullName(name);
    newUser.setPasswordHash(passwordEncoder.encode(password));
    newUser.setRole(requestedRole);
    newUser.setStatus(UserStatus.ACTIVE);
    newUser.setFreeArticlesLeft(3);
    newUser.setVipExpiryDate(null);
    userRepo.save(newUser);
  }

  private UserRole parseSelfRegisterRole(String roleStr) {
    if (roleStr == null) {
      return UserRole.MEMBER;
    }
    try {
      UserRole parsed = UserRole.valueOf(roleStr.toUpperCase().trim());
      if (parsed == UserRole.AUTHOR || parsed == UserRole.CENSOR || parsed == UserRole.ADMIN) {
        throw new ConflictException("Không được phép tự đăng ký vai trò quản trị/tác giả/kiểm duyệt");
      }
      return parsed;
    } catch (IllegalArgumentException ex) {
      return UserRole.MEMBER;
    }
  }
}
