package ptit.tmdt.lop6nhom7.baodientu.repository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.entity.Category;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Integer> {
  Optional<User> findByEmail(@NotBlank(message = "Email khong duoc bo trong") @Email String email);
  
  Optional<User> findById(Long id);
  boolean existsByEmail(@NotBlank(message = "Email khong duoc bo trong") @Email String email);
  boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
  List<User> findByRoleOrderByFullNameAsc(UserRole role);
  List<User> findAllByOrderByCreatedAtDesc();
  List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc(
      String fullName,
      String email
  );

  @EntityGraph(attributePaths = "preferredCategories")
  List<User> findAll();

  @EntityGraph(attributePaths = "preferredCategories")
  Optional<User> findWithPreferredCategoriesById(Integer id);

  @EntityGraph(attributePaths = "preferredCategories")
  @org.springframework.data.jpa.repository.Query("""
      select distinct u
      from User u
      where (u.status is null or u.status <> :lockedStatus)
        and u.pushNotificationsEnabled = true
        and u.id <> :authorId
        and :category member of u.preferredCategories
      """)
  List<User> findNewsNotificationRecipients(
      @org.springframework.data.repository.query.Param("category") Category category,
      @org.springframework.data.repository.query.Param("lockedStatus") UserStatus lockedStatus,
      @org.springframework.data.repository.query.Param("authorId") Integer authorId
  );

  @org.springframework.data.jpa.repository.Query("""
      select u
      from User u
      where u.role in :roles
        and (u.status is null or u.status <> :lockedStatus)
        and u.pushNotificationsEnabled = true
      """)
  List<User> findModerationNotificationRecipients(
      @org.springframework.data.repository.query.Param("roles") List<UserRole> roles,
      @org.springframework.data.repository.query.Param("lockedStatus") UserStatus lockedStatus
  );
}
