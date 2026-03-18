package com.mapin.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mapin.content.domain.Content;
import com.mapin.content.port.SearchQuerySuggestionClient;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchQueryGenerationServiceTest {

    @Mock
    private SearchQuerySuggestionClient searchQuerySuggestionClient;

    @InjectMocks
    private SearchQueryGenerationService searchQueryGenerationService;

    private Content source;

    @BeforeEach
    void setUp() {
        source = Content.builder()
                .canonicalUrl("https://youtube.com/watch?v=test")
                .platform("YOUTUBE")
                .externalContentId("test")
                .title("경제 위기")
                .description("세계 경제 동향을 요약한 콘텐츠")
                .thumbnailUrl("https://img.test/thumb.jpg")
                .channelTitle("테스트 채널")
                .publishedAt(OffsetDateTime.parse("2024-01-01T00:00:00Z"))
                .youtubeCategoryId("25")
                .duration("PT5M")
                .viewCount(1000L)
                .status("ACTIVE")
                .build();
        source.updatePerspective("경제", "정부정책", "정부");
    }

    @Test
    void generate_includesAiSuggestionsAndFallbacks() {
        when(searchQuerySuggestionClient.suggestKeywords(
                eq("경제 위기"),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(5)))
                .thenReturn(List.of("경제 위기 심층 분석", "국제 금리 변동", "물가 체감 시민 의견"));

        List<String> queries = searchQueryGenerationService.generate(source);

        assertThat(queries)
                .contains("경제 위기", "경제 위기 원인 분석", "경제 위기 전문가 해설")
                .containsSequence("경제 위기", "경제 위기 심층 분석", "국제 금리 변동");
    }

    @Test
    void generate_whenAiSuggestionsEmpty_usesFallbackQueries() {
        when(searchQuerySuggestionClient.suggestKeywords(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyInt()))
                .thenReturn(List.of());

        List<String> queries = searchQueryGenerationService.generate(source);

        assertThat(queries)
                .containsExactly(
                        "경제 위기",
                        "경제 위기 원인 분석",
                        "경제 위기 구조 문제",
                        "경제 위기 전문가 해설",
                        "경제 위기 시민 반응"
                );
    }

    @Test
    void generate_whenTitleBlank_returnsAiSuggestionsOnly() {
        Content untitled = Content.builder()
                .canonicalUrl("https://youtube.com/watch?v=blank")
                .platform("YOUTUBE")
                .externalContentId("blank")
                .title("   ")
                .description("원전 정책의 안전 이슈를 다룹니다")
                .thumbnailUrl("https://img.test/thumb2.jpg")
                .channelTitle("테스트 채널2")
                .publishedAt(OffsetDateTime.parse("2024-02-01T00:00:00Z"))
                .youtubeCategoryId("25")
                .duration("PT6M")
                .viewCount(500L)
                .status("ACTIVE")
                .build();
        untitled.updatePerspective("에너지", "안전", "시민단체");

        when(searchQuerySuggestionClient.suggestKeywords(
                eq(""),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(5)))
                .thenReturn(List.of("원전 안전 진단", "전력 수급 계획"));

        List<String> queries = searchQueryGenerationService.generate(untitled);

        assertThat(queries)
                .containsExactly("원전 안전 진단", "전력 수급 계획");
    }
}
