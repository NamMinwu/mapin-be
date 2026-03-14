package com.mapin.content.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QdrantUpsertResponse(
        String status
) {
}
