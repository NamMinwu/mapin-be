package com.mapin.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import com.mapin.content.domain.Content;
import com.mapin.content.dto.SimilarContentResponse;
import com.mapin.content.port.VectorSearchResult;
import com.mapin.content.port.VectorStoreClient;
import com.mapin.content.domain.ContentRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class SimilarContentSearchControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VectorStoreClient vectorStoreClient;

    private Content source;
    private Content candidate;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();

        source = contentRepository.save(Content.builder()
                .canonicalUrl("https://www.youtube.com/watch?v=source")
                .platform("YOUTUBE")
                .externalContentId("source")
                .title("Source Title")
                .description("Source Description")
                .thumbnailUrl("https://image.test/source.jpg")
                .channelTitle("Source Channel")
                .publishedAt(OffsetDateTime.parse("2024-01-01T00:00:00Z"))
                .youtubeCategoryId("10")
                .duration("PT5M")
                .viewCount(1_000L)
                .status("ACTIVE")
                .build());
        source.updateEmbeddingInfo("source text", "test-model", "content:" + source.getId());
        contentRepository.save(source);

        candidate = contentRepository.save(Content.builder()
                .canonicalUrl("https://www.youtube.com/watch?v=candidate")
                .platform("YOUTUBE")
                .externalContentId("candidate")
                .title("Candidate Title")
                .description("Candidate Description")
                .thumbnailUrl("https://image.test/candidate.jpg")
                .channelTitle("Candidate Channel")
                .publishedAt(OffsetDateTime.parse("2024-01-05T00:00:00Z"))
                .youtubeCategoryId("11")
                .duration("PT7M")
                .viewCount(2_000L)
                .status("ACTIVE")
                .build());
        candidate.updateEmbeddingInfo("candidate text", "test-model", "content:" + candidate.getId());
        contentRepository.save(candidate);
    }

    @AfterEach
    void tearDown() {
        contentRepository.deleteAll();
    }

    @Test
    void searchSimilar_returnsOrderedResultsWithSimilarityScores() throws Exception {
        when(vectorStoreClient.searchById(eq(source.getVectorId()), anyInt()))
                .thenAnswer(invocation -> List.of(
                        new VectorSearchResult(source.getVectorId(), 1.0, Map.of("contentId", source.getId())),
                        new VectorSearchResult(candidate.getVectorId(), 0.85, Map.of("contentId", candidate.getId())),
                        new VectorSearchResult("missing", 0.5, Map.of())
                ));

        MvcResult result = mockMvc.perform(get("/api/contents/{contentId}/similar", source.getId())
                        .param("topK", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contentId").value(candidate.getId()))
                .andExpect(jsonPath("$[0].similarityScore").value(0.85))
                .andReturn();

        List<SimilarContentResponse> responses = objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<List<SimilarContentResponse>>() {}
        );

        assertThat(responses).hasSize(1);
        SimilarContentResponse response = responses.get(0);
        assertThat(response.contentId()).isEqualTo(candidate.getId());
        assertThat(response.vectorId()).isEqualTo(candidate.getVectorId());
        assertThat(response.canonicalUrl()).isEqualTo(candidate.getCanonicalUrl());
        assertThat(response.similarityScore()).isEqualTo(0.85);
        Mockito.verify(vectorStoreClient)
                .searchById(source.getVectorId(), 3);
    }
}
