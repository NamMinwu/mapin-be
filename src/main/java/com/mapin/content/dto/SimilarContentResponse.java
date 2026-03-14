package com.mapin.content.dto;

import java.time.OffsetDateTime;

public record SimilarContentResponse(
        Long contentId,
        String canonicalUrl,
        String title,
        String thumbnailUrl,
        String channelTitle,
        OffsetDateTime publishedAt,
        String vectorId,
        double similarityScore
) {
}
