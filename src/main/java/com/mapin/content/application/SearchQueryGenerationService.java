package com.mapin.content.application;

import com.mapin.content.domain.Content;
import com.mapin.content.port.SearchQuerySuggestionClient;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchQueryGenerationService {

    private static final int MAX_AI_SUGGESTIONS = 5;
    private static final List<String> FALLBACK_SUFFIXES = List.of(
            " 원인 분석",
            " 구조 문제",
            " 전문가 해설",
            " 시민 반응"
    );

    private final SearchQuerySuggestionClient searchQuerySuggestionClient;

    public List<String> generate(Content source) {
        String base = normalizeBaseQuery(source.getTitle());

        List<String> queries = new ArrayList<>();
        queries.add(base);

        List<String> aiSuggestions = fetchAiSuggestions(source, base);
        if (aiSuggestions.isEmpty()) {
            queries.addAll(buildFallbackQueries(base));
        } else {
            queries.addAll(aiSuggestions);
            if (queries.size() < MAX_AI_SUGGESTIONS) {
                queries.addAll(buildFallbackQueries(base));
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

    private List<String> buildFallbackQueries(String base) {
        if (base.isBlank()) {
            return List.of();
        }
        return FALLBACK_SUFFIXES.stream()
                .map(base::concat)
                .toList();
    }
}
