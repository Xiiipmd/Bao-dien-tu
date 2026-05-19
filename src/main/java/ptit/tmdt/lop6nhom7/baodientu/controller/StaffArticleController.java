package ptit.tmdt.lop6nhom7.baodientu.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.service.StaffArticleService;


@RestController
@RequestMapping("/api/staff/articles")
@RequiredArgsConstructor
@Slf4j
public class StaffArticleController {

    private final StaffArticleService staffArticleService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<Void> createNewArticle(@Valid @RequestBody ArticleDTO articleDTO) {
        log.info("Creating new article with title: {}", articleDTO.getTitle());
        staffArticleService.createArticle(articleDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
}
