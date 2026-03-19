package com.mapin.content.application;

import com.mapin.content.domain.Content;
import com.mapin.content.domain.ContentRepository;
import com.mapin.content.dto.ContentRecommendationResponse;
import com.mapin.content.dto.RecommendationCandidate;
import com.mapin.content.dto.SelectedRecommendation;
import com.mapin.content.port.VectorSearchResult;
import com.mapin.content.port.VectorStoreClient;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InternalRecommendationService {

    private final ContentRepository contentRepository;
    private final VectorStoreClient vectorStoreClient;
    private final CandidateValidationService candidateValidationService;
    private final FinalRecommendationSelector finalRecommendationSelector;

    public List<ContentRecommendationResponse> recommend(Content source, int topK) {
        int searchSize = Math.max(topK * 5, topK + 5);
        List<VectorSearchResult> searchResults = vectorStoreClient.searchById(source.getVectorId(), searchSize);

        List<String> candidateVectorIds = searchResults.stream()
                .map(VectorSearchResult::vectorId)
                .filter(vectorId -> !vectorId.equals(source.getVectorId()))
                .toList();

        if (candidateVectorIds.isEmpty()) {
            return List.of();
        }

        Map<String, Content> contentByVectorId = contentRepository.findAllByVectorIdIn(candidateVectorIds).stream()
                .collect(Collectors.toMap(Content::getVectorId, Function.identity()));

        int candidateLimit = Math.max(topK * 5, 15);
        List<RecommendationCandidate> candidates = searchResults.stream()
                .filter(result -> !result.vectorId().equals(source.getVectorId()))
                .map(result -> {
                    Content candidate = contentByVectorId.get(result.vectorId());
                    if (candidate == null) {
                        return null;
                    }
                    if (!sameCategory(source, candidate)) {
                        return null;
                    }
                    return candidateValidationService.validate(source, candidate, result.similarityScore());
                })
                .filter(Objects::nonNull)
                .filter(RecommendationCandidate::qualified)
                .sorted(Comparator.comparing(RecommendationCandidate::candidateScore).reversed())
                .limit(candidateLimit)
                .toList();

        List<SelectedRecommendation> selected = finalRecommendationSelector.select(candidates, topK);

        return selected.stream()
                .map(selection -> {
                    RecommendationCandidate candidate = selection.candidate();
                    return new ContentRecommendationResponse(
                            candidate.content().getId(),
                            candidate.content().getCanonicalUrl(),
                            candidate.content().getTitle(),
                            candidate.content().getThumbnailUrl(),
                            candidate.content().getChannelTitle(),
                            candidate.content().getPublishedAt(),
                            candidate.content().getCategory(),
                            candidate.content().getFrame(),
                            candidate.content().getScope(),
                            candidate.content().getTone(),
                            candidate.content().getFormat(),
                            candidate.content().getPerspectiveLevel(),
                            candidate.content().getPerspectiveStakeholder(),
                            candidate.topicSimilarity(),
                            candidate.perspectiveDistance(),
                            candidate.candidateScore(),
                            selection.diversityScore(),
                            selection.selectionScore()
                    );
                })
                .toList();
    }

    private boolean sameCategory(Content source, Content candidate) {
        String sourceCategory = source.getCategory();
        String candidateCategory = candidate.getCategory();
        if (sourceCategory == null || candidateCategory == null) {
            return false;
        }
        return sourceCategory.equals(candidateCategory);
    }
}
