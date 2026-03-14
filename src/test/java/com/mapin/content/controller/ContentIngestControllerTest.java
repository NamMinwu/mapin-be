package com.mapin.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mapin.content.domain.Content;
import com.mapin.content.dto.ContentResponse;
import com.mapin.content.dto.YoutubeVideoMetadata;
import com.mapin.content.domain.ContentRepository;
import com.mapin.content.infrastructure.YoutubeMetadataClient;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class ContentIngestControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private YoutubeMetadataClient youtubeMetadataClient;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @AfterEach
    void tearDown() {
        contentRepository.deleteAll();
    }

    @Test
    void ingestNewYoutubeUrl_persistsContentAndReturnsResponse() throws Exception {
        YoutubeVideoMetadata metadata = sampleMetadata("abc123");
        when(youtubeMetadataClient.fetchVideoMetadata("abc123")).thenReturn(metadata);

        MvcResult result = mockMvc.perform(post("/api/contents/ingest/youtube")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://www.youtube.com/watch?v=abc123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(metadata.title()))
                .andExpect(jsonPath("$.description").value(metadata.description()))
                .andExpect(jsonPath("$.thumbnailUrl").value(metadata.thumbnailUrl()))
                .andExpect(jsonPath("$.channelTitle").value(metadata.channelTitle()))
                .andExpect(jsonPath("$.publishedAt").value(metadata.publishedAt().toInstant().toString()))
                .andExpect(jsonPath("$.youtubeCategoryId").value(metadata.categoryId()))
                .andExpect(jsonPath("$.duration").value(metadata.duration()))
                .andExpect(jsonPath("$.viewCount").value(metadata.viewCount()))
                .andReturn();

        ContentResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                ContentResponse.class
        );

        Content saved = contentRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo(metadata.title());
        assertThat(saved.getDescription()).isEqualTo(metadata.description());
        assertThat(saved.getThumbnailUrl()).isEqualTo(metadata.thumbnailUrl());
        assertThat(saved.getChannelTitle()).isEqualTo(metadata.channelTitle());
        assertThat(saved.getPublishedAt()).isEqualTo(metadata.publishedAt());
        assertThat(saved.getYoutubeCategoryId()).isEqualTo(metadata.categoryId());
        assertThat(saved.getDuration()).isEqualTo(metadata.duration());
        assertThat(saved.getViewCount()).isEqualTo(metadata.viewCount());
        assertThat(contentRepository.count()).isEqualTo(1);
    }

    @Test
    void ingestSameYoutubeUrlTwice_returnsExistingContentWithoutCreatingDuplicate() throws Exception {
        YoutubeVideoMetadata metadata = sampleMetadata("abc123");
        when(youtubeMetadataClient.fetchVideoMetadata("abc123")).thenReturn(metadata);

        ContentResponse first = performIngest("https://www.youtube.com/watch?v=abc123");
        ContentResponse second = performIngest("https://www.youtube.com/watch?v=abc123");

        assertThat(first.id()).isEqualTo(second.id());
        assertThat(contentRepository.count()).isEqualTo(1);
        verify(youtubeMetadataClient, times(1)).fetchVideoMetadata("abc123");
    }

    private ContentResponse performIngest(String url) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/contents/ingest/youtube")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + url + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                ContentResponse.class
        );
    }

    private YoutubeVideoMetadata sampleMetadata(String videoId) {
        return new YoutubeVideoMetadata(
                videoId,
                "Sample Title",
                "Sample Description",
                "https://image.test/sample.jpg",
                "Sample Channel",
                OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                "42",
                "PT15M",
                10_000L
        );
    }
}
