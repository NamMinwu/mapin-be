package com.mapin.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mapin.content.domain.Content;
import com.mapin.content.domain.ContentRepository;
import com.mapin.content.dto.ContentRecommendationResponse;
import com.mapin.content.dto.RecommendationCandidate;
import com.mapin.content.port.VectorSearchResult;
import com.mapin.content.port.VectorStoreClient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentRecommendationServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private VectorStoreClient vectorStoreClient;

    @Mock
    private CandidateValidationService candidateValidationService;

    @InjectMocks
    private ContentRecommendationService contentRecommendationService;

    private Content source;
    private Content candidate;

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
                .viewCount(10000L)
                .status("ACTIVE")
                .build();
        source.updateEmbeddingInfo("source text", "model", "content:1");
        source.updatePerspective("경제", "사건", "정부");

        candidate = Content.builder()
                .canonicalUrl("https://youtube.com/watch?v=candidate")
                .platform("YOUTUBE")
                .externalContentId("candidate")
                .title("Candidate Title")
                .description("Candidate Description")
                .thumbnailUrl("https://image.test/candidate.jpg")
                .channelTitle("Candidate Channel")
                .publishedAt(OffsetDateTime.parse("2024-01-02T00:00:00Z"))
                .youtubeCategoryId("10")
                .duration("PT8M")
                .viewCount(50000L)
                .status("ACTIVE")
                .build();
        candidate.updateEmbeddingInfo("candidate text", "model", "content:2");
        candidate.updatePerspective("경제", "원인", "전문가");
    }

    @Test
    void recommend_returnsQualifiedCandidatesSortedByFinalScore() {
        when(contentRepository.findById(anyLong())).thenReturn(Optional.of(source));
        when(vectorStoreClient.searchById("content:1", 8)).thenReturn(List.of(
                new VectorSearchResult("content:1", 1.0, Map.of()),
                new VectorSearchResult("content:2", 0.85, Map.of())
        ));
        when(contentRepository.findAllByVectorIdIn(anyList())).thenReturn(List.of(candidate));

        RecommendationCandidate validated = new RecommendationCandidate(
                candidate,
                0.85,
                2,
                1.0,
                0.8,
                0.91,
                true
        );
        when(candidateValidationService.validate(source, candidate, 0.85)).thenReturn(validated);

        List<ContentRecommendationResponse> responses = contentRecommendationService.recommend(1L, 3);

        assertThat(responses).hasSize(1);
        ContentRecommendationResponse response = responses.get(0);
        assertThat(response.contentId()).isEqualTo(candidate.getId());
        assertThat(response.topicSimilarity()).isEqualTo(0.85);
        assertThat(response.perspectiveDistance()).isEqualTo(2);
        assertThat(response.finalScore()).isEqualTo(0.91);
        assertThat(response.perspectiveLevel()).isEqualTo("원인");
        assertThat(response.perspectiveStakeholder()).isEqualTo("전문가");

        verify(contentRepository, times(1)).findById(1L);
        verify(vectorStoreClient, times(1)).searchById("content:1", 8);
        verify(candidateValidationService, times(1)).validate(source, candidate, 0.85);
    }
}
