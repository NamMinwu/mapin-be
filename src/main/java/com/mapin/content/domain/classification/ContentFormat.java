package com.mapin.content.domain.classification;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum ContentFormat {
    NEWS("뉴스"),
    INTERVIEW("인터뷰"),
    DEBATE("토론"),
    EXPLANATION("해설"),
    DOCUMENTARY("다큐/리포트"),
    FIELD_VLOG("현장/브이로그"),
    LECTURE("강연/교육");

    private static final Map<String, ContentFormat> BY_LABEL = Arrays.stream(values())
            .collect(Collectors.toMap(ContentFormat::label, format -> format));

    private final String label;

    ContentFormat(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<ContentFormat> fromLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_LABEL.get(label.strip()));
    }
}
