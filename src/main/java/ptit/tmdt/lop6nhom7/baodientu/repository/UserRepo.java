package ptit.tmdt.lop6nhom7.baodientu.repository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Integer> {
  Optional<User> findByEmail(@NotBlank(message = "Email khong duoc bo trong") @Email String email);
  
  Optional<User> findById(Long id);
  boolean existsByEmail(@NotBlank(message = "Email khong duoc bo trong") @Email String email);
  List<User> findByRoleOrderByFullNameAsc(UserRole role);
}
