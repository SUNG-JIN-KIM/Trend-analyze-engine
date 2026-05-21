package com.gametrend.agent.onboarding.service;

import com.gametrend.agent.infrastructure.llm.LlmClient;
import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.onboarding.dto.AgentAnswerDraft;
import com.gametrend.agent.onboarding.dto.AgentEvidenceBundle;
import com.gametrend.agent.onboarding.dto.AgentPlan;
import com.gametrend.agent.onboarding.dto.AgentPlanningContext;
import com.gametrend.agent.reinterpretation.dto.ReinterpretationCandidateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AgentAnswerSynthesizer {

    // ✅ 수정 1: 시스템 프롬프트를 역할 중심으로 명확하게 재작성
    // 기존: 너무 포괄적 → LLM이 "모든 기능 소개"를 하려는 경향
    // 개선: 대화체 + 길이 제한 + 내부정보 노출 금지를 명시
    private static final String SYSTEM_PROMPT = """
            당신은 게임 트렌드를 잘 아는 친근한 AI입니다.
            사용자의 질문에 짧고 자연스럽게 한국어 대화체로 답하세요.

            반드시 지켜야 할 규칙:
            1. 기본 답변은 2~4문장입니다. 사용자가 "자세히", "분석해줘"처럼 요청할 때만 길게 씁니다.
            2. 인사나 일상 대화에는 게임 분석 데이터를 사용하지 않습니다.
            3. 게임 추천은 기본 3개만 합니다.
            4. AgentPlan, sessionId, memory, intent 같은 내부 용어를 답변에 절대 쓰지 않습니다.
            5. "MVP"라는 단어 대신 "프로토타입" 또는 "작게 검증하는 버전"이라고 씁니다.
            6. 데이터가 없으면 없다고 솔직히 말하고 대안을 제안합니다.
            7. 숫자는 핵심 근거 하나만 쓰고 나열하지 않습니다.
            8. 항상 한국어로만 답합니다.
            9. 첫 문장은 사용자의 질문에 바로 답하고, 이후에 이유를 붙입니다.
            10. 질문이 애매하면 확인 질문은 하나만 하고, 가능한 임시 해석도 함께 제시합니다.
            11. 게임 범위 밖 질문은 짧게 한계를 말한 뒤 게임 추천/트렌드/개발 관점으로 자연스럽게 연결합니다.
            """;

    // ✅ 수정 2: 대화형 intent는 LLM 호출 없이 즉시 반환
    // 기존: GREETING도 전체 프롬프트를 LLM에 던짐 → "게임 추천, 실시간 인기 게임..." 같은 기능 나열 출력
    private static final Set<String> CONVERSATIONAL_INTENTS = Set.of(
            "GREETING", "SMALL_TALK", "HELP", "OUT_OF_SCOPE"
    );

    private final LlmClient llmClient;

    public AgentAnswerSynthesizer(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public AgentAnswerDraft synthesize(
            String message,
            AgentPlanningContext context,
            AgentPlan plan,
            AgentEvidenceBundle evidence,
            String fallbackSummary,
            String fallbackAnswer
    ) {
        // ✅ 핵심 수정: GREETING/SMALL_TALK/HELP는 LLM 없이 바로 자연스러운 답변 반환
        if (CONVERSATIONAL_INTENTS.contains(plan.intent())) {
            return conversationalDraft(plan, context);
        }

        try {
            String userPrompt = buildPrompt(message, context, plan, evidence);
            String content = llmClient.complete(SYSTEM_PROMPT, userPrompt);

            if (content == null || content.isBlank()) {
                return fallbackDraft(message, plan, evidence, context, fallbackSummary, fallbackAnswer);
            }

            // ✅ 수정 3: firstUsefulParagraph → extractAnswer로 교체
            // 기존: 첫 문장만 반환, #으로 시작하는 줄·"- "으로 시작하는 줄 모두 제거 → 유용한 내용이 다 잘림
            // 개선: 내부 용어 노출 줄만 걸러내고 전체 답변을 자연스럽게 반환
            String answer = extractAnswer(content);

            return new AgentAnswerDraft(
                    summaryFromPlan(plan, evidence, fallbackSummary),
                    answer.isBlank() ? fallbackAnswer(message, plan, evidence, fallbackAnswer) : answer,
                    content.strip(),
                    followUps(plan, context)
            );
        } catch (RuntimeException ex) {
            log.warn("AgentAnswerSynthesizer LLM 답변 생성 실패. fallback을 사용합니다. cause={}", ex.toString());
            return fallbackDraft(message, plan, evidence, context, fallbackSummary, fallbackAnswer);
        }
    }

    // ✅ 신규: GREETING/HELP/SMALL_TALK 전용 자연스러운 즉시 답변
    private AgentAnswerDraft conversationalDraft(AgentPlan plan, AgentPlanningContext context) {
        String answer = switch (plan.intent()) {
            case "GREETING" -> "안녕하세요! 요즘 할만한 게임, 방송 트렌드, 개발 아이디어 뭐든 편하게 물어보세요.";
            case "HELP" -> """
                    이런 것들을 물어볼 수 있어요:
                    - "요즘 할만한 게임 추천해줘" → 지금 인기 있는 게임 추천
                    - "방송에서 뜨는 게임 뭐야?" → Twitch/치지직 실시간 트렌드
                    - "어떤 게임 개발하면 좋을까?" → 장르·시장성 분석
                    - "웹캠/마이크로 만들 수 있는 게임 있어?" → 인터랙션 아이디어
                    - "스타크래프트 재해석하면 어때?" → 과거 게임 현대화 방향
                    """;
            case "SMALL_TALK" -> "저는 게임 트렌드 분석 전문이라 일상 대화는 좀 서툴러요 😅 게임 얘기라면 뭐든 물어보세요!";
            case "OUT_OF_SCOPE" -> "그 주제는 제 전문 밖이에요. 게임 추천이나 트렌드 관련 질문이라면 바로 도와드릴 수 있어요!";
            default -> "게임 트렌드, 추천, 개발 아이디어 뭐든 물어보세요!";
        };

        return new AgentAnswerDraft(
                null,
                answer,
                answer,
                followUps(plan, context)
        );
    }

    // ✅ 수정 3 상세: 답변 추출 로직 개선
    // 기존 firstUsefulParagraph(): #줄, - 줄 모두 제거 후 첫 줄만 → 게임 목록·이유 설명이 전부 잘림
    // 개선 extractAnswer(): 내부 용어 노출 줄만 제거, 나머지 전체 반환
    private String extractAnswer(String content) {
        if (content == null || content.isBlank()) return "";

        List<String> internalTerms = List.of(
                "agentplan", "sessionid", "memory", "intent=", "userrole=",
                "needslivetrend", "needsreinterpretation", "confidence=",
                "analysispurpose", "resolvedtopic", "genrefilter", "platformfilter"
        );

        String cleaned = content.lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .filter(line -> {
                    String lower = line.toLowerCase();
                    return internalTerms.stream().noneMatch(lower::contains);
                })
                .collect(Collectors.joining("\n"))
                .strip();

        return cleaned;
    }

    // ✅ 수정 4: buildPrompt 슬림화
    // 기존: AgentPlan 전체 필드를 프롬프트에 나열 → LLM이 그걸 답변에 출력하려는 경향
    // 개선: LLM이 실제로 답변 생성에 필요한 정보만 전달
    private String buildPrompt(
            String message,
            AgentPlanningContext context,
            AgentPlan plan,
            AgentEvidenceBundle evidence
    ) {
        String depthGuide = switch (plan.responseDepth()) {
            case "SHORT" -> "2~3문장으로 핵심만 답하세요.";
            case "DETAILED" -> "이유, 리스크, 다음 단계까지 조금 더 자세히 설명하세요.";
            default -> "후보 3개 중심으로 간결하게 답하세요.";
        };

        String questionUnderstanding = buildQuestionUnderstanding(plan);
        String playContext = buildPlayContext(message, plan);
        String previousContext = buildPreviousContext(context);
        String evidenceSection = buildEvidenceSection(plan, evidence);
        String roleGuide = buildRoleGuide(plan);
        String clarificationGuide = plan.needsClarification()
                ? "질문이 넓거나 모호합니다. 답을 미루지 말고 가능한 해석으로 먼저 답한 뒤, 마지막에 확인 질문 하나만 덧붙이세요."
                : "";

        return """
                사용자 질문: %s

                질문 해석:
                %s
                %s

                %s

                답변 방식: %s

                %s

                %s

                근거 데이터:
                %s

                위 정보를 바탕으로 사용자 질문에 자연스럽게 답하세요.
                내부 시스템 정보(AgentPlan, sessionId, memory, intent 등)는 절대 답변에 쓰지 마세요.
                """.formatted(
                safe(message),
                questionUnderstanding,
                playContext,
                roleGuide,
                depthGuide,
                clarificationGuide,
                previousContext,
                evidenceSection
        );
    }

    private String buildQuestionUnderstanding(AgentPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("- 사용자 관점: ").append(userRoleLabel(plan.userRole())).append("\n");
        sb.append("- 요청 목적: ").append(purposeLabel(plan.analysisPurpose())).append("\n");
        sb.append("- 관심 플랫폼: ").append(displayValue(plan.platformFilter(), "전체")).append("\n");
        sb.append("- 관심 장르: ").append(displayValue(plan.genreFilter(), "전체")).append("\n");
        if (plan.interactionFeatures() != null && !plan.interactionFeatures().isEmpty()) {
            sb.append("- 관심 인터랙션: ").append(String.join(", ", plan.interactionFeatures())).append("\n");
        }
        sb.append("- 판단 확신도: ").append(confidenceLabel(plan.confidence()));
        return sb.toString();
    }

    private String buildPlayContext(String message, AgentPlan plan) {
        String normalized = safe(message).toLowerCase(Locale.ROOT);
        if (!"USER_GAME_RECOMMENDATION".equals(safe(plan.analysisPurpose()))) {
            return "";
        }
        if (containsAny(normalized, "혼자", "솔로", "1인", "싱글", "single", "singleplayer", "single-player", "solo")) {
            return "- 플레이 상황: 혼자 플레이. 이미 명시된 조건이므로 혼자/친구 여부를 다시 묻지 말고 싱글 또는 솔로 친화 게임을 추천하세요.";
        }
        if (containsAny(normalized, "친구랑", "친구와", "같이", "함께", "협동", "멀티", "co-op", "coop")) {
            return "- 플레이 상황: 친구와 함께 플레이. 협동, 파티성, 역할 분담을 우선하세요.";
        }
        return "- 플레이 상황: 미정. 추천 후 확인 질문은 하나만 덧붙일 수 있습니다.";
    }

    private String userRoleLabel(String userRole) {
        return switch (safe(userRole).toUpperCase(Locale.ROOT)) {
            case "PLAYER" -> "플레이어";
            case "DEVELOPER" -> "개발자";
            case "STREAMER" -> "스트리머";
            default -> "일반 사용자";
        };
    }

    private String purposeLabel(String analysisPurpose) {
        return switch (safe(analysisPurpose).toUpperCase(Locale.ROOT)) {
            case "USER_GAME_RECOMMENDATION" -> "플레이할 게임 추천";
            case "DEVELOPER_MARKET_ANALYSIS" -> "개발 아이디어와 시장성 판단";
            case "GAME_REINTERPRETATION" -> "과거 게임 재해석";
            case "INTERACTION_GAME_IDEA" -> "웹캠/TTS/STT 기반 인터랙션 아이디어";
            case "STREAMING_FIT_ANALYSIS" -> "방송 적합성 분석";
            case "TREND_ANALYSIS" -> "게임 트렌드 분석";
            case "HELP" -> "기능 안내";
            case "OUT_OF_SCOPE" -> "게임 분석 범위 밖 질문";
            default -> "게임 관련 일반 조언";
        };
    }

    private String confidenceLabel(double confidence) {
        if (confidence >= 0.75) {
            return "높음";
        }
        if (confidence >= 0.45) {
            return "보통";
        }
        return "낮음";
    }

    private String displayValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.strip();
    }

    private String buildRoleGuide(AgentPlan plan) {
        return switch (safe(plan.analysisPurpose())) {
            case "USER_GAME_RECOMMENDATION" -> {
                if ("PARTY".equals(safe(plan.genreFilter()))) {
                    yield "관점: 친구와 같이 할 게임을 찾는 사람에게 추천합니다. 순수 인기 순위보다 협동, 파티성, 역할 분담, 같이 웃을 상황을 우선하고 도박/비게임 카테고리는 추천하지 마세요.";
                }
                if ("RPG".equals(safe(plan.genreFilter()))) {
                    yield "관점: RPG 게임을 찾는 사람에게 추천합니다. 실시간 인기 순위보다 성장 방식, 전투 취향, 혼자/협동 플레이 적합성을 우선하세요.";
                }
                if ("FPS".equals(safe(plan.genreFilter()))) {
                    yield "관점: FPS 게임을 찾는 사람에게 추천합니다. 혼자 플레이가 명시되면 팀 기반 경쟁 FPS보다 싱글 캠페인이나 솔로 친화 FPS를 우선하세요.";
                }
                yield "관점: 지금 플레이할 게임을 찾는 사람에게 추천합니다. 혼자/친구/가볍게 상황을 자연스럽게 나눠주세요.";
            }
            case "DEVELOPER_MARKET_ANALYSIS" ->
                    "관점: 개발자에게 장르 흐름, 시장성, 구현 가능성, 작게 검증할 방향을 제안합니다.";
            case "GAME_REINTERPRETATION" ->
                    "관점: 과거 게임의 핵심 재미 구조를 현대적으로 바꾸는 방향을 설명합니다. 원작 복제가 아닌 재해석입니다.";
            case "INTERACTION_GAME_IDEA" ->
                    "관점: 웹캠/마이크/TTS/STT 같은 하드웨어를 활용한 인터랙션 게임 아이디어를 제안합니다.";
            case "STREAMING_FIT_ANALYSIS" ->
                    "관점: 방송/스트리밍에서 시청자 반응성이 좋은 게임을 분석합니다.";
            default -> "관점: 게임 트렌드와 관련된 질문에 자연스럽게 답합니다.";
        };
    }

    private String buildPreviousContext(AgentPlanningContext context) {
        if (context == null || context.memorySummary() == null) return "";

        StringBuilder sb = new StringBuilder();
        String summaryText = context.memorySummary().summaryText();
        if (summaryText != null && !summaryText.isBlank()) {
            sb.append("이전 대화 맥락: ").append(summaryText.strip()).append("\n");
        }
        List<String> recommended = context.memorySummary().recommendedGames();
        if (recommended != null && !recommended.isEmpty()) {
            sb.append("이전에 추천한 게임: ").append(String.join(", ", recommended)).append("\n");
        }
        List<String> interactionFeatures = context.memorySummary().interactionFeatures();
        if (interactionFeatures != null && !interactionFeatures.isEmpty()) {
            sb.append("이전에 관심 보인 인터랙션: ").append(String.join(", ", interactionFeatures)).append("\n");
        }
        List<String> constraints = context.memorySummary().constraints();
        if (constraints != null && !constraints.isEmpty()) {
            sb.append("사용자 제약/선호: ").append(String.join(", ", constraints)).append("\n");
        }
        return sb.toString().strip();
    }

    private String buildEvidenceSection(AgentPlan plan, AgentEvidenceBundle evidence) {
        if (evidence == null) {
            return "관련 데이터 없음";
        }

        StringBuilder sb = new StringBuilder();

        if (plan.needsLiveTrend() && evidence.liveTrendGames() != null && !evidence.liveTrendGames().isEmpty()) {
            sb.append("실시간 트렌드 (상위 3개):\n");
            sb.append(liveTrendLines(evidence.liveTrendGames()));
        } else if (plan.needsLiveTrend()) {
            sb.append("실시간 트렌드: 현재 데이터 없음\n");
        }

        if (plan.needsReinterpretation() && evidence.reinterpretationCandidates() != null && !evidence.reinterpretationCandidates().isEmpty()) {
            sb.append("\n재해석 후보 (상위 3개):\n");
            sb.append(reinterpretationLines(evidence.reinterpretationCandidates()));
        }

        if (sb.toString().isBlank() && evidence.evidenceCards() != null && !evidence.evidenceCards().isEmpty()) {
            sb.append("보조 근거:\n");
            evidence.evidenceCards().stream()
                    .limit(3)
                    .forEach(card -> sb.append("- ")
                            .append(safe(card.title()))
                            .append(" | ")
                            .append(safe(card.description()))
                            .append("\n"));
        }

        return sb.toString().isBlank() ? "관련 데이터 없음" : sb.toString().strip();
    }

    private AgentAnswerDraft fallbackDraft(
            String message,
            AgentPlan plan,
            AgentEvidenceBundle evidence,
            AgentPlanningContext context,
            String fallbackSummary,
            String fallbackAnswer
    ) {
        return new AgentAnswerDraft(
                summaryFromPlan(plan, evidence, fallbackSummary),
                fallbackAnswer(message, plan, evidence, fallbackAnswer),
                fallbackReport(plan, evidence, fallbackSummary, fallbackAnswer),
                followUps(plan, context)
        );
    }

    private String summaryFromPlan(AgentPlan plan, AgentEvidenceBundle evidence, String fallbackSummary) {
        if (plan.needsGameRecommendation() && !evidence.liveTrendGames().isEmpty()) {
            return "플레이어 관점의 게임 추천 요청입니다.";
        }
        if (plan.needsReinterpretation() && !evidence.reinterpretationCandidates().isEmpty()) {
            return "과거 게임 재해석 관점의 요청입니다.";
        }
        if ("DEVELOPER_MARKET_ANALYSIS".equals(plan.analysisPurpose())) {
            return "개발자 관점의 시장/장르 분석 요청입니다.";
        }
        return fallbackSummary;
    }

    private String fallbackAnswer(String message, AgentPlan plan, AgentEvidenceBundle evidence, String fallbackAnswer) {
        if (plan.needsGameRecommendation()) {
            boolean soloPlay = containsAny(safe(message).toLowerCase(Locale.ROOT), "혼자", "솔로", "1인", "싱글", "single", "singleplayer", "single-player", "solo");
            if (!evidence.liveTrendGames().isEmpty()) {
                String titles = evidence.liveTrendGames().stream()
                        .limit(3)
                        .map(game -> "%s(%s)".formatted(game.title(), game.source()))
                        .collect(Collectors.joining(", "));
                if (soloPlay) {
                    return "혼자 할 기준이면 %s를 먼저 볼 만해요. 이미 혼자 플레이 조건이 있으니 친구 여부를 다시 묻기보다, 싱글 진행감과 솔로 친화성을 우선으로 봐야 합니다.".formatted(titles);
                }
                if ("PARTY".equals(safe(plan.genreFilter()))) {
                    return "맞아요, 친구랑 할 기준이면 %s를 먼저 볼 만해요. 단순 인기보다 같이 역할을 나누거나 리액션이 나오는 게임을 우선으로 봐야 합니다.".formatted(titles);
                }
                return "지금 플레이할 게임으로는 %s를 먼저 볼 만해요. 혼자 할지, 친구랑 할지에 따라 더 좁혀드릴 수 있어요.".formatted(titles);
            }
            if (soloPlay && "FPS".equals(safe(plan.genreFilter()))) {
                return "혼자 할 FPS라면 Titanfall 2, DOOM Eternal, Metro Exodus 쪽을 먼저 추천해요. 팀 기반 경쟁 FPS보다 캠페인 완성도와 혼자 반복해도 재미있는 전투 흐름을 우선으로 보는 게 맞습니다.";
            }
            if ("RPG".equals(safe(plan.genreFilter()))) {
                return soloPlay
                        ? "혼자 할 RPG라면 Baldur's Gate 3, Elden Ring, Cyberpunk 2077을 먼저 추천해요. 실시간 인기 순위보다 성장 방식, 전투 난이도, 혼자 몰입할 수 있는 분량을 기준으로 봤습니다."
                        : "RPG 추천이라면 Monster Hunter Wilds, Baldur's Gate 3, Path of Exile 2를 먼저 볼 만해요. 혼자면 몰입형 RPG, 친구와면 협동 사냥이나 파밍형 RPG로 좁히는 게 좋습니다.";
            }
            if ("PARTY".equals(safe(plan.genreFilter()))) {
                return "친구랑 할 기준이면 협동 호러, 파티 게임, 샌드박스 생존처럼 같이 상황을 만들 수 있는 게임이 좋아요. 지금 라이브 데이터에는 조건에 맞는 후보가 부족해서, Lethal Company, Phasmophobia, It Takes Two, Overcooked, Minecraft 같은 방향부터 보는 게 맞습니다.";
            }
            return "어떤 상황에서 게임을 하고 싶은지 알려주시면 더 잘 추천해드릴 수 있어요. 혼자 할지, 친구랑 할지, 가볍게 할지 어떤가요?";
        }
        if (plan.needsReinterpretation()) {
            if (!evidence.reinterpretationCandidates().isEmpty()) {
                ReinterpretationCandidateResponse top = evidence.reinterpretationCandidates().get(0);
                return "%s를 %s 방향으로 재해석하는 게 가장 가능성 있어 보여요. 원작 메커니즘을 그대로 복제하기보다 시청자 참여나 인터랙션 요소로 바꾸는 쪽이 좋습니다."
                        .formatted(top.title(), top.reinterpretationConcept());
            }
            return "재해석 후보 데이터가 아직 부족해요. 어떤 장르나 게임을 생각하고 계신지 알려주시면 방향을 잡아드릴 수 있어요.";
        }
        if ("DEVELOPER_MARKET_ANALYSIS".equals(plan.analysisPurpose())) {
            if (!evidence.liveTrendGames().isEmpty()) {
                LiveTrendGameResponse top = evidence.liveTrendGames().get(0);
                return "개발 관점이라면 %s 같은 트렌드를 참고하되, 전체를 따라하기보다 핵심 재미 하나를 프로토타입으로 검증하는 게 좋아요.".formatted(top.title());
            }
            return "지금 어떤 장르를 생각하시는지 알려주시면 시장성과 구현 가능성을 함께 분석해드릴게요.";
        }
        return fallbackAnswer;
    }

    private String fallbackReport(AgentPlan plan, AgentEvidenceBundle evidence, String fallbackSummary, String fallbackAnswer) {
        return """
                ## 분석 요약
                %s

                ## 답변
                %s

                ## 근거
                %s
                %s
                """.formatted(
                summaryFromPlan(plan, evidence, fallbackSummary),
                fallbackAnswer("", plan, evidence, fallbackAnswer),
                liveTrendLines(evidence.liveTrendGames()),
                reinterpretationLines(evidence.reinterpretationCandidates())
        );
    }

    private List<String> followUps(AgentPlan plan, AgentPlanningContext context) {
        if (CONVERSATIONAL_INTENTS.contains(plan.intent())) {
            return List.of(
                    "요즘 할만한 게임 추천해줘",
                    "방송에서 뜨는 게임 알려줘",
                    "어떤 게임 개발하면 좋을까?",
                    "웹캠으로 만들 수 있는 게임 있어?"
            );
        }

        if (context != null && context.memorySummary() != null) {
            String lastIntent = context.memorySummary().lastIntent();
            if ("USER_GAME_RECOMMENDATION".equals(lastIntent) && "USER_GAME_RECOMMENDATION".equals(plan.analysisPurpose())) {
                return List.of("친구랑 할 게임으로 좁혀줘", "혼자 할 게임으로 좁혀줘", "가볍게 할 게임 추천해줘", "치지직 기준으로 다시 추천해줘");
            }
            if ("DEVELOPER_MARKET_ANALYSIS".equals(lastIntent) && "DEVELOPER_MARKET_ANALYSIS".equals(plan.analysisPurpose())) {
                return List.of("소규모 팀 기준으로 다시 분석해줘", "1인 개발 프로토타입으로 줄여줘", "리스크가 낮은 후보만 알려줘", "방송 반응성 기준으로 다시 봐줘");
            }
            if ("GAME_REINTERPRETATION".equals(lastIntent) && "GAME_REINTERPRETATION".equals(plan.analysisPurpose())) {
                return List.of("TTS/STT 중심으로 재해석해줘", "웹캠 중심으로 재해석해줘", "소규모 팀이 만들기 쉬운 후보만 알려줘", "시청자 참여형으로 바꿔줘");
            }
        }

        return switch (plan.analysisPurpose()) {
            case "USER_GAME_RECOMMENDATION" -> List.of("혼자 할 게임 추천해줘", "친구랑 할 게임 추천해줘", "가볍게 할 게임 추천해줘", "치지직 기준으로 인기 있는 게임 추천해줘");
            case "GAME_REINTERPRETATION" -> List.of("웹캠 중심으로 재해석해줘", "TTS/STT 중심으로 분석해줘", "소규모 팀이 만들기 쉬운 후보만 알려줘", "스트리밍 반응성이 높은 후보만 알려줘");
            case "DEVELOPER_MARKET_ANALYSIS" -> List.of("1인 개발 프로토타입 기준으로 분석해줘", "시장성 좋은 장르 중심으로 봐줘", "리스크 낮은 후보만 알려줘", "라이브 트렌드 근거로 다시 분석해줘");
            default -> List.of("Twitch 기준으로 다시 분석해줘", "CHZZK 기준으로 다시 분석해줘", "시청자 수 기준으로 분석해줘", "방송 수 기준으로 분석해줘");
        };
    }

    private String liveTrendLines(List<LiveTrendGameResponse> games) {
        if (games == null || games.isEmpty()) return "- 라이브 트렌드 데이터 없음";
        return games.stream()
                .limit(3)
                .map(game -> "- %s | 플랫폼: %s | 트렌드 점수: %.1f | 시청자: %,d명 | 이유: %s"
                        .formatted(game.title(), game.source(), game.trendScore(), game.totalViewerCount(), safe(game.reason())))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String reinterpretationLines(List<ReinterpretationCandidateResponse> candidates) {
        if (candidates == null || candidates.isEmpty()) return "- 재해석 후보 데이터 없음";
        return candidates.stream()
                .limit(3)
                .map(c -> "- %s | 재해석 방향: %s | 점수: %.1f | 이유: %s"
                        .formatted(c.title(), c.reinterpretationConcept(), c.reinterpretationScore(), safe(c.reason())))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "없음" : value.strip();
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
}
