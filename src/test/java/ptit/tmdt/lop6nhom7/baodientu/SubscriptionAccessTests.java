package ptit.tmdt.lop6nhom7.baodientu;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.ForbiddenException;
import ptit.tmdt.lop6nhom7.baodientu.repository.CategoryRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.SubscriptionRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;
import ptit.tmdt.lop6nhom7.baodientu.service.SubscriptionService;
import ptit.tmdt.lop6nhom7.baodientu.service.VipAccessService;

@ExtendWith(MockitoExtension.class)
class SubscriptionAccessTests {

  @Mock
  private SubscriptionRepo subscriptionRepo;
  @Mock
  private UserRepo userRepo;
  @Mock
  private CategoryRepo categoryRepo;
  @Mock
  private VipAccessService vipAccessService;
  @InjectMocks
  private SubscriptionService subscriptionService;

  @Test
  void regularMemberCanLoadFollowedJournalists() {
    User member = member(UserStatus.ACTIVE);
    when(vipAccessService.requireCurrentUser()).thenReturn(member);
    when(subscriptionRepo.findByUserOrderByIdDesc(member)).thenReturn(List.of());

    subscriptionService.getMySubscriptions();

    verify(vipAccessService).requireCurrentUser();
    verify(subscriptionRepo).findByUserOrderByIdDesc(member);
  }

  @Test
  void lockedMemberCannotChangeSubscriptions() {
    when(vipAccessService.requireCurrentUser()).thenReturn(member(UserStatus.LOCKED));

    assertThrows(
        ForbiddenException.class,
        () -> subscriptionService.getMySubscriptions()
    );
  }

  private User member(UserStatus status) {
    User user = new User();
    user.setId(10);
    user.setRole(UserRole.MEMBER);
    user.setStatus(status);
    return user;
  }
}
