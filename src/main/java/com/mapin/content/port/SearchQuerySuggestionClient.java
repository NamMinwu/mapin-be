package com.mapin.content.port;

import java.util.List;

public interface SearchQuerySuggestionClient {

    List<String> suggestKeywords(
            String title,
            String description,
            String category,
            String perspectiveLevel,
            String perspectiveStakeholder,
            int limit
    );
}
