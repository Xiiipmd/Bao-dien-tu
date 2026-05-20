package ptit.tmdt.lop6nhom7.baodientu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ptit.tmdt.lop6nhom7.baodientu.enums.SubscriptionTargetType;

public record SubscriptionRequest(
    @NotNull(message = "Loai doi tuong theo doi khong duoc de trong")
    SubscriptionTargetType targetType,

    @NotNull(message = "ID doi tuong theo doi khong duoc de trong")
    @Positive(message = "ID doi tuong theo doi phai la so duong")
    Integer targetId
) {
}
