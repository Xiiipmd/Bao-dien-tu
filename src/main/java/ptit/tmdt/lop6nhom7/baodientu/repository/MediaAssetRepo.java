package ptit.tmdt.lop6nhom7.baodientu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ptit.tmdt.lop6nhom7.baodientu.entity.MediaAsset;

@Repository
public interface MediaAssetRepo extends JpaRepository<MediaAsset, Integer> {
}
