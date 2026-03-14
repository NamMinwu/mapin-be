package com.mapin.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mapin.content.domain.Content;
import com.mapin.content.domain.ContentRepository;
import com.mapin.content.dto.ContentResponse;
import com.mapin.content.port.YoutubeSearchClient;
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
class FallbackCandidateExpansionServiceTest {

    @Mock
    private SearchQueryGenerationService searchQueryGenerationService;

    @Mock
    private YoutubeSearchClient youtubeSearchClient;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentIngestService contentIngestService;

    @Mock
    private ContentEmbeddingService contentEmbeddingService;

    @Mock
    private ContentPerspectiveAnalysisService contentPerspectiveAnalysisService;

    @InjectMocks
    private FallbackCandidateExpansionService fallbackCandidateExpansionService;

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
                .viewCount(1000L)
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
                .viewCount(5000L)
                .status("ACTIVE")
                .build();
    }

    @Test
    void expand_ingestsEmbedsAndAnalyzesNewCandidates() {
        when(searchQueryGenerationService.generate(source)).thenReturn(List.of("query1"));
        when(youtubeSearchClient.searchVideoIds("query1", 5)).thenReturn(List.of("candidate", "source"));
        when(contentIngestService.ingestYoutubeUrl("https://www.youtube.com/watch?v=candidate"))
                .thenReturn(new ContentResponse(2L, candidate.getCanonicalUrl(), candidate.getPlatform(),
                        candidate.getExternalContentId(), candidate.getTitle(), candidate.getDescription(),
                        candidate.getThumbnailUrl(), candidate.getChannelTitle(), candidate.getPublishedAt(),
                        candidate.getYoutubeCategoryId(), candidate.getDuration(), candidate.getViewCount(), candidate.getStatus()));
        when(contentRepository.findById(2L)).thenReturn(Optional.of(candidate));

        fallbackCandidateExpansionService.expand(source);

        verify(contentIngestService, times(1)).ingestYoutubeUrl("https://www.youtube.com/watch?v=candidate");
        verify(contentEmbeddingService, times(1)).embed(2L);
        verify(contentPerspectiveAnalysisService, times(1)).analyze(2L);
    }
}
