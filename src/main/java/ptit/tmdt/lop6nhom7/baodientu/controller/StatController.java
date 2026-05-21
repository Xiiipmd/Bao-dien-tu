package ptit.tmdt.lop6nhom7.baodientu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.AuthorStatDTO;
import ptit.tmdt.lop6nhom7.baodientu.service.StatService;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Slf4j
public class StatController {
    private final StatService statService;

    @GetMapping("/author")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<AuthorStatDTO> getAuthorStat(
        @RequestParam int authorId,
        @RequestParam String startDate,
        @RequestParam String endDate
    ) {
        return ResponseEntity.ok(statService.getAuthorStat(authorId, startDate, endDate));
    }

}
