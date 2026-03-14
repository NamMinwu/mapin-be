package com.mapin.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mapin.content.domain.Content;
import com.mapin.content.domain.ContentRepository;
import com.mapin.content.dto.ContentRecommendationResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentRecommendationOrchestratorTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private InternalRecommendationService internalRecommendationService;

    @Mock
    private FallbackCandidateExpansionService fallbackCandidateExpansionService;

    @InjectMocks
    private ContentRecommendationOrchestrator orchestrator;

    private Content source;

    @BeforeEach
    void setUp() {
        source = Content.builder()
                .canonicalUrl("https://youtube.com/watch?v=source")
                .platform("YOUTUBE")
                .externalContentId("source")
                .title("Source Title")
                .description("Source Description")
                .thumbnailUrl("https://image.test/source.jpg")
                .channelTitle("Source Channel")
                .publishedAt(OffsetDateTime.parse("2024-01-01T00:00:00Z"))
                .youtubeCategoryId("10")
                .duration("PT10M")
                .viewCount(1000L)
                .status("ACTIVE")
                .build();
        source.updateEmbeddingInfo("source text", "model", "content:1");
        source.updatePerspective("경제", "사건", "정부");
    }

    @Test
    void recommend_triggersFallbackWhenInitialResultsInsufficient() {
        when(contentRepository.findById(1L)).thenReturn(Optional.of(source));
        List<ContentRecommendationResponse> fallbackResults = List.of(
                new ContentRecommendationResponse(2L, "url", "title", "thumb", "channel",
                        OffsetDateTime.parse("2024-01-02T00:00:00Z"), "경제", "원인", "전문가", 0.8, 2, 0.9)
        );

        when(internalRecommendationService.recommend(source, 3))
                .thenReturn(List.of())
                .thenReturn(fallbackResults);

        List<ContentRecommendationResponse> responses = orchestrator.recommend(1L, 3);

        verify(fallbackCandidateExpansionService, times(1)).expand(source);
        verify(internalRecommendationService, times(2)).recommend(source, 3);
        assertThat(responses).hasSize(1);
    }
}
