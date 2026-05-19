package ptit.tmdt.lop6nhom7.baodientu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ptit.tmdt.lop6nhom7.baodientu.entity.Category;

@Repository
public interface CategoryRepo extends JpaRepository<Category, Integer> {

}
