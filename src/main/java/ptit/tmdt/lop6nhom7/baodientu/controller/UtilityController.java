package ptit.tmdt.lop6nhom7.baodientu.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import ptit.tmdt.lop6nhom7.baodientu.dto.utility.ExchangeRateDto;
import ptit.tmdt.lop6nhom7.baodientu.dto.utility.FootballMatchDto;
import ptit.tmdt.lop6nhom7.baodientu.dto.utility.FootballStandingDto;
import ptit.tmdt.lop6nhom7.baodientu.dto.utility.UtilityDataEnvelope;
import ptit.tmdt.lop6nhom7.baodientu.service.ExchangeRateService;
import ptit.tmdt.lop6nhom7.baodientu.service.FootballDataService;
import ptit.tmdt.lop6nhom7.baodientu.service.OptionalUtilityProviderService;

@RestController
@RequestMapping("/api/utilities")
@RequiredArgsConstructor
public class UtilityController {
    private final FootballDataService footballDataService;
    private final ExchangeRateService exchangeRateService;
    private final OptionalUtilityProviderService optionalProviderService;

    @GetMapping("/football/matches")
    public UtilityDataEnvelope<List<FootballMatchDto>> footballMatches(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String competition) {
        if (dateTo.isBefore(dateFrom) || ChronoUnit.DAYS.between(dateFrom, dateTo) > 31) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khoảng ngày Bóng đá phải từ 0 đến 31 ngày");
        }
        return footballDataService.getMatches(dateFrom, dateTo, competition);
    }

    @GetMapping("/football/standings")
    public UtilityDataEnvelope<List<FootballStandingDto>> footballStandings(
            @RequestParam String competition,
            @RequestParam(required = false) Integer season) {
        return footballDataService.getStandings(competition, season);
    }

    @GetMapping("/finance/exchange-rates")
    public UtilityDataEnvelope<List<ExchangeRateDto>> exchangeRates() {
        return exchangeRateService.getRates();
    }

    @GetMapping("/lottery")
    public JsonNode lottery(
            @RequestParam String region,
            @RequestParam(required = false) String province,
            @RequestParam String drawDate,
            @RequestParam(required = false) String lotteryType) {
        return optionalProviderService.getLottery(region, province, drawDate, lotteryType);
    }

    @GetMapping("/finance/gold")
    public JsonNode gold() {
        return optionalProviderService.getGold();
    }
}
