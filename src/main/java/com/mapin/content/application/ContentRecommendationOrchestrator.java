package com.mapin.content.application;

import com.mapin.content.domain.Content;
import com.mapin.content.domain.ContentRepository;
import com.mapin.content.dto.ContentRecommendationResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ContentRecommendationOrchestrator {

    private final ContentRepository contentRepository;
    private final InternalRecommendationService internalRecommendationService;
    private final FallbackCandidateExpansionService fallbackCandidateExpansionService;

    public List<ContentRecommendationResponse> recommend(Long sourceContentId, int topK) {
        Content source = contentRepository.findById(sourceContentId)
                .orElseThrow(() -> new IllegalArgumentException("콘텐츠를 찾을 수 없습니다. id=" + sourceContentId));

        if (source.getVectorId() == null || source.getVectorId().isBlank()) {
            throw new IllegalStateException("임베딩되지 않은 콘텐츠입니다. id=" + sourceContentId);
        }

        List<ContentRecommendationResponse> initialResults = internalRecommendationService.recommend(source, topK);
        if (initialResults.size() >= topK) {
            return initialResults;
        }

        fallbackCandidateExpansionService.expand(source);
        return internalRecommendationService.recommend(source, topK);
    }
}
