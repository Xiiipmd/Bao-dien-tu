package ptit.tmdt.lop6nhom7.baodientu.dto;

import java.time.Instant;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;

public record AdminUserResponse(
    Integer id,
    String fullName,
    String email,
    UserRole role,
    UserStatus status,
    Instant createdAt
) {
}
