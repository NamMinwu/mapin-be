package com.mapin.content.infrastructure;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mapin.content.port.SearchQuerySuggestionClient;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonSchemaLocalValidation;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class GptSearchQuerySuggestionClient implements SearchQuerySuggestionClient {

    private static final int DESCRIPTION_MAX_LENGTH = 500;

    private final OpenAIClient openAIClient;

    @Value("${openai.search-query-model:gpt-4.1-mini}")
    private String model;

    @Override
    public List<String> suggestKeywords(
            String title,
            String description,
            String category,
            String frame,
            String scope,
            String tone,
            String format,
            String perspectiveLevel,
            String perspectiveStakeholder,
            int limit
    ) {
        if (limit <= 0) {
            return List.of();
        }

        try {
            StructuredChatCompletionCreateParams<SearchQuerySchema> params = StructuredChatCompletionCreateParams
                    .<SearchQuerySchema>builder()
                    .model(toChatModel(model))
                    .addSystemMessage(systemPrompt())
                    .addUserMessage(userPrompt(
                            title,
                            description,
                            category,
                            frame,
                            scope,
                            tone,
                            format,
                            perspectiveLevel,
                            perspectiveStakeholder,
                            limit))
                    .responseFormat(SearchQuerySchema.class, JsonSchemaLocalValidation.YES)
                    .build();

            StructuredChatCompletion<SearchQuerySchema> completion = openAIClient.chat()
                    .completions()
                    .create(params);

            Optional<List<String>> maybeKeywords = completion.choices().stream()
                    .findFirst()
                    .flatMap(choice -> choice.message().content())
                    .map(schema -> schema.keywords);

            return maybeKeywords.orElse(Collections.emptyList()).stream()
                    .map(keyword -> keyword == null ? "" : keyword.trim())
                    .filter(keyword -> !keyword.isBlank())
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to generate search keyword suggestions from GPT", e);
            return List.of();
        }
    }

    private String systemPrompt() {
        return """
                너는 유튜브 검색 쿼리를 추천하는 도우미다.
                주어진 영상 제목과 설명을 읽고, 다양한 관점에서 검색 결과를 얻을 수 있는 한국어 키워드만 반환한다.
                제시된 스키마를 반드시 따르고 불필요한 문장은 제거한다.
                키워드는 5~25자 범위에서 작성하고 구체적인 명사를 포함하며, 서로 겹치지 않도록 한다.
                동일 카테고리를 유지하되, frame/scope/tone/format이 바뀌도록 검색 의도를 확장한다.
                """;
    }

    private String userPrompt(
            String title,
            String description,
            String category,
            String frame,
            String scope,
            String tone,
            String format,
            String perspectiveLevel,
            String perspectiveStakeholder,
            int limit
    ) {
        String safeTitle = title == null ? "" : title.strip();
        String safeDescription = truncate(description == null ? "" : description.strip(), DESCRIPTION_MAX_LENGTH);
        String safeCategory = category == null || category.isBlank() ? "미정" : category.strip();
        String safeFrame = frame == null || frame.isBlank() ? "미정" : frame.strip();
        String safeScope = scope == null || scope.isBlank() ? "미정" : scope.strip();
        String safeTone = tone == null || tone.isBlank() ? "미정" : tone.strip();
        String safeFormat = format == null || format.isBlank() ? "미정" : format.strip();
        String safePerspectiveLevel = perspectiveLevel == null || perspectiveLevel.isBlank() ? "미정" : perspectiveLevel.strip();
        String safePerspectiveStakeholder = perspectiveStakeholder == null || perspectiveStakeholder.isBlank() ? "미정" : perspectiveStakeholder.strip();

        return """
                다음 영상을 유튜브에서 검색할 때 사용할 수 있는 서로 다른 키워드 %d개를 추천해줘.
                - 키워드는 짧고 명확해야 한다.
                - 해시태그나 번호는 붙이지 않는다.
                - 카테고리는 유지한다.
                - frame/scope/tone/format을 다양화하되 scope는 지나치게 멀어지지 않는다.
                - 동일한 단어를 반복하지 말고, 현재 관점과 반대되거나 보완적인 탐색 의도를 만들어라.
                - JSON 스키마에 맞는 데이터만 생성한다.

                제목: %s
                설명: %s
                카테고리(변경 금지): %s
                현재 frame: %s
                현재 scope: %s
                현재 tone: %s
                현재 format: %s
                관점 레벨: %s
                관점 이해당사자: %s
                """.formatted(limit, safeTitle, safeDescription, safeCategory, safeFrame, safeScope, safeTone, safeFormat, safePerspectiveLevel, safePerspectiveStakeholder);
    }

    private ChatModel toChatModel(String modelName) {
        return switch (modelName) {
            case "gpt-4.1" -> ChatModel.GPT_4_1;
            case "gpt-4.1-mini" -> ChatModel.GPT_4_1_MINI;
            case "gpt-4.1-nano" -> ChatModel.GPT_4_1_NANO;
            default -> throw new IllegalArgumentException("지원하지 않는 OpenAI 모델입니다: " + modelName);
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @JsonClassDescription("유튜브 검색 키워드 추천 결과")
    static class SearchQuerySchema {

        @JsonPropertyDescription("중복되지 않는 한국어 키워드 목록")
        public List<String> keywords;
    }
}
