package com.mapin.content.dto.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeSearchId(
        String kind,
        String videoId
) {
}
