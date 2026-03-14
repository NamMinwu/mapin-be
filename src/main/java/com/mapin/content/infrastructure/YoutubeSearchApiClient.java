package com.mapin.content.infrastructure;

import com.mapin.content.dto.youtube.YoutubeSearchResponse;
import com.mapin.content.port.YoutubeSearchClient;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("!test")
public class YoutubeSearchApiClient implements YoutubeSearchClient {

    private final RestClient restClient;

    @Value("${youtube.api.key}")
    private String apiKey;

    public YoutubeSearchApiClient() {
        this(RestClient.create("https://www.googleapis.com/youtube/v3"));
    }

    YoutubeSearchApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public List<String> searchVideoIds(String query, int maxResults) {
        YoutubeSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", query)
                        .queryParam("type", "video")
                        .queryParam("maxResults", maxResults)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(YoutubeSearchResponse.class);

        if (response == null || response.items() == null) {
            return List.of();
        }

        return response.items().stream()
                .map(item -> item.id() != null ? item.id().videoId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
