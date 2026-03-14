package com.mapin.content.dto.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeThumbnails(
        @JsonProperty("default") YoutubeThumbnail defaultValue,
        YoutubeThumbnail medium,
        YoutubeThumbnail high
) {
}
