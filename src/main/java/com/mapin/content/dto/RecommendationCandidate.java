package com.mapin.content.dto;

import com.mapin.content.domain.Content;

public record RecommendationCandidate(
        Content content,
        double topicSimilarity,
        double frameDifferenceScore,
        double scopeDifferenceScore,
        double toneDifferenceScore,
        double formatDifferenceScore,
        double perspectiveSupportScore,
        double qualityScore,
        double candidateScore,
        int perspectiveDistance,
        boolean qualified
) {
}
