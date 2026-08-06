package ptit.tmdt.lop6nhom7.baodientu.dto.utility;

public record FootballMatchDto(
        long id,
        String competitionId,
        String competition,
        String competitionCode,
        String season,
        Integer matchday,
        String utcDate,
        String localDate,
        String status,
        Integer minute,
        String stage,
        Team homeTeam,
        Team awayTeam,
        Score score,
        String venue,
        String updatedAt) {

    public record Team(long id, String name, String shortName, String crest) {
    }

    public record Score(
            Integer home,
            Integer away,
            Integer halfTimeHome,
            Integer halfTimeAway,
            Integer extraTimeHome,
            Integer extraTimeAway,
            Integer penaltiesHome,
            Integer penaltiesAway,
            String duration) {
    }
}
