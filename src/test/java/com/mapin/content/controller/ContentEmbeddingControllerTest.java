package com.mapin.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mapin.content.domain.Content;
import com.mapin.content.dto.ContentEmbeddingResponse;
import com.mapin.content.port.EmbeddingClient;
import com.mapin.content.port.VectorStoreClient;
import com.mapin.content.domain.ContentRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
//@ActiveProfiles("test")
class ContentEmbeddingControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmbeddingClient embeddingClient;

    @MockitoBean
    private VectorStoreClient vectorStoreClient;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @AfterEach
    void tearDown() {
        contentRepository.deleteAll();
    }

    @Test
    void embed_withContentId_executesFullEmbeddingWorkflow() throws Exception {
        Content content = contentRepository.save(Content.builder()
                .canonicalUrl("https://www.youtube.com/watch?v=abc123")
                .platform("YOUTUBE")
                .externalContentId("abc123")
                .title("테스트 영상 제목")
                .description("테스트 설명")
                .thumbnailUrl("https://image.test/thumb.jpg")
                .channelTitle("테스트 채널")
                .publishedAt(OffsetDateTime.parse("2024-01-01T00:00:00Z"))
                .youtubeCategoryId("42")
                .duration("PT10M")
                .viewCount(1000L)
                .status("ACTIVE")
                .build());

        String expectedEmbeddingText = "테스트 영상 제목\n테스트 설명";
        List<Float> vector = List.of(0.1f, -0.2f, 0.3f);
        when(embeddingClient.embed(expectedEmbeddingText)).thenReturn(vector);
        when(embeddingClient.modelName()).thenReturn("test-model");

        MvcResult result = mockMvc.perform(post("/api/contents/{contentId}/embed", content.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value(content.getId()))
                .andExpect(jsonPath("$.embeddingModel").value("test-model"))
                .andExpect(jsonPath("$.vectorId").value("content:" + content.getId()))
                .andExpect(jsonPath("$.embeddingText").value(expectedEmbeddingText))
                .andReturn();

        ContentEmbeddingResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                ContentEmbeddingResponse.class
        );
        assertThat(response.contentId()).isEqualTo(content.getId());
        assertThat(response.embeddingText()).isEqualTo(expectedEmbeddingText);
        assertThat(response.embeddingModel()).isEqualTo("test-model");
        assertThat(response.vectorId()).isEqualTo("content:" + content.getId());

        Content updated = contentRepository.findById(content.getId()).orElseThrow();
        assertThat(updated.getEmbeddingText()).isEqualTo(expectedEmbeddingText);
        assertThat(updated.getEmbeddingModel()).isEqualTo("test-model");
        assertThat(updated.getVectorId()).isEqualTo("content:" + content.getId());

        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(vectorStoreClient, times(1))
                .upsert(eq("content:" + content.getId()), eq(vector), metadataCaptor.capture());
        Map<String, Object> metadata = metadataCaptor.getValue();
        assertThat(metadata)
                .containsEntry("contentId", content.getId())
                .containsEntry("platform", "YOUTUBE")
                .containsEntry("status", "ACTIVE")
                .containsEntry("externalContentId", "abc123");
        verify(embeddingClient, times(1)).embed(expectedEmbeddingText);
        verify(embeddingClient, times(1)).modelName();
    }
}
