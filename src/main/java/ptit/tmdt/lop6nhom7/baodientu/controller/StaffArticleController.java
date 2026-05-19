package ptit.tmdt.lop6nhom7.baodientu.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleDTO;
import ptit.tmdt.lop6nhom7.baodientu.service.StaffArticleService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Controller
@RequestMapping("/api/staff/articles")
@RequiredArgsConstructor
@Slf4j
public class StaffArticleController {

    private final StaffArticleService privateArticleService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('AUTHOR')")
    public void createNewArticle(@Valid @RequestBody ArticleDTO articleDTO) {
        privateArticleService.createArticle(articleDTO);
    }
    
}
