package ptit.tmdt.lop6nhom7.baodientu.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class OptionalUtilityProviderService {
    private final String lotteryUrl;
    private final String goldUrl;
    private final RestClient client = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "NewsDaily/1.0")
            .build();

    public OptionalUtilityProviderService(
            @Value("${LOTTERY_DATA_API_URL:}") String lotteryUrl,
            @Value("${GOLD_DATA_API_URL:}") String goldUrl) {
        this.lotteryUrl = lotteryUrl == null ? "" : lotteryUrl.trim();
        this.goldUrl = goldUrl == null ? "" : goldUrl.trim();
    }

    public JsonNode getLottery(String region, String province, String drawDate, String lotteryType) {
        if (!StringUtils.hasText(lotteryUrl)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Nguồn Xổ số chưa được cấu hình trên server");
        }
        URI uri = UriComponentsBuilder.fromUriString(lotteryUrl)
                .queryParam("region", region)
                .queryParamIfPresent("province", optional(province))
                .queryParam("drawDate", drawDate)
                .queryParamIfPresent("lotteryType", optional(lotteryType))
                .build(true)
                .toUri();
        return client.get().uri(uri).retrieve().body(JsonNode.class);
    }

    public JsonNode getGold() {
        if (!StringUtils.hasText(goldUrl)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Nguồn Giá vàng chưa được cấu hình trên server");
        }
        return client.get().uri(goldUrl).retrieve().body(JsonNode.class);
    }

    private java.util.Optional<String> optional(String value) {
        return StringUtils.hasText(value)
                ? java.util.Optional.of(value.trim())
                : java.util.Optional.empty();
    }
}
