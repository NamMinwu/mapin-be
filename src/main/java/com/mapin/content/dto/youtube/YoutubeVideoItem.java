package com.mapin.content.dto.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeVideoItem(
        String id,
        YoutubeSnippet snippet,
        YoutubeContentDetails contentDetails,
        YoutubeStatistics statistics
) {
}
