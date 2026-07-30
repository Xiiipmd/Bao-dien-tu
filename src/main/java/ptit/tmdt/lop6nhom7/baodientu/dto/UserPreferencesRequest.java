package ptit.tmdt.lop6nhom7.baodientu.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UserPreferencesRequest(
    @NotNull Set<Integer> categoryIds,
    boolean pushNotificationsEnabled
) {}
