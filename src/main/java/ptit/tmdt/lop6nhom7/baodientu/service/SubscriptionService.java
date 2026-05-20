package ptit.tmdt.lop6nhom7.baodientu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.tmdt.lop6nhom7.baodientu.dto.SubscriptionRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.SubscriptionResponse;
import ptit.tmdt.lop6nhom7.baodientu.entity.Category;
import ptit.tmdt.lop6nhom7.baodientu.entity.Subscription;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.SubscriptionTargetType;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.exception.BadRequestException;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.CategoryRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.SubscriptionRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
  private final SubscriptionRepo subscriptionRepo;
  private final UserRepo userRepo;
  private final CategoryRepo categoryRepo;
  private final VipAccessService vipAccessService;

  @Transactional(readOnly = true)
  public List<SubscriptionResponse> getMySubscriptions() {
    User currentUser = vipAccessService.requireCurrentVipUser();
    return subscriptionRepo.findByUserOrderByIdDesc(currentUser)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public SubscriptionResponse subscribe(SubscriptionRequest request) {
    User currentUser = vipAccessService.requireCurrentVipUser();
    validateTarget(request.targetType(), request.targetId());

    return subscriptionRepo.findByUserAndTargetTypeAndTargetId(
            currentUser,
            request.targetType(),
            request.targetId()
        )
        .map(this::toResponse)
        .orElseGet(() -> {
          Subscription subscription = new Subscription();
          subscription.setUser(currentUser);
          subscription.setTargetType(request.targetType());
          subscription.setTargetId(request.targetId());
          return toResponse(subscriptionRepo.save(subscription));
        });
  }

  @Transactional
  public void unsubscribe(String rawTargetType, Integer targetId) {
    User currentUser = vipAccessService.requireCurrentVipUser();
    SubscriptionTargetType targetType = parseTargetType(rawTargetType);

    Subscription subscription = subscriptionRepo.findByUserAndTargetTypeAndTargetId(
            currentUser,
            targetType,
            targetId
        )
        .orElseThrow(() -> new NotFoundException("Khong tim thay dang ky theo doi can huy"));

    subscriptionRepo.delete(subscription);
  }

  private void validateTarget(SubscriptionTargetType targetType, Integer targetId) {
    if (targetType == SubscriptionTargetType.AUTHOR) {
      User author = userRepo.findById(targetId)
          .orElseThrow(() -> new NotFoundException("Khong tim thay tac gia voi id = " + targetId));
      if (author.getRole() != UserRole.AUTHOR) {
        throw new BadRequestException("Nguoi dung duoc chon khong phai la tac gia");
      }
      return;
    }

    categoryRepo.findById(targetId)
        .orElseThrow(() -> new NotFoundException("Khong tim thay chu de voi id = " + targetId));
  }

  private SubscriptionResponse toResponse(Subscription subscription) {
    return new SubscriptionResponse(
        subscription.getId(),
        subscription.getTargetType(),
        subscription.getTargetId(),
        resolveTargetName(subscription.getTargetType(), subscription.getTargetId())
    );
  }

  private String resolveTargetName(SubscriptionTargetType targetType, Integer targetId) {
    if (targetType == SubscriptionTargetType.AUTHOR) {
      return userRepo.findById(targetId)
          .map(User::getFullName)
          .orElse("Tac gia #" + targetId);
    }
    return categoryRepo.findById(targetId)
        .map(Category::getName)
        .orElse("Chu de #" + targetId);
  }

  private SubscriptionTargetType parseTargetType(String rawTargetType) {
    try {
      return SubscriptionTargetType.valueOf(rawTargetType.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("Loai doi tuong theo doi chi chap nhan AUTHOR hoac CATEGORY");
    }
  }
}
