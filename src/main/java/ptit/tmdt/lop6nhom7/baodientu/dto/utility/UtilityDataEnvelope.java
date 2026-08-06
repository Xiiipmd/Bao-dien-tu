package ptit.tmdt.lop6nhom7.baodientu.dto.utility;

public record UtilityDataEnvelope<T>(
        T data,
        String source,
        String updatedAt) {
}
