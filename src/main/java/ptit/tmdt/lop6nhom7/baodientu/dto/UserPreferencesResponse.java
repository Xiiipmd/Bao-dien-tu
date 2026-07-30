package ptit.tmdt.lop6nhom7.baodientu.dto;

import java.util.List;

public record UserPreferencesResponse(
    List<CategoryDTO> selectedTopics,
    boolean pushNotificationsEnabled
) {}
