package com.mapin.content.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QdrantQueryResult(
        List<QdrantScoredPoint> points
) {
}
