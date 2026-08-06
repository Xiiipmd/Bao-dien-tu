package ptit.tmdt.lop6nhom7.baodientu.dto.utility;

import java.math.BigDecimal;

public record ExchangeRateDto(
        String code,
        String name,
        BigDecimal cashBuy,
        BigDecimal transferBuy,
        BigDecimal sell) {
}
