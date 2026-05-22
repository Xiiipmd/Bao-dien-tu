package ptit.tmdt.lop6nhom7.baodientu.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ptit.tmdt.lop6nhom7.baodientu.dto.CategoryDTO;
import ptit.tmdt.lop6nhom7.baodientu.repository.CategoryRepo;

@Service
@RequiredArgsConstructor
public class CategoryService {
  private final CategoryRepo categoryRepo;

  @Transactional(readOnly = true)
  public List<CategoryDTO> getCategories() {
    return categoryRepo.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
        .map(category -> new CategoryDTO(category.getId(), category.getName()))
        .toList();
  }
}