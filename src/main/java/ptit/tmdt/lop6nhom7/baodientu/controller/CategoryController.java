package ptit.tmdt.lop6nhom7.baodientu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.CategoryDTO;
import ptit.tmdt.lop6nhom7.baodientu.service.CategoryService;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {
  private final CategoryService categoryService;

  @GetMapping
  public ResponseEntity<List<CategoryDTO>> getCategories() {
    log.info("Fetching categories");
    return ResponseEntity.ok(categoryService.getCategories());
  }
}