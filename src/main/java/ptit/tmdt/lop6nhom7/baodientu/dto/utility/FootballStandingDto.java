package ptit.tmdt.lop6nhom7.baodientu.dto.utility;

public record FootballStandingDto(
        int position,
        long teamId,
        String team,
        String crest,
        int played,
        int won,
        int draw,
        int lost,
        int goalDifference,
        int points) {
}
