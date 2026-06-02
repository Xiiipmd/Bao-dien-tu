package ptit.tmdt.lop6nhom7.baodientu.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.ipc.http.HttpSender.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.AdminOverviewStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.AdminTopStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.AuthorStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.dto.StatOptionDTO;
import ptit.tmdt.lop6nhom7.baodientu.service.StatService;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Slf4j
public class StatController {
    private final StatService statService;

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR')")
    @GetMapping("/article")
    public ResponseEntity<ArticleStatDTO> getArticleStat(
        @RequestParam int articleId,
        @RequestParam Instant startDate,
        @RequestParam Instant endDate,
        @RequestParam String granularity
    ) {
        return ResponseEntity.ok(statService.calcArticleStat(articleId, startDate, endDate, granularity));
    }

    @GetMapping("/author")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<AuthorStatDTO> getAuthorStat(
        @RequestParam int authorId,
        @RequestParam String startDate,
        @RequestParam String endDate,
        @RequestParam(defaultValue = "day") String groupBy
    ) {
        return ResponseEntity.ok(statService.getAuthorStat(authorId, startDate, endDate, groupBy));
    }

    @GetMapping("/admin/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminOverviewStatDTO> getAdminOverviewStat(
        @RequestParam(required = false) Integer authorId,
        @RequestParam(required = false) Integer categoryId,
        @RequestParam String startDate,
        @RequestParam String endDate,
        @RequestParam(defaultValue = "day") String groupBy
    ) {
        return ResponseEntity.ok(statService.getAdminOverviewStat(authorId, categoryId, startDate, endDate, groupBy));
    }

    @GetMapping("/admin/top")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminTopStatDTO>> getAdminTopStats(
        @RequestParam(defaultValue = "author") String targetType,
        @RequestParam(defaultValue = "revenue") String sortBy,
        @RequestParam String startDate,
        @RequestParam String endDate,
        @RequestParam(defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(statService.getAdminTopStats(targetType, sortBy, startDate, endDate, limit));
    }

    @GetMapping("/admin/authors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StatOptionDTO>> getAuthorOptions() {
        return ResponseEntity.ok(statService.getAuthorOptions());
    }
}
