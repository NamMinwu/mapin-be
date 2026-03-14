package com.mapin.content.controller;

import com.mapin.content.application.ContentIngestService;
import com.mapin.content.dto.ContentResponse;
import com.mapin.content.dto.IngestYoutubeContentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentIngestController {

    private final ContentIngestService contentIngestService;

    @PostMapping("/ingest/youtube")
    public ResponseEntity<ContentResponse> ingestYoutube(
            @RequestBody IngestYoutubeContentRequest request
    ) {
        return ResponseEntity.ok(
                contentIngestService.ingestYoutubeUrl(request.url())
        );
    }
}
