package com.mapin.content.application;

import com.mapin.content.domain.Content;
import com.mapin.content.domain.ContentRepository;
import com.mapin.content.dto.ContentResponse;
import com.mapin.content.port.YoutubeSearchClient;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FallbackCandidateExpansionService {

    private final SearchQueryGenerationService searchQueryGenerationService;
    private final YoutubeSearchClient youtubeSearchClient;
    private final ContentRepository contentRepository;
    private final ContentEmbeddingService contentEmbeddingService;
    private final ContentPerspectiveAnalysisService contentPerspectiveAnalysisService;
    private final ContentIngestService contentIngestService;

    public void expand(Content source) {
        List<String> queries = searchQueryGenerationService.generate(source);
        Set<String> candidateVideoIds = new LinkedHashSet<>();

        for (String query : queries) {
            List<String> videoIds = youtubeSearchClient.searchVideoIds(query, 5);
            candidateVideoIds.addAll(videoIds);
        }

        candidateVideoIds.remove(source.getExternalContentId());

        for (String videoId : candidateVideoIds) {
            String url = "https://www.youtube.com/watch?v=" + videoId;

            ContentResponse response = contentIngestService.ingestYoutubeUrl(url);
            Long contentId = response.id();

            Content candidate = contentRepository.findById(contentId)
                    .orElseThrow(() -> new IllegalStateException("후보 콘텐츠를 찾을 수 없습니다. id=" + contentId));

            if (candidate.getVectorId() == null || candidate.getVectorId().isBlank()) {
                contentEmbeddingService.embed(contentId);
            }

            if (candidate.getPerspectiveLevel() == null || candidate.getPerspectiveStakeholder() == null) {
                contentPerspectiveAnalysisService.analyze(contentId);
            }
        }
    }
}
