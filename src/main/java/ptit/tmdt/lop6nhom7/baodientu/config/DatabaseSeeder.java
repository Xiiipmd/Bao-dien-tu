package ptit.tmdt.lop6nhom7.baodientu.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ptit.tmdt.lop6nhom7.baodientu.entity.Category;
import ptit.tmdt.lop6nhom7.baodientu.repository.CategoryRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final CategoryRepo categoryRepo;

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
    }
}
