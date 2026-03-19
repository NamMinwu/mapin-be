package com.mapin.content.domain.classification;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum ContentScope {
    PERSONAL("개인", 0),
    ORGANIZATION("조직/산업", 1),
    NATIONAL("국가", 2),
    INTERNATIONAL("국제", 3),
    CIVILIZATION("장기/문명", 4);

    private static final Map<String, ContentScope> BY_LABEL = Arrays.stream(values())
            .collect(Collectors.toMap(ContentScope::label, scope -> scope));

    private final String label;
    private final int order;

    ContentScope(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String label() {
        return label;
    }

    public int order() {
        return order;
    }

    public static Optional<ContentScope> fromLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_LABEL.get(label.strip()));
    }
}
