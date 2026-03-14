package com.mapin.content.dto;

public record ContentEmbeddingResponse(
        Long contentId,
        String embeddingModel,
        String vectorId,
        String embeddingText
) {
}
