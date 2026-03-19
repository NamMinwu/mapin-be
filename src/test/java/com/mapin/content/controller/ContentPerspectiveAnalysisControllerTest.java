package com.mapin.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mapin.content.domain.Content;
import com.mapin.content.dto.ContentPerspectiveResponse;
import com.mapin.content.domain.ContentRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class ContentPerspectiveAnalysisControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @AfterEach
    void tearDown() {
        contentRepository.deleteAll();
    }

    @Test
    void analyzePerspective_classifiesAndSavesPerspectiveFields() throws Exception {
        Content content = contentRepository.save(Content.builder()
                .canonicalUrl("https://www.youtube.com/watch?v=perspective")
                .platform("YOUTUBE")
                .externalContentId("perspective")
                .title("한국 경제 전망")
                .description("정부가 발표한 경제 대책을 분석합니다.")
                .thumbnailUrl("https://image.test/perspective.jpg")
                .channelTitle("경제 방송")
                .publishedAt(OffsetDateTime.parse("2024-01-10T00:00:00Z"))
                .youtubeCategoryId("25")
                .duration("PT12M")
                .viewCount(5000L)
                .status("ACTIVE")
                .build());

        MvcResult result = mockMvc.perform(post("/api/contents/{contentId}/analyze-perspective", content.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("경제"))
                .andExpect(jsonPath("$.frame").value("시장동향"))
                .andExpect(jsonPath("$.scope").value("국가"))
                .andExpect(jsonPath("$.tone").value("해설"))
                .andExpect(jsonPath("$.format").value("뉴스"))
                .andExpect(jsonPath("$.perspectiveLevel").value("사건"))
                .andExpect(jsonPath("$.perspectiveStakeholder").value("정부"))
                .andReturn();

        ContentPerspectiveResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                ContentPerspectiveResponse.class
        );
        assertThat(response.contentId()).isEqualTo(content.getId());

        Content updated = contentRepository.findById(content.getId()).orElseThrow();
        assertThat(updated.getCategory()).isEqualTo("경제");
        assertThat(updated.getFrame()).isEqualTo("시장동향");
        assertThat(updated.getScope()).isEqualTo("국가");
        assertThat(updated.getTone()).isEqualTo("해설");
        assertThat(updated.getFormat()).isEqualTo("뉴스");
        assertThat(updated.getPerspectiveLevel()).isEqualTo("사건");
        assertThat(updated.getPerspectiveStakeholder()).isEqualTo("정부");
    }
}
