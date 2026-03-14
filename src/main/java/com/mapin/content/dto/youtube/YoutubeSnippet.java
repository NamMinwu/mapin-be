package com.mapin.content.dto.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeSnippet(
        String title,
        String description,
        String channelTitle,
        String publishedAt,
        String categoryId,
        YoutubeThumbnails thumbnails
) {
}
