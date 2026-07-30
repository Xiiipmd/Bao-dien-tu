package ptit.tmdt.lop6nhom7.baodientu.event;

public record ArticleModerationDecidedEvent(
    Integer articleId,
    boolean approved
) {}
