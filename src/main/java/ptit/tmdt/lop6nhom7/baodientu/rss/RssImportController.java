package ptit.tmdt.lop6nhom7.baodientu.rss;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rss")
@RequiredArgsConstructor
public class RssImportController {

    private final RssImportService rssImportService;

    @PostMapping("/import")
    public ResponseEntity<String> triggerImport() {
        rssImportService.importAll();
        return ResponseEntity.ok("RSS import triggered successfully");
    }
}
