package ptit.tmdt.lop6nhom7.baodientu.dto;

import ptit.tmdt.lop6nhom7.baodientu.enums.SubscriptionTargetType;

public record SubscriptionResponse(
    Integer id,
    SubscriptionTargetType targetType,
    Integer targetId,
    String targetName
) {
}
