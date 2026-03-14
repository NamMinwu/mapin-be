package com.mapin.content.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QdrantScoredPoint(
        Object id,
        double score,
        Map<String, Object> payload
) {
}
