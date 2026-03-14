package com.mapin.content.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mapin.content.dto.youtube.YoutubeSearchId;
import com.mapin.content.dto.youtube.YoutubeSearchItem;
import com.mapin.content.dto.youtube.YoutubeSearchResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class YoutubeSearchApiClientTest {

    private RestClient restClient;
    private YoutubeSearchApiClient client;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        client = new YoutubeSearchApiClient(restClient);
        client.setApiKey("test-key");
    }

    @Test
    void searchVideoIds_returnsDistinctVideoIds() {
        YoutubeSearchResponse response = new YoutubeSearchResponse(
                List.of(
                        new YoutubeSearchItem(new YoutubeSearchId("youtube#video", "abc123")),
                        new YoutubeSearchItem(new YoutubeSearchId("youtube#video", "def456")),
                        new YoutubeSearchItem(new YoutubeSearchId("youtube#video", "abc123"))
                )
        );

        when(restClient.get()
                .uri(any())
                .retrieve()
                .body(YoutubeSearchResponse.class))
                .thenReturn(response);

        List<String> videoIds = client.searchVideoIds("query", 5);

        assertThat(videoIds).containsExactly("abc123", "def456");
    }
}
