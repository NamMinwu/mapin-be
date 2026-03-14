package com.mapin.content.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QdrantQueryResponse(
        String status,
        QdrantQueryResult result
) {
}
