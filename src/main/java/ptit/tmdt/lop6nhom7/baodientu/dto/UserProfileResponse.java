package ptit.tmdt.lop6nhom7.baodientu.dto;

import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;

import java.time.Instant;

public record UserProfileResponse(
    Integer id,
    String fullName,
    String email,
    UserRole role,
    Instant vipExpiryDate,
    Instant createdAt,
    String avatar
) {}
