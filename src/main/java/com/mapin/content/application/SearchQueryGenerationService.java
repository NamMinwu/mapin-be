package com.mapin.content.application;

import com.mapin.content.domain.Content;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchQueryGenerationService {

    public List<String> generate(Content source) {
        String base = normalizeBaseQuery(source.getTitle());

        List<String> queries = new ArrayList<>();
        queries.add(base);
        queries.add(base + " 원인 분석");
        queries.add(base + " 구조 문제");
        queries.add(base + " 전문가 해설");
        queries.add(base + " 시민 반응");

        return queries.stream().distinct().toList();
    }

    private String normalizeBaseQuery(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        return title.trim();
    }
}
