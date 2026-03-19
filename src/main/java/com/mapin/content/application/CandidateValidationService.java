package com.mapin.content.application;

import com.mapin.content.domain.Content;
import com.mapin.content.domain.classification.ContentScope;
import com.mapin.content.dto.RecommendationCandidate;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CandidateValidationService {

    public RecommendationCandidate validate(Content source, Content candidate, double topicSimilarity) {
        if (!safeEquals(source.getCategory(), candidate.getCategory())) {
            log.debug("[CandidateValidation] filtered-out: category mismatch sourceId={} candidateId={} sourceCategory={} candidateCategory={}",
                    source.getId(), candidate.getId(), source.getCategory(), candidate.getCategory());
            return null;
        }

        if (topicSimilarity < 0.5) {
            log.debug("[CandidateValidation] filtered-out: topic too low sourceId={} candidateId={} topic={}",
                    source.getId(), candidate.getId(), String.format("%.3f", topicSimilarity));
            return null;
        }
        int perspectiveDistance = calculatePerspectiveDistance(source, candidate);
        double frameDifferenceScore = binaryDifferenceScore(source.getFrame(), candidate.getFrame());
        double scopeDifferenceScore = scopeDifferenceScore(source.getScope(), candidate.getScope());
        double toneDifferenceScore = binaryDifferenceScore(source.getTone(), candidate.getTone());
        double formatDifferenceScore = binaryDifferenceScore(source.getFormat(), candidate.getFormat());
        double perspectiveSupportScore = perspectiveSupportScore(perspectiveDistance);
        double qualityScore = calculateQualityScore(candidate);

        double candidateScore =
                0.50 * topicSimilarity
                        + 0.30 * frameDifferenceScore
                        + 0.07 * toneDifferenceScore
                        + 0.05 * formatDifferenceScore
                        + 0.04 * scopeDifferenceScore
                        + 0.02 * qualityScore
                        + 0.02 * perspectiveSupportScore;

        boolean qualified = candidateScore >= 0.60;

        if (!qualified) {
            log.debug("[CandidateValidation] rejected sourceId={} candidateId={} topic={} frameDiff={} toneDiff={} formatDiff={} scopeDiff={} score={}",
                    source.getId(), candidate.getId(),
                    String.format("%.3f", topicSimilarity),
                    String.format("%.3f", frameDifferenceScore),
                    String.format("%.3f", toneDifferenceScore),
                    String.format("%.3f", formatDifferenceScore),
                    String.format("%.3f", scopeDifferenceScore),
                    String.format("%.3f", candidateScore));
        } else {
            log.debug("[CandidateValidation] accepted sourceId={} candidateId={} score={} topic={} frameDiff={} toneDiff={} formatDiff={} scopeDiff={} quality={}",
                    source.getId(), candidate.getId(),
                    String.format("%.3f", candidateScore),
                    String.format("%.3f", topicSimilarity),
                    String.format("%.3f", frameDifferenceScore),
                    String.format("%.3f", toneDifferenceScore),
                    String.format("%.3f", formatDifferenceScore),
                    String.format("%.3f", scopeDifferenceScore),
                    String.format("%.3f", qualityScore));
        }

        return new RecommendationCandidate(
                candidate,
                topicSimilarity,
                frameDifferenceScore,
                scopeDifferenceScore,
                toneDifferenceScore,
                formatDifferenceScore,
                perspectiveSupportScore,
                qualityScore,
                candidateScore,
                perspectiveDistance,
                qualified
        );
    }

    private int calculatePerspectiveDistance(Content source, Content candidate) {
        int distance = 0;

        if (!safeEquals(source.getPerspectiveLevel(), candidate.getPerspectiveLevel())) {
            distance++;
        }

        if (!safeEquals(source.getPerspectiveStakeholder(), candidate.getPerspectiveStakeholder())) {
            distance++;
        }

        return distance;
    }

    private double perspectiveSupportScore(int distance) {
        return switch (distance) {
            case 2 -> 1.0;
            case 1 -> 0.7;
            default -> 0.2;
        };
    }

    private double calculateQualityScore(Content candidate) {
        Long viewCount = candidate.getViewCount();
        if (viewCount == null) {
            return 0.3;
        }

        if (viewCount >= 1_000_000) return 1.0;
        if (viewCount >= 100_000) return 0.8;
        if (viewCount >= 10_000) return 0.6;
        if (viewCount >= 1_000) return 0.4;
        return 0.2;
    }

    private double binaryDifferenceScore(String sourceValue, String candidateValue) {
        if (isBlank(sourceValue) || isBlank(candidateValue)) {
            return 0.4;
        }
        return sourceValue.equals(candidateValue) ? 0.0 : 1.0;
    }

    private double scopeDifferenceScore(String sourceValue, String candidateValue) {
        Optional<ContentScope> sourceScope = ContentScope.fromLabel(sourceValue);
        Optional<ContentScope> targetScope = ContentScope.fromLabel(candidateValue);

        if (sourceScope.isEmpty() || targetScope.isEmpty()) {
            return 0.4;
        }

        int gap = Math.abs(sourceScope.get().order() - targetScope.get().order());
        return switch (gap) {
            case 0 -> 0.0;
            case 1 -> 1.0;
            case 2 -> 0.7;
            default -> 0.4;
        };
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
