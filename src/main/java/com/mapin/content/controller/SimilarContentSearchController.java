package com.mapin.content.controller;

import com.mapin.content.application.SimilarContentSearchService;
import com.mapin.content.dto.SimilarContentResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class SimilarContentSearchController {

    private final SimilarContentSearchService similarContentSearchService;

    @GetMapping("/{contentId}/similar")
    public ResponseEntity<List<SimilarContentResponse>> searchSimilar(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "5") int topK
    ) {
        return ResponseEntity.ok(similarContentSearchService.search(contentId, topK));
    }
}
