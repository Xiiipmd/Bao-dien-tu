package ptit.tmdt.lop6nhom7.baodientu.rss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RssImportScheduler {

    private final RssImportService rssImportService;

    // Run RSS import scheduler: by default every 15 minutes (900,000 milliseconds)
    @Scheduled(fixedDelayString = "${news.rss.refresh-delay-ms:900000}")
    public void importNews() {
        log.info("Starting scheduled RSS news import job...");
        try {
            rssImportService.importAll();
        } catch (Exception e) {
            log.error("Error occurred during scheduled RSS import job", e);
        }
    }
}
