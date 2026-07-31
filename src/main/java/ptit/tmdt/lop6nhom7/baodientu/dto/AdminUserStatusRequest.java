package ptit.tmdt.lop6nhom7.baodientu.dto;

import jakarta.validation.constraints.NotNull;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;

public record AdminUserStatusRequest(
    @NotNull UserStatus status
) {
}
