package com.gametrend.agent.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.infrastructure.llm.LlmClient;
import com.gametrend.agent.onboarding.dto.AgentPlan;
import com.gametrend.agent.onboarding.dto.AgentPlanningContext;
import com.gametrend.agent.onboarding.dto.AgentQueryConditionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
public class AgentPlanner {

    private static final String SYSTEM_PROMPT = """
            당신은 GEMMA4 E2B 기반 자연 대화형 게임 분석 Agent의 Planner입니다.
            사용자의 최신 질문을 가장 우선해서 의도와 필요한 도구를 결정합니다.
            이전 대화는 "그거", "아까 말한 후보", "네가 알려준 게임" 같은 참조를 해소할 때만 보조로 사용합니다.
            반드시 JSON 객체 하나만 반환하세요. 설명 문장, 마크다운, 코드블록은 출력하지 마세요.
            """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final AgentQueryConditionResolver fallbackResolver;

    public AgentPlanner(
            LlmClient llmClient,
            ObjectMapper objectMapper,
            AgentQueryConditionResolver fallbackResolver
    ) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.fallbackResolver = fallbackResolver;
    }

    public AgentPlan plan(
            String message,
            List<String> preferredFeatures,
            AgentPlanningContext context
    ) {
        AgentPlanningContext safeContext = context == null ? AgentPlanningContext.empty() : context;
        Optional<AgentPlan> conversationalPlan = simpleConversationPlan(message);
        if (conversationalPlan.isPresent()) {
            return conversationalPlan.get();
        }
        AgentQueryConditionResponse fallbackCondition = fallbackResolver.resolveFollowUp(
                message,
                preferredFeatures,
                safeContext.previousMessage(),
                List.of()
        );

        try {
            String response = llmClient.complete(SYSTEM_PROMPT, buildPrompt(message, preferredFeatures, safeContext));
            AgentPlan plan = parsePlan(response, fallbackCondition, safeContext);
            if (plan.confidence() < 0.35) {
                return fallbackPlan(message, preferredFeatures, safeContext, fallbackCondition, "LLM confidence가 낮아 fallback 해석을 사용했습니다.");
            }
            return plan;
        } catch (RuntimeException ex) {
            log.warn("AgentPlanner LLM 계획 수립 실패. QueryCondition fallback을 사용합니다. cause={}", ex.toString());
            return fallbackPlan(message, preferredFeatures, safeContext, fallbackCondition, "LLM 계획 수립 실패로 fallback 해석을 사용했습니다.");
        }
    }

    private String buildPrompt(
            String message,
            List<String> preferredFeatures,
            AgentPlanningContext context
    ) {
        return """
                최신 사용자 질문:
                %s

                사용자가 명시한 선호 기능:
                %s

                이전 대화 요약:
                - 이전 질문: %s
                - 이전 요약: %s
                - 이전 추천/후보 제목: %s
                - 이전 리포트 발췌: %s

                세션 Conversation Memory:
                - 최근 대화 요약: %s
                - 이전 intent/userRole: %s / %s
                - 선호 플랫폼: %s
                - 선호 정렬 기준: %s
                - 이전 추천 게임: %s
                - 이전 개발 후보: %s
                - 이전 재해석 후보: %s
                - 상호작용 기능: %s
                - 사용자 제약: %s

                사용 가능한 도구:
                - LiveTrendService: Twitch/CHZZK/SOOP 라이브 트렌드 순위 조회
                - ReinterpretationCandidateService: 과거 게임 재해석 후보 조회
                - TrendGameService: 내부 보조 트렌드 조회. 공개 답변 근거의 우선순위는 낮습니다.
                - GEMMA4 E2B AnswerSynthesizer: evidence 기반 최종 답변 생성

                intent 후보:
                TREND_ANALYSIS, USER_GAME_RECOMMENDATION, DEVELOPER_MARKET_ANALYSIS,
                STREAMING_FIT_ANALYSIS, INTERACTION_GAME_IDEA, GAME_REINTERPRETATION,
                GENERAL_GAME_ADVICE, CLARIFICATION_REQUIRED, SMALL_TALK, GREETING, HELP, OUT_OF_SCOPE

                userRole 후보:
                PLAYER, DEVELOPER, STREAMER, UNKNOWN

                responseDepth 후보:
                SHORT, NORMAL, DETAILED

                판단 규칙:
                - "안녕", "안녕하세요", "반가워" 같은 인사는 GREETING입니다.
                - "뭐 할 수 있어", "너는 뭐야", "도움말", "사용법 알려줘"는 HELP입니다.
                - 인사/일상 대화는 needsLiveTrend=false, needsReinterpretation=false, needsGameRecommendation=false로 둡니다.
                - 최신 질문의 의도가 명확하면 이전 대화 intent보다 최신 질문을 우선합니다.
                - "요즘 할만한 게임", "내가 할 게임", "친구랑 할 게임"은 PLAYER의 USER_GAME_RECOMMENDATION입니다.
                - "친구랑 한다고 했는데", "같이 한다고 했는데"처럼 이전 추천을 바로잡는 말은 친구/협동 조건의 USER_GAME_RECOMMENDATION이며 genreFilter=PARTY입니다.
                - "과거 게임", "재해석", "다시 만들", "리메이크"는 GAME_REINTERPRETATION입니다.
                - "개발하면", "시장성", "기획", "MVP", "프로토타입"은 DEVELOPER_MARKET_ANALYSIS입니다.
                - "치지직 기준", "트위치 기준"은 platformFilter를 설정합니다.
                - "fps", "공포", "파티", "생존", "rpg", "moba", "퍼즐"처럼 장르가 명시되면 genreFilter를 설정합니다.
                - "자세히", "구체적으로", "이유까지", "분석해줘", "리포트로", "단계별로"가 있으면 responseDepth=DETAILED입니다.
                - 일반적인 질문은 responseDepth=SHORT 또는 NORMAL입니다.
                - "그거", "그 게임", "아까 말한 게임", "방금 추천한 것", "그 후보", "그 기준", "위에 말한 것"이 있으면 세션 Conversation Memory에서 resolvedTopic을 추론합니다.
                - 최신 질문에 명시된 게임명이 있으면 그걸 최우선 resolvedTopic으로 사용합니다.
                - 개발 관련 질문이면 developerCandidates 또는 reinterpretationCandidates를 recommendedGames보다 우선합니다.
                - 플레이어 추천 관련 질문이면 recommendedGames를 우선합니다.
                - 답변할 수 있는 방향이 하나라도 있으면 바로 포기하지 말고, needsClarification=true와 임시 해석을 함께 둡니다.
                - 모호해도 답변을 포기하지 말고 needsClarification=true와 임시 답변 방향을 함께 계획합니다.

                JSON 스키마:
                {
                  "intent": "USER_GAME_RECOMMENDATION",
                  "userRole": "PLAYER",
                  "platformFilter": "ALL",
                  "genreFilter": "FPS",
                  "sortMetric": "TREND_SCORE",
                  "analysisPurpose": "USER_GAME_RECOMMENDATION",
                  "interactionFeatures": ["WEBCAM"],
                  "needsLiveTrend": true,
                  "needsReinterpretation": false,
                  "needsGameRecommendation": true,
                  "needsClarification": false,
                  "referencedPreviousTopic": "그 게임",
                  "resolvedTopic": "Counter-Strike 2",
                  "answerStyle": "대화체, 직접 답변",
                  "confidence": 0.82,
                  "reasoningSummary": "최신 질문이 플레이할 게임 추천을 요청함",
                  "responseDepth": "NORMAL"
                }
                """.formatted(
                safe(message),
                preferredFeatures == null ? List.of() : preferredFeatures,
                safe(context.previousMessage()),
                safe(context.previousSummary()),
                context.previousConceptTitles() == null ? List.of() : context.previousConceptTitles(),
                truncate(context.previousReport(), 700),
                safe(context.memorySummary() == null ? null : context.memorySummary().summaryText()),
                safe(context.memorySummary() == null ? null : context.memorySummary().lastIntent()),
                safe(context.memorySummary() == null ? null : context.memorySummary().lastUserRole()),
                safe(context.memorySummary() == null ? null : context.memorySummary().preferredPlatform()),
                safe(context.memorySummary() == null ? null : context.memorySummary().preferredSortMetric()),
                context.memorySummary() == null ? List.of() : context.memorySummary().recommendedGames(),
                context.memorySummary() == null ? List.of() : context.memorySummary().developerCandidates(),
                context.memorySummary() == null ? List.of() : context.memorySummary().reinterpretationCandidates(),
                context.memorySummary() == null ? List.of() : context.memorySummary().interactionFeatures(),
                context.memorySummary() == null ? List.of() : context.memorySummary().constraints()
        );
    }

    private AgentPlan parsePlan(
            String response,
            AgentQueryConditionResponse fallbackCondition,
            AgentPlanningContext context
    ) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(response));
            String intent = normalizedChoice(text(root, "intent", fallbackCondition.analysisPurpose()), fallbackCondition.analysisPurpose());
            String analysisPurpose = normalizedChoice(text(root, "analysisPurpose", intent), intent);
            if (isConversationalIntent(intent)) {
                analysisPurpose = intent;
            }
            String platformFilter = text(root, "platformFilter", fallbackCondition.platformFilter()).toUpperCase(Locale.ROOT);
            List<String> features = stringList(root.get("interactionFeatures"));
            if (features.isEmpty()) {
                features = fallbackCondition.interactionFeatures();
            }
            String resolvedTopic = text(root, "resolvedTopic", "");
            if (resolvedTopic.isBlank()) {
                resolvedTopic = inferPreviousTopic(fallbackCondition.originalMessage(), context);
            }
            boolean conversationalIntent = isConversationalIntent(intent) || isConversationalIntent(analysisPurpose);
            String responseDepth = normalizeResponseDepth(text(
                    root,
                    "responseDepth",
                    responseDepthFor(fallbackCondition.originalMessage())
            ));
            String genreFilter = normalizeGenreFilter(text(
                    root,
                    "genreFilter",
                    extractGenreFilter(fallbackCondition.originalMessage())
            ));

            return new AgentPlan(
                    intent,
                    text(root, "userRole", userRoleFor(analysisPurpose)),
                    platformFilter.isBlank() ? "ALL" : platformFilter,
                    genreFilter,
                    text(root, "sortMetric", fallbackCondition.sortMetric()),
                    analysisPurpose,
                    features,
                    conversationalIntent ? false : bool(root, "needsLiveTrend", needsLiveTrend(analysisPurpose)),
                    conversationalIntent ? false : bool(root, "needsReinterpretation", "GAME_REINTERPRETATION".equals(analysisPurpose)),
                    conversationalIntent ? false : bool(root, "needsGameRecommendation", "USER_GAME_RECOMMENDATION".equals(analysisPurpose)),
                    conversationalIntent ? false : bool(root, "needsClarification", false),
                    text(root, "referencedPreviousTopic", ""),
                    resolvedTopic,
                    text(root, "answerStyle", "대화체, 직접 답변"),
                    confidence(root, "confidence", 0.6),
                    text(root, "reasoningSummary", "LLM이 최신 질문과 이전 맥락을 함께 고려했습니다."),
                    responseDepth
            );
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.warn("AgentPlanner JSON 파싱 실패. QueryCondition fallback을 사용합니다. cause={}", ex.toString());
            return fallbackPlan(fallbackCondition.originalMessage(), List.of(), context, fallbackCondition, "LLM JSON 파싱 실패로 fallback 해석을 사용했습니다.");
        }
    }

    private AgentPlan fallbackPlan(
            String message,
            List<String> preferredFeatures,
            AgentPlanningContext context,
            AgentQueryConditionResponse condition,
            String reason
    ) {
        String purpose = condition.analysisPurpose();
        return new AgentPlan(
                purpose,
                userRoleFor(purpose),
                condition.platformFilter(),
                extractGenreFilter(message),
                condition.sortMetric(),
                purpose,
                condition.interactionFeatures() == null ? List.of() : condition.interactionFeatures(),
                needsLiveTrend(purpose),
                "GAME_REINTERPRETATION".equals(purpose),
                "USER_GAME_RECOMMENDATION".equals(purpose),
                isPotentiallyAmbiguous(message),
                referencesPreviousTopic(message) ? "이전 답변 참조" : "",
                inferPreviousTopic(message, context),
                "대화체, 직접 답변",
                0.45,
                reason,
                responseDepthFor(message)
        );
    }

    private Optional<AgentPlan> simpleConversationPlan(String message) {
        String normalized = normalize(message);
        if (isHelp(normalized)) {
            return Optional.of(conversationPlan("HELP", "기능 안내 요청으로 판단했습니다.", responseDepthFor(message)));
        }
        if (isGreeting(normalized)) {
            return Optional.of(conversationPlan("GREETING", "인사 표현으로 판단했습니다.", "SHORT"));
        }
        if (isSmallTalk(normalized)) {
            return Optional.of(conversationPlan("SMALL_TALK", "게임 분석이 아닌 일상 대화로 판단했습니다.", responseDepthFor(message)));
        }
        if (isOutOfScope(normalized)) {
            return Optional.of(conversationPlan("OUT_OF_SCOPE", "게임 분석 범위를 벗어난 질문으로 판단했습니다.", responseDepthFor(message)));
        }
        return Optional.empty();
    }

    private AgentPlan conversationPlan(String intent, String reason, String responseDepth) {
        return new AgentPlan(
                intent,
                "UNKNOWN",
                "ALL",
                null,                    // ← genreFilter 신규 (대화는 null)
                "TREND_SCORE",
                intent,
                List.of(),
                false,
                false,
                false,
                false,
                "",
                "",
                "짧고 자연스러운 대화체",
                0.95,
                reason,
                responseDepth
        );
    }

    private String extractJsonObject(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("LLM 응답이 비어 있습니다.");
        }
        String stripped = response.strip();
        int start = stripped.indexOf('{');
        int end = stripped.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("JSON 객체를 찾을 수 없습니다.");
        }
        return stripped.substring(start, end + 1);
    }

    private List<String> stringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (item != null && item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText().strip().toUpperCase(Locale.ROOT));
            }
        });
        return List.copyOf(values);
    }

    private String text(JsonNode root, String field, String defaultValue) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return defaultValue == null ? "" : defaultValue;
        }
        return node.asText().strip();
    }

    private boolean bool(JsonNode root, String field, boolean defaultValue) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return Boolean.parseBoolean(node.asText());
    }

    private double confidence(JsonNode root, String field, double defaultValue) {
        JsonNode node = root.get(field);
        if (node == null || !node.isNumber()) {
            return defaultValue;
        }
        double value = node.asDouble();
        if (value > 1.0) {
            value = value / 100.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String normalizedChoice(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }

    private boolean needsLiveTrend(String purpose) {
        return switch (purpose) {
            case "TREND_ANALYSIS", "USER_GAME_RECOMMENDATION", "DEVELOPER_MARKET_ANALYSIS", "STREAMING_FIT_ANALYSIS", "INTERACTION_GAME_IDEA" -> true;
            default -> false;
        };
    }

    private boolean isConversationalIntent(String intent) {
        return switch (nullToEmpty(intent)) {
            case "SMALL_TALK", "GREETING", "HELP", "OUT_OF_SCOPE" -> true;
            default -> false;
        };
    }

    private String userRoleFor(String purpose) {
        return switch (purpose) {
            case "USER_GAME_RECOMMENDATION" -> "PLAYER";
            case "DEVELOPER_MARKET_ANALYSIS", "INTERACTION_GAME_IDEA", "GAME_REINTERPRETATION" -> "DEVELOPER";
            case "STREAMING_FIT_ANALYSIS" -> "STREAMER";
            default -> "UNKNOWN";
        };
    }

    private String inferPreviousTopic(String message, AgentPlanningContext context) {
        if (!referencesPreviousTopic(message)) {
            return "";
        }
        if (context.memorySummary() != null) {
            String normalizedMessage = normalize(message);
            if (containsAny(normalizedMessage, "그 게임", "방금 추천", "추천한 것", "네가 알려준 게임")) {
                String recommendedTopic = firstFrom(context.memorySummary().recommendedGames());
                if (!recommendedTopic.isBlank()) {
                    return recommendedTopic;
                }
            }
            if (containsAny(normalizedMessage, "개발", "만들", "가치", "시장성", "후보")) {
                String developerTopic = firstFrom(context.memorySummary().developerCandidates());
                if (!developerTopic.isBlank()) {
                    return developerTopic;
                }
                String reinterpretationTopic = firstFrom(context.memorySummary().reinterpretationCandidates());
                if (!reinterpretationTopic.isBlank()) {
                    return reinterpretationTopic;
                }
            }
            String recommendedTopic = firstFrom(context.memorySummary().recommendedGames());
            if (!recommendedTopic.isBlank()) {
                return recommendedTopic;
            }
            String mentionedTopic = firstFrom(context.memorySummary().mentionedGames());
            if (!mentionedTopic.isBlank()) {
                return mentionedTopic;
            }
        }
        if (context.previousConceptTitles() != null && !context.previousConceptTitles().isEmpty()) {
            return context.previousConceptTitles().get(0);
        }
        return "";
    }

    private boolean referencesPreviousTopic(String message) {
        String normalized = normalize(message);
        return containsAny(normalized, "그거", "그 게임", "네가 알려준", "아까", "그 후보", "그걸", "아까 말한 게임", "방금 추천한 것", "그 기준", "위에 말한 것");
    }

    private String firstFrom(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private boolean isPotentiallyAmbiguous(String message) {
        String normalized = normalize(message);
        return normalized.length() <= 50 && containsAny(normalized, "그럼", "왜", "더 자세히", "다시", "그 기준", "알려줘", "했는데", "한다고");
    }

    private String responseDepthFor(String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, "자세히", "구체적으로", "이유까지", "분석해줘", "리포트로", "단계별로")) {
            return "DETAILED";
        }
        if (normalized.length() <= 30 && !containsAny(normalized, "추천", "트렌드", "개발", "재해석")) {
            return "SHORT";
        }
        return "NORMAL";
    }

    private String normalizeResponseDepth(String value) {
        String normalized = nullToEmpty(value);
        return switch (normalized) {
            case "SHORT", "NORMAL", "DETAILED" -> normalized;
            default -> "NORMAL";
        };
    }

    private boolean isGreeting(String normalizedMessage) {
        String compact = normalizedMessage.replace(" ", "");
        return containsAny(compact, "안녕", "안녕하세요", "반가워", "반갑습니다");
    }

    private boolean isHelp(String normalizedMessage) {
        String compact = normalizedMessage.replace(" ", "");
        return containsAny(
                compact,
                "뭐할수있어",
                "뭐할수있니",
                "무엇을할수있어",
                "너는뭐야",
                "도움말",
                "사용법알려줘",
                "사용법",
                "help"
        );
    }

    private boolean isSmallTalk(String normalizedMessage) {
        String compact = normalizedMessage.replace(" ", "");
        return containsAny(compact, "뭐해", "뭐하니", "잘지내", "심심해");
    }

    private boolean isOutOfScope(String normalizedMessage) {
        if (normalizedMessage.isBlank() || isGameDomainMessage(normalizedMessage)) {
            return false;
        }
        return containsAny(
                normalizedMessage,
                "날씨",
                "점심",
                "저녁",
                "요리",
                "주식",
                "코인",
                "부동산",
                "여행",
                "영화 추천",
                "수학 문제"
        );
    }

    private boolean isGameDomainMessage(String normalizedMessage) {
        return containsAny(
                normalizedMessage,
                "게임",
                "트렌드",
                "인기",
                "방송",
                "스트리머",
                "트위치",
                "치지직",
                "soop",
                "스팀",
                "개발",
                "장르",
                "웹캠",
                "tts",
                "stt",
                "추천",
                "플레이",
                "친구랑",
                "친구와",
                "같이",
                "함께",
                "협동"
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).strip();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... keywords) {
        if (value == null || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && value.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "없음" : value.strip();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "없음";
        }
        String stripped = value.strip();
        return stripped.length() <= maxLength ? stripped : stripped.substring(0, maxLength) + "...";
    }

    private String normalizeGenreFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALL", "NONE", "NULL", "없음", "전체" -> null;
            case "FPS", "HORROR", "PARTY", "SURVIVAL", "RPG", "MOBA", "PUZZLE", "SPORTS" -> normalized;
            case "SHOOTER", "BATTLE_ROYALE", "BATTLE ROYALE" -> "FPS";
            case "ADVENTURE", "PUZZLE_ADVENTURE" -> "PUZZLE";
            case "SURVIVAL_ROGUELIKE", "ROGUELIKE" -> "SURVIVAL";
            default -> normalized;
        };
    }

    private String extractGenreFilter(String message) {
        if (message == null) return null;
        String m = message.toLowerCase(Locale.ROOT);
        if (containsAny(m, "fps", "슈팅", "배그", "배틀로얄", "발로란트", "서든")) return "FPS";
        if (containsAny(m, "공포", "horror", "호러", "귀신")) return "HORROR";
        if (containsAny(m, "파티", "party", "같이", "함께", "친구", "친구랑", "친구와", "협동", "멀티", "co-op", "coop")) return "PARTY";
        if (containsAny(m, "생존", "서바이벌", "survival", "마크", "minecraft")) return "SURVIVAL";
        if (containsAny(m, "rpg", "롤플레잉")) return "RPG";
        if (containsAny(m, "moba", "롤", "리그오브레전드", "도타")) return "MOBA";
        if (containsAny(m, "퍼즐", "puzzle", "어드벤처")) return "PUZZLE";
        return null; // 장르 명시 없으면 null → 전체 통과
    }

}
