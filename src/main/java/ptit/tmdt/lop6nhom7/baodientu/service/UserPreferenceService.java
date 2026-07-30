package ptit.tmdt.lop6nhom7.baodientu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.tmdt.lop6nhom7.baodientu.dto.CategoryDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.UserPreferencesRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.UserPreferencesResponse;
import ptit.tmdt.lop6nhom7.baodientu.entity.Category;
import ptit.tmdt.lop6nhom7.baodientu.entity.Subscription;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.SubscriptionTargetType;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.CategoryRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.SubscriptionRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {
  private final UserRepo userRepo;
  private final CategoryRepo categoryRepo;
  private final SubscriptionRepo subscriptionRepo;

  @Transactional(readOnly = true)
  public UserPreferencesResponse getPreferences(Integer userId) {
    return toResponse(getUser(userId));
  }

  @Transactional
  public UserPreferencesResponse updatePreferences(Integer userId, UserPreferencesRequest request) {
    Set<Integer> requestedIds = request.categoryIds() == null ? Set.of() : request.categoryIds();
    List<Category> categories = categoryRepo.findAllById(requestedIds);
    if (categories.size() != requestedIds.size()) {
      throw new BadRequestException("Một hoặc nhiều chủ đề không tồn tại");
    }

    User user = getUser(userId);
    user.setPreferredCategories(new LinkedHashSet<>(categories));
    user.setPushNotificationsEnabled(request.pushNotificationsEnabled());
    User savedUser = userRepo.save(user);
    syncCategoryEmailSubscriptions(savedUser, requestedIds);
    return toResponse(savedUser);
  }

  private void syncCategoryEmailSubscriptions(User user, Set<Integer> selectedCategoryIds) {
    List<Subscription> currentSubscriptions = subscriptionRepo.findByUserAndTargetType(
        user,
        SubscriptionTargetType.CATEGORY
    );

    List<Subscription> removedSubscriptions = currentSubscriptions.stream()
        .filter(subscription -> !selectedCategoryIds.contains(subscription.getTargetId()))
        .toList();
    subscriptionRepo.deleteAll(removedSubscriptions);

    Set<Integer> subscribedCategoryIds = currentSubscriptions.stream()
        .map(Subscription::getTargetId)
        .collect(java.util.stream.Collectors.toSet());
    List<Subscription> addedSubscriptions = selectedCategoryIds.stream()
        .filter(categoryId -> !subscribedCategoryIds.contains(categoryId))
        .map(categoryId -> {
          Subscription subscription = new Subscription();
          subscription.setUser(user);
          subscription.setTargetType(SubscriptionTargetType.CATEGORY);
          subscription.setTargetId(categoryId);
          return subscription;
        })
        .toList();
    subscriptionRepo.saveAll(addedSubscriptions);
  }

  private User getUser(Integer userId) {
    return userRepo.findWithPreferredCategoriesById(userId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
  }

  private UserPreferencesResponse toResponse(User user) {
    List<CategoryDTO> topics = user.getPreferredCategories().stream()
        .sorted(Comparator.comparing(Category::getName))
        .map(category -> new CategoryDTO(category.getId(), category.getName()))
        .toList();
    return new UserPreferencesResponse(topics, !Boolean.FALSE.equals(user.getPushNotificationsEnabled()));
  }
}
