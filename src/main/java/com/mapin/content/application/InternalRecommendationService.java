package com.mapin.content.application;

import com.mapin.content.domain.Content;
import com.mapin.content.domain.ContentRepository;
import com.mapin.content.dto.ContentRecommendationResponse;
import com.mapin.content.dto.RecommendationCandidate;
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

    public List<ContentRecommendationResponse> recommend(Content source, int topK) {
        List<VectorSearchResult> searchResults = vectorStoreClient.searchById(source.getVectorId(), topK + 5);

        List<String> candidateVectorIds = searchResults.stream()
                .map(VectorSearchResult::vectorId)
                .filter(vectorId -> !vectorId.equals(source.getVectorId()))
                .toList();

        if (candidateVectorIds.isEmpty()) {
            return List.of();
        }

        Map<String, Content> contentByVectorId = contentRepository.findAllByVectorIdIn(candidateVectorIds).stream()
                .collect(Collectors.toMap(Content::getVectorId, Function.identity()));

        List<RecommendationCandidate> candidates = searchResults.stream()
                .filter(result -> !result.vectorId().equals(source.getVectorId()))
                .map(result -> {
                    Content candidate = contentByVectorId.get(result.vectorId());
                    if (candidate == null) {
                        return null;
                    }
                    return candidateValidationService.validate(source, candidate, result.similarityScore());
                })
                .filter(Objects::nonNull)
                .filter(RecommendationCandidate::qualified)
                .sorted(Comparator.comparing(RecommendationCandidate::finalScore).reversed())
                .limit(topK)
                .toList();

        return candidates.stream()
                .map(candidate -> new ContentRecommendationResponse(
                        candidate.content().getId(),
                        candidate.content().getCanonicalUrl(),
                        candidate.content().getTitle(),
                        candidate.content().getThumbnailUrl(),
                        candidate.content().getChannelTitle(),
                        candidate.content().getPublishedAt(),
                        candidate.content().getCategory(),
                        candidate.content().getPerspectiveLevel(),
                        candidate.content().getPerspectiveStakeholder(),
                        candidate.topicSimilarity(),
                        candidate.perspectiveDistance(),
                        candidate.finalScore()
                ))
                .toList();
    }
}
