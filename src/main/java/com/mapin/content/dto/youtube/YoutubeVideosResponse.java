package com.mapin.content.dto.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeVideosResponse(
        List<YoutubeVideoItem> items
) {
}
