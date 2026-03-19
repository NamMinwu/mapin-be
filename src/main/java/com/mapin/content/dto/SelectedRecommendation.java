package com.mapin.content.dto;

public record SelectedRecommendation(
        RecommendationCandidate candidate,
        double diversityScore,
        double selectionScore
) {
}
