package com.mapin.content.infrastructure;

import com.mapin.content.port.SearchQuerySuggestionClient;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class MockSearchQuerySuggestionClient implements SearchQuerySuggestionClient {

    @Override
    public List<String> suggestKeywords(
            String title,
            String description,
            String category,
            String frame,
            String scope,
            String tone,
            String format,
            String perspectiveLevel,
            String perspectiveStakeholder,
            int limit
    ) {
        return List.of("테스트 이슈 분석", "테스트 대응 전략", "테스트 인터뷰");
    }
}
