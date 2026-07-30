package ptit.tmdt.lop6nhom7.baodientu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ptit.tmdt.lop6nhom7.baodientu.dto.VipPackageResponse;
import ptit.tmdt.lop6nhom7.baodientu.entity.VipPackage;
import ptit.tmdt.lop6nhom7.baodientu.repository.VipPackageRepo;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VipPackageService {

    private final VipPackageRepo vipPackageRepository;

    /** Lấy toàn bộ danh sách gói VIP */
    public List<VipPackageResponse> getAllPackages() {
        return vipPackageRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Lấy chi tiết một gói VIP theo id */
    public VipPackageResponse getPackageById(Integer id) {
        VipPackage vipPackage = vipPackageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói VIP với id: " + id));
        return toResponse(vipPackage);
    }

    // -------------------------------------------------------------------------

    private VipPackageResponse toResponse(VipPackage p) {
        return VipPackageResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .durationDays(p.getDurationDays())
                .price(p.getPrice())
                .discountPercent(p.getDiscountPercent())
                .description(p.getDescription())
                .build();
    }
}
