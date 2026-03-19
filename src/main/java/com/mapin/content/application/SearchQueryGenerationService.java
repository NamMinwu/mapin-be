package com.mapin.content.application;

import com.mapin.content.domain.Content;
import com.mapin.content.domain.classification.CategoryFrame;
import com.mapin.content.domain.classification.ContentCategory;
import com.mapin.content.port.SearchQuerySuggestionClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchQueryGenerationService {

    private static final int MAX_AI_SUGGESTIONS = 5;
    private final SearchQuerySuggestionClient searchQuerySuggestionClient;

    public List<String> generate(Content source) {
        String base = normalizeBaseQuery(source.getTitle());

        List<String> queries = new ArrayList<>();
        queries.add(base);

        List<String> aiSuggestions = fetchAiSuggestions(source, base);
        if (aiSuggestions.isEmpty()) {
            queries.addAll(buildFallbackQueries(source, base));
        } else {
            queries.addAll(aiSuggestions);
            if (queries.size() < MAX_AI_SUGGESTIONS) {
                queries.addAll(buildFallbackQueries(source, base));
            }
        }

        List<String> distinctQueries = queries.stream()
                .map(query -> query == null ? "" : query.trim())
                .filter(query -> !query.isBlank())
                .distinct()
                .toList();
        if (distinctQueries.isEmpty()) {
            log.warn("SearchQueryGenerationService generated no queries for contentId={} title='{}'", source.getId(), source.getTitle());
        }
        return distinctQueries;
    }

    private String normalizeBaseQuery(String title) {
        if (title == null || title.isBlank()) {
            log.warn("Source title is blank while generating search query");
            return "";
        }
        return title.trim();
    }

    private List<String> fetchAiSuggestions(Content source, String normalizedTitle) {
        try {
            List<String> suggestions = searchQuerySuggestionClient.suggestKeywords(
                    normalizedTitle,
                    source.getDescription(),
                    source.getCategory(),
                    source.getFrame(),
                    source.getScope(),
                    source.getTone(),
                    source.getFormat(),
                    source.getPerspectiveLevel(),
                    source.getPerspectiveStakeholder(),
                    MAX_AI_SUGGESTIONS
            );
            if (suggestions == null || suggestions.isEmpty()) {
                log.info("SearchQuerySuggestionClient returned no suggestions for contentId={} title='{}'",
                        source.getId(), source.getTitle());
                return List.of();
            }
            log.info("SearchQuerySuggestionClient suggestions for contentId={} title='{}': {}",
                    source.getId(), source.getTitle(), suggestions);
            return suggestions;
        } catch (Exception e) {
            log.error("AI search query suggestion failed for contentId={} title='{}'", source.getId(), source.getTitle(), e);
            return List.of();
        }
    }

    private List<String> buildFallbackQueries(Content source, String base) {
        if (base.isBlank()) {
            return List.of();
        }

        List<String> queries = new ArrayList<>();
        suggestAlternativeFrames(source).forEach(frame -> queries.add(base + " " + frame));

        toneExplorationKeyword(source.getTone()).ifPresent(keyword -> queries.add(base + " " + keyword));
        scopeExplorationKeyword(source.getScope()).ifPresent(keyword -> queries.add(base + " " + keyword));
        formatExplorationKeyword(source.getFormat()).ifPresent(keyword -> queries.add(base + " " + keyword));

        if (queries.isEmpty()) {
            queries.add(base + " 다른 관점");
        }

        return queries.stream()
                .map(query -> query == null ? "" : query.trim())
                .filter(query -> !query.isBlank())
                .limit(4)
                .toList();
    }

    private List<String> suggestAlternativeFrames(Content source) {
        return ContentCategory.fromLabel(source.getCategory())
                .map(category -> CategoryFrame.labelsFor(category).stream()
                        .filter(frame -> !frame.equals(source.getFrame()))
                        .limit(2)
                        .toList())
                .orElse(List.of("다른 시각", "현장 반응"));
    }

    private Optional<String> toneExplorationKeyword(String currentTone) {
        if (currentTone == null || currentTone.isBlank()) {
            return Optional.of("현장 반응");
        }
        return switch (currentTone) {
            case "경고" -> Optional.of("대응 전략");
            case "비판" -> Optional.of("반론");
            case "낙관" -> Optional.of("위기 요인");
            case "해설" -> Optional.of("인터뷰");
            default -> Optional.of("심층 분석");
        };
    }

    private Optional<String> scopeExplorationKeyword(String scope) {
        if (scope == null || scope.isBlank()) {
            return Optional.empty();
        }
        return switch (scope) {
            case "개인" -> Optional.of("산업 영향");
            case "조직/산업" -> Optional.of("현장 사례");
            case "국가" -> Optional.of("국제 반응");
            case "국제" -> Optional.of("국내 영향");
            case "장기/문명" -> Optional.of("당장 영향");
            default -> Optional.empty();
        };
    }

    private Optional<String> formatExplorationKeyword(String format) {
        if (format == null || format.isBlank()) {
            return Optional.of("토론");
        }
        return switch (format) {
            case "뉴스" -> Optional.of("토론");
            case "인터뷰" -> Optional.of("토론");
            case "토론" -> Optional.of("현장/브이로그");
            case "해설" -> Optional.of("현장/브이로그");
            case "다큐/리포트" -> Optional.of("인터뷰");
            case "현장/브이로그" -> Optional.of("강연/교육");
            case "강연/교육" -> Optional.of("인터뷰");
            default -> Optional.empty();
        };
    }
}
