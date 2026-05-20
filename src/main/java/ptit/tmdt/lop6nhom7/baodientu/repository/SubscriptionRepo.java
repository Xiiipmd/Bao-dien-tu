package ptit.tmdt.lop6nhom7.baodientu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ptit.tmdt.lop6nhom7.baodientu.entity.Subscription;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.SubscriptionTargetType;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepo extends JpaRepository<Subscription, Integer> {
  List<Subscription> findByUserOrderByIdDesc(User user);

  Optional<Subscription> findByUserAndTargetTypeAndTargetId(
      User user,
      SubscriptionTargetType targetType,
      Integer targetId
  );

  List<Subscription> findByTargetTypeAndTargetId(
      SubscriptionTargetType targetType,
      Integer targetId
  );
}
