package com.mapin.content.controller;

import com.mapin.content.application.ContentPerspectiveAnalysisService;
import com.mapin.content.dto.ContentPerspectiveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentPerspectiveAnalysisController {

    private final ContentPerspectiveAnalysisService contentPerspectiveAnalysisService;

    @PostMapping("/{contentId}/analyze-perspective")
    public ResponseEntity<ContentPerspectiveResponse> analyzePerspective(
            @PathVariable Long contentId
    ) {
        return ResponseEntity.ok(contentPerspectiveAnalysisService.analyze(contentId));
    }
}
