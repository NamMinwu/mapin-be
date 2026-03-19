package com.mapin.content.domain.classification;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum ContentTone {
    NEUTRAL("중립"),
    WARNING("경고"),
    CRITICAL("비판"),
    OPTIMISTIC("낙관"),
    ANALYSIS("해설");

    private static final Map<String, ContentTone> BY_LABEL = Arrays.stream(values())
            .collect(Collectors.toMap(ContentTone::label, tone -> tone));

    private final String label;

    ContentTone(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<ContentTone> fromLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_LABEL.get(label.strip()));
    }
}
