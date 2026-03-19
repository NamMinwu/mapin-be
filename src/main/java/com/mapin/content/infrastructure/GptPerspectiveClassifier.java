package com.mapin.content.infrastructure;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mapin.content.dto.PerspectiveAnalysisResult;
import com.mapin.content.port.PerspectiveClassifier;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonSchemaLocalValidation;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class GptPerspectiveClassifier implements PerspectiveClassifier {

    private final OpenAIClient openAIClient;

    @Value("${openai.model:gpt-4.1}")
    private String model;

    @Override
    public PerspectiveAnalysisResult classify(String text) {
        StructuredChatCompletionCreateParams<PerspectiveSchema> params =
                StructuredChatCompletionCreateParams.<PerspectiveSchema>builder()
                        .model(toChatModel(model))
                        .addSystemMessage(systemPrompt())
                        .addUserMessage(userPrompt(text))
                        .responseFormat(PerspectiveSchema.class, JsonSchemaLocalValidation.YES)
                        .build();

        StructuredChatCompletion<PerspectiveSchema> completion = openAIClient.chat()
                .completions()
                .create(params);

        PerspectiveSchema schema = completion.choices().stream()
                .findFirst()
                .flatMap(choice -> choice.message().content())
                .orElseThrow(() -> new IllegalStateException("분류 결과가 비어 있습니다."));

        return new PerspectiveAnalysisResult(
                schema.category,
                schema.frame,
                schema.scope,
                schema.tone,
                schema.format,
                schema.perspectiveLevel,
                schema.perspectiveStakeholder
        );
    }

    private String systemPrompt() {
        return """
                너는 유튜브 콘텐츠 관점 분류기다.
                입력된 제목과 설명을 읽고 반드시 지정된 스키마에 맞는 JSON만 반환한다.

                [필드 설명]
                1) category (정치, 경제, 사회, 생활/문화, IT/과학, 세계, 연예, 스포츠)

                2) frame (category별 허용값 중 택1)
                - 정치: 정책, 정당/권력갈등, 선거, 입법/제도, 사법/감사, 여론/정치반응, 정치전략
                - 경제: 시장동향, 산업구조, 기업전략, 소비자영향, 투자/자산, 고용/노동, 정책/금리/환율
                - 사회: 사건사고, 사회갈등, 제도문제, 세대/젠더, 교육, 복지/안전, 시민경험
                - 생활/문화: 라이프스타일, 건강, 여행, 음식, 소비트렌드, 문화해설, 일상정보
                - IT/과학: 기술발전, 산업혁신, 노동시장, 윤리, 규제, 연구성과, 미래전망
                - 세계: 안보/군사, 외교, 국제질서, 경제파급, 에너지/자원, 시민피해/인도주의, 해외정치동향
                - 연예: 작품/콘텐츠, 흥행/성과, 업계동향, 논란/이슈, 팬반응, 인물/커리어, 문화트렌드
                - 스포츠: 경기결과, 전술분석, 선수퍼포먼스, 팀운영, 시즌흐름, 이적시장, 팬반응

                3) scope (개인, 조직/산업, 국가, 국제, 장기/문명)

                4) tone (중립, 경고, 비판, 낙관, 해설)

                5) format (뉴스, 인터뷰, 토론, 해설, 다큐/리포트, 현장/브이로그, 강연/교육)

                6) perspectiveLevel (사건, 원인, 구조)
                - 사건: 지금 무슨 일이 일어났는가 중심
                - 원인: 왜 이런 일이 발생했는가 중심
                - 구조: 더 큰 시스템/구조적 문제 중심

                7) perspectiveStakeholder (정부, 전문가, 시민, 기업, 국제)
                - 정부: 정부/공공기관 발표 및 입장 중심
                - 전문가: 전문가/학자/분석가 해설 중심
                - 시민: 일반 시민 경험/반응/현장 목소리 중심
                - 기업: 산업/기업 관점 중심
                - 국제: 해외/국제 시각 중심

                규칙:
                - 반드시 하나의 값만 선택
                - frame은 선택한 category의 허용값 중에서만 고른다.
                - tone/format은 영상의 분위기와 구성 방식을 기준으로 한다.
                - 설명 문장은 출력하지 말고 구조화된 결과만 반환한다.
                """;
    }

    private String userPrompt(String text) {
        return """
                아래 콘텐츠를 분류해줘.

                %s
                """.formatted(text);
    }

    private ChatModel toChatModel(String modelName) {
        return switch (modelName) {
            case "gpt-4.1" -> ChatModel.GPT_4_1;
            case "gpt-4.1-mini" -> ChatModel.GPT_4_1_MINI;
            case "gpt-4.1-nano" -> ChatModel.GPT_4_1_NANO;
            default -> throw new IllegalArgumentException("지원하지 않는 OpenAI 모델입니다: " + modelName);
        };
    }

    @JsonClassDescription("유튜브 콘텐츠 관점 분류 결과")
    static class PerspectiveSchema {

        @JsonPropertyDescription("정치, 경제, 사회, 생활/문화, IT/과학, 세계, 연예, 스포츠 중 하나")
        public String category;

        @JsonPropertyDescription("category별 허용 frame 목록 중 하나")
        public String frame;

        @JsonPropertyDescription("개인, 조직/산업, 국가, 국제, 장기/문명 중 하나")
        public String scope;

        @JsonPropertyDescription("중립, 경고, 비판, 낙관, 해설 중 하나")
        public String tone;

        @JsonPropertyDescription("뉴스, 인터뷰, 토론, 해설, 다큐/리포트, 현장/브이로그, 강연/교육 중 하나")
        public String format;

        @JsonPropertyDescription("사건, 원인, 구조 중 하나")
        public String perspectiveLevel;

        @JsonPropertyDescription("정부, 전문가, 시민, 기업, 국제 중 하나")
        public String perspectiveStakeholder;
    }
}
