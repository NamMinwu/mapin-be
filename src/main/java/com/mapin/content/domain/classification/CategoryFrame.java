package com.mapin.content.domain.classification;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum CategoryFrame {
    POLITICS_POLICY(ContentCategory.POLITICS, "정책"),
    POLITICS_POWER(ContentCategory.POLITICS, "정당/권력갈등"),
    POLITICS_ELECTION(ContentCategory.POLITICS, "선거"),
    POLITICS_LEGISLATION(ContentCategory.POLITICS, "입법/제도"),
    POLITICS_JUDICIARY(ContentCategory.POLITICS, "사법/감사"),
    POLITICS_OPINION(ContentCategory.POLITICS, "여론/정치반응"),
    POLITICS_STRATEGY(ContentCategory.POLITICS, "정치전략"),

    ECONOMY_MARKET(ContentCategory.ECONOMY, "시장동향"),
    ECONOMY_STRUCTURE(ContentCategory.ECONOMY, "산업구조"),
    ECONOMY_CORPORATE(ContentCategory.ECONOMY, "기업전략"),
    ECONOMY_CONSUMER(ContentCategory.ECONOMY, "소비자영향"),
    ECONOMY_INVESTMENT(ContentCategory.ECONOMY, "투자/자산"),
    ECONOMY_LABOR(ContentCategory.ECONOMY, "고용/노동"),
    ECONOMY_POLICY(ContentCategory.ECONOMY, "정책/금리/환율"),

    SOCIETY_INCIDENT(ContentCategory.SOCIETY, "사건사고"),
    SOCIETY_CONFLICT(ContentCategory.SOCIETY, "사회갈등"),
    SOCIETY_INSTITUTION(ContentCategory.SOCIETY, "제도문제"),
    SOCIETY_GENERATION(ContentCategory.SOCIETY, "세대/젠더"),
    SOCIETY_EDUCATION(ContentCategory.SOCIETY, "교육"),
    SOCIETY_WELFARE(ContentCategory.SOCIETY, "복지/안전"),
    SOCIETY_CITIZEN(ContentCategory.SOCIETY, "시민경험"),

    LIFESTYLE_STYLE(ContentCategory.LIFESTYLE_CULTURE, "라이프스타일"),
    LIFESTYLE_HEALTH(ContentCategory.LIFESTYLE_CULTURE, "건강"),
    LIFESTYLE_TRAVEL(ContentCategory.LIFESTYLE_CULTURE, "여행"),
    LIFESTYLE_FOOD(ContentCategory.LIFESTYLE_CULTURE, "음식"),
    LIFESTYLE_TREND(ContentCategory.LIFESTYLE_CULTURE, "소비트렌드"),
    LIFESTYLE_CULTURE_EXPLAIN(ContentCategory.LIFESTYLE_CULTURE, "문화해설"),
    LIFESTYLE_DAILY_INFO(ContentCategory.LIFESTYLE_CULTURE, "일상정보"),

    IT_TECH(ContentCategory.IT_SCIENCE, "기술발전"),
    IT_INNOVATION(ContentCategory.IT_SCIENCE, "산업혁신"),
    IT_LABOR(ContentCategory.IT_SCIENCE, "노동시장"),
    IT_ETHICS(ContentCategory.IT_SCIENCE, "윤리"),
    IT_REGULATION(ContentCategory.IT_SCIENCE, "규제"),
    IT_RESEARCH(ContentCategory.IT_SCIENCE, "연구성과"),
    IT_FUTURE(ContentCategory.IT_SCIENCE, "미래전망"),

    WORLD_SECURITY(ContentCategory.WORLD, "안보/군사"),
    WORLD_DIPLOMACY(ContentCategory.WORLD, "외교"),
    WORLD_ORDER(ContentCategory.WORLD, "국제질서"),
    WORLD_ECONOMIC_IMPACT(ContentCategory.WORLD, "경제파급"),
    WORLD_ENERGY(ContentCategory.WORLD, "에너지/자원"),
    WORLD_HUMANITARIAN(ContentCategory.WORLD, "시민피해/인도주의"),
    WORLD_FOREIGN_POLITICS(ContentCategory.WORLD, "해외정치동향"),

    ENTERTAINMENT_WORK(ContentCategory.ENTERTAINMENT, "작품/콘텐츠"),
    ENTERTAINMENT_BOX_OFFICE(ContentCategory.ENTERTAINMENT, "흥행/성과"),
    ENTERTAINMENT_INDUSTRY(ContentCategory.ENTERTAINMENT, "업계동향"),
    ENTERTAINMENT_CONTROVERSY(ContentCategory.ENTERTAINMENT, "논란/이슈"),
    ENTERTAINMENT_FANDOM(ContentCategory.ENTERTAINMENT, "팬반응"),
    ENTERTAINMENT_PERSON(ContentCategory.ENTERTAINMENT, "인물/커리어"),
    ENTERTAINMENT_TREND(ContentCategory.ENTERTAINMENT, "문화트렌드"),

    SPORTS_RESULT(ContentCategory.SPORTS, "경기결과"),
    SPORTS_TACTICS(ContentCategory.SPORTS, "전술분석"),
    SPORTS_PERFORMANCE(ContentCategory.SPORTS, "선수퍼포먼스"),
    SPORTS_TEAM(ContentCategory.SPORTS, "팀운영"),
    SPORTS_SEASON(ContentCategory.SPORTS, "시즌흐름"),
    SPORTS_TRANSFER(ContentCategory.SPORTS, "이적시장"),
    SPORTS_FANDOM(ContentCategory.SPORTS, "팬반응");

    private static final Map<ContentCategory, List<CategoryFrame>> FRAMES_BY_CATEGORY = Arrays.stream(values())
            .collect(Collectors.groupingBy(CategoryFrame::category, () -> new EnumMap<>(ContentCategory.class), Collectors.toList()));

    private final ContentCategory category;
    private final String label;

    CategoryFrame(ContentCategory category, String label) {
        this.category = category;
        this.label = label;
    }

    public ContentCategory category() {
        return category;
    }

    public String label() {
        return label;
    }

    public static List<String> labelsFor(ContentCategory category) {
        return FRAMES_BY_CATEGORY.getOrDefault(category, List.of()).stream()
                .map(CategoryFrame::label)
                .toList();
    }

    public static boolean isValidFrame(ContentCategory category, String label) {
        if (category == null || label == null) {
            return false;
        }
        return FRAMES_BY_CATEGORY.getOrDefault(category, List.of()).stream()
                .anyMatch(frame -> frame.label.equals(label.strip()));
    }
}
