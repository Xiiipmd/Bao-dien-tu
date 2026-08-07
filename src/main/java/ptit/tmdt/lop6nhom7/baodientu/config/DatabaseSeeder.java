package ptit.tmdt.lop6nhom7.baodientu.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import ptit.tmdt.lop6nhom7.baodientu.entity.Category;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;
import ptit.tmdt.lop6nhom7.baodientu.repository.CategoryRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final CategoryRepo categoryRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Checking database category seeding...");
        List<String> categoriesToSeed = List.of(
            "Công nghệ",
            "Kinh doanh",
            "Đời sống",
            "Khoa học",
            "Tin mới nhất",
            "Thời sự",
            "Thế giới",
            "Pháp luật",
            "Sức khỏe",
            "Giáo dục",
            "Thể thao",
            "Giải trí",
            "Du lịch",
            "Xe",
            "Bất động sản"
        );

        List<Category> existingCategories = categoryRepo.findAll();

        for (String catName : categoriesToSeed) {
            boolean exists = existingCategories.stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(catName));
            if (!exists) {
                log.info("Seeding category: {}", catName);
                Category category = new Category();
                category.setName(catName);
                categoryRepo.save(category);
            }
        }
        log.info("Category seeding verification completed.");

        log.info("Checking database VIP tester user seeding...");
        String testerEmail = "viptester@gmail.com";
        if (!userRepo.existsByEmail(testerEmail)) {
            log.info("Seeding VIP tester user: {}", testerEmail);
            User tester = new User();
            tester.setFullName("VIP Tester");
            tester.setEmail(testerEmail);
            // bcrypt for "12345678" dynamically
            tester.setPasswordHash(passwordEncoder.encode("12345678"));
            tester.setRole(UserRole.VIP);
            tester.setStatus(UserStatus.ACTIVE);
            tester.setFreeArticlesLeft(999);
            tester.setVipExpiryDate(Instant.now().plus(3650, ChronoUnit.DAYS)); // 10 years VIP
            userRepo.save(tester);
            log.info("VIP tester user seeded successfully.");
        } else {
            // Update to VIP and reset password to 12345678 to ensure it matches
            userRepo.findByEmail(testerEmail).ifPresent(user -> {
                user.setRole(UserRole.VIP);
                user.setPasswordHash(passwordEncoder.encode("12345678"));
                user.setVipExpiryDate(Instant.now().plus(3650, ChronoUnit.DAYS));
                userRepo.save(user);
                log.info("Updated/Ensured existing tester account is VIP role and password reset to 12345678.");
            });
        }
    }
}
