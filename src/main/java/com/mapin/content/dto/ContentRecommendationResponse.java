package com.mapin.content.dto;

import java.time.OffsetDateTime;

public record ContentRecommendationResponse(
        Long contentId,
        String canonicalUrl,
        String title,
        String thumbnailUrl,
        String channelTitle,
        OffsetDateTime publishedAt,
        String category,
        String frame,
        String scope,
        String tone,
        String format,
        String perspectiveLevel,
        String perspectiveStakeholder,
        double topicSimilarity,
        int perspectiveDistance,
        double candidateScore,
        double diversityScore,
        double selectionScore
) {
}
