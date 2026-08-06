package ptit.tmdt.lop6nhom7.baodientu.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;

import ptit.tmdt.lop6nhom7.baodientu.dto.utility.FootballMatchDto;
import ptit.tmdt.lop6nhom7.baodientu.dto.utility.FootballStandingDto;
import ptit.tmdt.lop6nhom7.baodientu.dto.utility.UtilityDataEnvelope;

@Service
public class FootballDataService {
    private static final String SOURCE = "football-data.org";
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RestClient client;
    private final String apiKey;

    public FootballDataService(
            @Value("${FOOTBALL_DATA_API_KEY:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.client = RestClient.builder()
                .baseUrl("https://api.football-data.org/v4")
                .defaultHeader(HttpHeaders.USER_AGENT, "NewsDaily/1.0")
                .build();
    }

    public UtilityDataEnvelope<List<FootballMatchDto>> getMatches(
            LocalDate dateFrom,
            LocalDate dateTo,
            String competition) {
        requireApiKey();
        JsonNode root = client.get()
                .uri(uri -> {
                    var builder = uri.path("/matches")
                            .queryParam("dateFrom", dateFrom)
                            .queryParam("dateTo", dateTo);
                    if (StringUtils.hasText(competition)) {
                        builder.queryParam("competitions", competition.trim());
                    }
                    return builder.build();
                })
                .header("X-Auth-Token", apiKey)
                .retrieve()
                .body(JsonNode.class);

        List<FootballMatchDto> matches = new ArrayList<>();
        if (root != null && root.path("matches").isArray()) {
            root.path("matches").forEach(node -> matches.add(mapMatch(node)));
        }
        matches.sort(Comparator.comparing(FootballMatchDto::utcDate));
        String updatedAt = matches.stream()
                .map(FootballMatchDto::updatedAt)
                .filter(StringUtils::hasText)
                .max(String::compareTo)
                .orElseGet(() -> Instant.now().toString());
        return new UtilityDataEnvelope<>(matches, SOURCE, updatedAt);
    }

    public UtilityDataEnvelope<List<FootballStandingDto>> getStandings(
            String competition,
            Integer season) {
        requireApiKey();
        if (!StringUtils.hasText(competition)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cần chọn giải đấu để tải bảng xếp hạng");
        }

        JsonNode root = client.get()
                .uri(uri -> {
                    var builder = uri.path("/competitions/{competition}/standings");
                    if (season != null) builder.queryParam("season", season);
                    return builder.build(competition.trim());
                })
                .header("X-Auth-Token", apiKey)
                .retrieve()
                .body(JsonNode.class);

        List<FootballStandingDto> rows = new ArrayList<>();
        JsonNode standings = root == null ? null : root.path("standings");
        if (standings != null && standings.isArray()) {
            JsonNode total = null;
            for (JsonNode standing : standings) {
                if ("TOTAL".equals(standing.path("type").asText())) {
                    total = standing;
                    break;
                }
            }
            if (total == null && !standings.isEmpty()) total = standings.get(0);
            if (total != null && total.path("table").isArray()) {
                total.path("table").forEach(row -> rows.add(mapStanding(row)));
            }
        }
        return new UtilityDataEnvelope<>(rows, SOURCE, Instant.now().toString());
    }

    private void requireApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Nguồn Bóng đá chưa được cấu hình trên server");
        }
    }

    private FootballMatchDto mapMatch(JsonNode node) {
        JsonNode competition = node.path("competition");
        JsonNode season = node.path("season");
        JsonNode home = node.path("homeTeam");
        JsonNode away = node.path("awayTeam");
        JsonNode score = node.path("score");
        String utcDate = text(node, "utcDate");
        String localDate = utcDate;
        try {
            localDate = ZonedDateTime.parse(utcDate)
                    .withZoneSameInstant(VIETNAM_ZONE)
                    .toLocalDateTime()
                    .toString();
        } catch (RuntimeException ignored) {
            // Preserve the provider value when an unexpected date format is returned.
        }
        String seasonLabel = season.path("startDate").asText("");
        if (seasonLabel.length() >= 4) {
            String end = season.path("endDate").asText("");
            seasonLabel = seasonLabel.substring(0, 4)
                    + (end.length() >= 4 ? "-" + end.substring(0, 4) : "");
        }

        return new FootballMatchDto(
                node.path("id").asLong(),
                String.valueOf(competition.path("id").asLong()),
                competition.path("name").asText("Giải đấu"),
                competition.path("code").asText(""),
                seasonLabel,
                nullableInt(node, "matchday"),
                utcDate,
                localDate,
                normalizeStatus(node.path("status").asText()),
                nullableInt(node, "minute"),
                node.path("stage").asText(""),
                mapTeam(home),
                mapTeam(away),
                new FootballMatchDto.Score(
                        nullableInt(score.path("fullTime"), "home"),
                        nullableInt(score.path("fullTime"), "away"),
                        nullableInt(score.path("halfTime"), "home"),
                        nullableInt(score.path("halfTime"), "away"),
                        nullableInt(score.path("extraTime"), "home"),
                        nullableInt(score.path("extraTime"), "away"),
                        nullableInt(score.path("penalties"), "home"),
                        nullableInt(score.path("penalties"), "away"),
                        score.path("duration").asText("")),
                node.path("venue").isNull() ? null : node.path("venue").asText(null),
                node.path("lastUpdated").asText(utcDate));
    }

    private FootballMatchDto.Team mapTeam(JsonNode node) {
        return new FootballMatchDto.Team(
                node.path("id").asLong(),
                node.path("name").asText("Chưa xác định"),
                node.path("shortName").asText(node.path("name").asText("")),
                node.path("crest").asText(null));
    }

    private FootballStandingDto mapStanding(JsonNode row) {
        JsonNode team = row.path("team");
        return new FootballStandingDto(
                row.path("position").asInt(),
                team.path("id").asLong(),
                team.path("name").asText("Chưa xác định"),
                team.path("crest").asText(null),
                row.path("playedGames").asInt(),
                row.path("won").asInt(),
                row.path("draw").asInt(),
                row.path("lost").asInt(),
                row.path("goalDifference").asInt(),
                row.path("points").asInt());
    }

    private String normalizeStatus(String value) {
        return switch (value) {
            case "SCHEDULED", "TIMED", "IN_PLAY", "PAUSED", "HALFTIME",
                    "FINISHED", "POSTPONED", "CANCELED", "SUSPENDED" -> value;
            default -> "SCHEDULED";
        };
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("");
    }

    private Integer nullableInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }
}
