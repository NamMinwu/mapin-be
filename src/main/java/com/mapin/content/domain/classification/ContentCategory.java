package com.mapin.content.domain.classification;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum ContentCategory {
    POLITICS("정치"),
    ECONOMY("경제"),
    SOCIETY("사회"),
    LIFESTYLE_CULTURE("생활/문화"),
    IT_SCIENCE("IT/과학"),
    WORLD("세계"),
    ENTERTAINMENT("연예"),
    SPORTS("스포츠");

    private static final Map<String, ContentCategory> BY_LABEL = Arrays.stream(values())
            .collect(Collectors.toMap(ContentCategory::label, category -> category));

    private final String label;

    ContentCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<ContentCategory> fromLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_LABEL.get(label.strip()));
    }
}
