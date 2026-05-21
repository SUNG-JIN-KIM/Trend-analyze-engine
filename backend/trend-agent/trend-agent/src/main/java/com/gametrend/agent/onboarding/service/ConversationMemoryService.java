package com.gametrend.agent.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.infrastructure.llm.LlmClient;
import com.gametrend.agent.onboarding.dto.AgentPlan;
import com.gametrend.agent.onboarding.dto.ConversationMemorySummaryResponse;
import com.gametrend.agent.onboarding.dto.ConversationMemoryUpdateContext;
import com.gametrend.agent.onboarding.dto.EvidenceCardResponse;
import com.gametrend.agent.onboarding.entity.ConversationMemorySummary;
import com.gametrend.agent.onboarding.repository.ConversationMemorySummaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class ConversationMemoryService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final int MAX_LIST_SIZE = 12;

    private final ConversationMemorySummaryRepository repository;
    private final ObjectMapper objectMapper;
    private final LlmClient llmClient;

    public ConversationMemoryService(
            ConversationMemorySummaryRepository repository,
            ObjectMapper objectMapper,
            LlmClient llmClient
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.llmClient = llmClient;
    }

    public String resolveSessionId(String requestedSessionId) {
        if (requestedSessionId != null && !requestedSessionId.isBlank()) {
            return requestedSessionId.strip();
        }
        return "agent-session-" + UUID.randomUUID();
    }

    public ConversationMemorySummaryResponse findOrCreate(String sessionId) {
        ConversationMemorySummary memory = repository.findBySessionId(sessionId)
                .orElseGet(() -> repository.save(newMemory(sessionId, null)));
        return toResponse(memory);
    }

    public ConversationMemorySummaryResponse findOrCreate(Long conversationId, String sessionId) {
        if (conversationId == null) {
            return findOrCreate(sessionId);
        }
        ConversationMemorySummary memory = repository.findByConversationId(conversationId)
                .orElseGet(() -> repository.findBySessionId(sessionId)
                        .orElseGet(() -> repository.save(newMemory(sessionId, conversationId))));
        if (memory.getConversationId() == null) {
            memory = repository.save(memory.toBuilder()
                    .conversationId(conversationId)
                    .build());
        }
        return toResponse(memory);
    }

    public ConversationMemorySummaryResponse update(ConversationMemoryUpdateContext context) {
        String sessionId = resolveSessionId(context.sessionId());
        ConversationMemorySummary previous = context.conversationId() == null
                ? repository.findBySessionId(sessionId).orElseGet(() -> newMemory(sessionId, null))
                : repository.findByConversationId(context.conversationId())
                .orElseGet(() -> repository.findBySessionId(sessionId)
                        .orElseGet(() -> newMemory(sessionId, context.conversationId())));
        LocalDateTime now = LocalDateTime.now();
        AgentPlan plan = context.agentPlan();

        List<String> evidenceTitles = titlesFromEvidence(context.evidenceCards());
        List<String> recommendedGames = merge(
                previous.getRecommendedGamesJson(),
                evidenceTitlesByType(context.evidenceCards(), "LIVE_TREND", "GAME_RECOMMENDATION")
        );
        List<String> reinterpretationCandidates = merge(
                previous.getReinterpretationCandidatesJson(),
                evidenceTitlesByType(context.evidenceCards(), "REINTERPRETATION")
        );
        List<String> developerCandidates = merge(
                previous.getDeveloperCandidatesJson(),
                "DEVELOPER_MARKET_ANALYSIS".equals(plan.analysisPurpose()) ? evidenceTitles : List.of()
        );
        List<String> mentionedGames = merge(
                previous.getMentionedGamesJson(),
                mergeRaw(evidenceTitles, plan.resolvedTopic() == null || plan.resolvedTopic().isBlank()
                        ? List.of()
                        : List.of(plan.resolvedTopic()))
        );
        List<String> interactionFeatures = merge(
                previous.getInteractionFeaturesJson(),
                plan.interactionFeatures() == null ? List.of() : plan.interactionFeatures()
        );
        List<String> constraints = merge(previous.getConstraintsJson(), constraintsFromMessage(context.userMessage()));
        List<String> excluded = readList(previous.getExcludedJson());
        String summaryText = summarize(previous, context, recommendedGames, developerCandidates, reinterpretationCandidates);

        ConversationMemorySummary updated = ConversationMemorySummary.builder()
                .id(previous.getId())
                .sessionId(sessionId)
                .conversationId(context.conversationId() == null ? previous.getConversationId() : context.conversationId())
                .currentUserGoal(currentUserGoal(context.userMessage(), plan))
                .lastIntent(plan.intent())
                .lastUserRole(plan.userRole())
                .preferredPlatform(resolvePreferred(plan.platformFilter(), previous.getPreferredPlatform(), "ALL"))
                .preferredSortMetric(resolvePreferred(plan.sortMetric(), previous.getPreferredSortMetric(), "TREND_SCORE"))
                .mentionedGamesJson(writeList(mentionedGames))
                .recommendedGamesJson(writeList(recommendedGames))
                .developerCandidatesJson(writeList(developerCandidates))
                .reinterpretationCandidatesJson(writeList(reinterpretationCandidates))
                .interactionFeaturesJson(writeList(interactionFeatures))
                .constraintsJson(writeList(constraints))
                .excludedJson(writeList(excluded))
                .summaryText(summaryText)
                .createdAt(previous.getCreatedAt() == null ? now : previous.getCreatedAt())
                .updatedAt(now)
                .build();
        return toResponse(repository.save(updated));
    }

    private ConversationMemorySummary newMemory(String sessionId, Long conversationId) {
        LocalDateTime now = LocalDateTime.now();
        return ConversationMemorySummary.builder()
                .sessionId(sessionId)
                .conversationId(conversationId)
                .currentUserGoal(null)
                .lastIntent(null)
                .lastUserRole(null)
                .preferredPlatform(null)
                .preferredSortMetric(null)
                .mentionedGamesJson("[]")
                .recommendedGamesJson("[]")
                .developerCandidatesJson("[]")
                .reinterpretationCandidatesJson("[]")
                .interactionFeaturesJson("[]")
                .constraintsJson("[]")
                .excludedJson("[]")
                .summaryText(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private ConversationMemorySummaryResponse toResponse(ConversationMemorySummary memory) {
        return new ConversationMemorySummaryResponse(
                memory.getSessionId(),
                memory.getCurrentUserGoal(),
                memory.getLastIntent(),
                memory.getLastUserRole(),
                memory.getPreferredPlatform(),
                memory.getPreferredSortMetric(),
                readList(memory.getMentionedGamesJson()),
                readList(memory.getRecommendedGamesJson()),
                readList(memory.getDeveloperCandidatesJson()),
                readList(memory.getReinterpretationCandidatesJson()),
                readList(memory.getInteractionFeaturesJson()),
                readList(memory.getConstraintsJson()),
                readList(memory.getExcludedJson()),
                memory.getSummaryText(),
                memory.getUpdatedAt(),
                memory.getConversationId()
        );
    }

    private String summarize(
            ConversationMemorySummary previous,
            ConversationMemoryUpdateContext context,
            List<String> recommendedGames,
            List<String> developerCandidates,
            List<String> reinterpretationCandidates
    ) {
        String fallback = fallbackSummary(previous, context, recommendedGames, developerCandidates, reinterpretationCandidates);
        try {
            String prompt = """
                    아래 Agent 대화 메모리를 한국어 3~5문장으로 짧게 갱신해 주세요.
                    비밀값이나 토큰은 절대 포함하지 마세요.

                    이전 요약:
                    %s

                    최신 질문:
                    %s

                    최신 intent/userRole:
                    %s / %s

                    추천 게임:
                    %s

                    개발 후보:
                    %s

                    재해석 후보:
                    %s

                    최신 답변 요약:
                    %s
                    """.formatted(
                    safe(previous.getSummaryText()),
                    safe(context.userMessage()),
                    context.agentPlan().intent(),
                    context.agentPlan().userRole(),
                    recommendedGames,
                    developerCandidates,
                    reinterpretationCandidates,
                    safe(context.answerSummary())
            );
            String response = llmClient.complete("당신은 대화 메모리 요약기입니다. 한국어 요약문만 반환합니다.", prompt);
            if (response == null || response.isBlank()) {
                return fallback;
            }
            return truncate(response.strip(), 800);
        } catch (RuntimeException ex) {
            log.warn("ConversationMemorySummary LLM 요약 실패. 규칙 기반 요약을 사용합니다. cause={}", ex.toString());
            return fallback;
        }
    }

    private String fallbackSummary(
            ConversationMemorySummary previous,
            ConversationMemoryUpdateContext context,
            List<String> recommendedGames,
            List<String> developerCandidates,
            List<String> reinterpretationCandidates
    ) {
        List<String> sentences = new ArrayList<>();
        if (previous.getSummaryText() != null && !previous.getSummaryText().isBlank()) {
            sentences.add(previous.getSummaryText().strip());
        }
        sentences.add("최근 사용자의 목적은 %s이며, 마지막 intent는 %s입니다."
                .formatted(currentUserGoal(context.userMessage(), context.agentPlan()), context.agentPlan().intent()));
        if (!recommendedGames.isEmpty()) {
            sentences.add("최근 추천 게임 후보는 %s입니다.".formatted(String.join(", ", recommendedGames.stream().limit(3).toList())));
        }
        if (!developerCandidates.isEmpty()) {
            sentences.add("개발 관점 후보는 %s입니다.".formatted(String.join(", ", developerCandidates.stream().limit(3).toList())));
        }
        if (!reinterpretationCandidates.isEmpty()) {
            sentences.add("재해석 후보는 %s입니다.".formatted(String.join(", ", reinterpretationCandidates.stream().limit(3).toList())));
        }
        return truncate(sentences.stream().limit(5).reduce((left, right) -> left + " " + right).orElse("아직 대화 메모리가 충분하지 않습니다."), 800);
    }

    private String currentUserGoal(String message, AgentPlan plan) {
        String goal = switch (plan.analysisPurpose()) {
            case "GREETING" -> "인사";
            case "HELP" -> "도움말 확인";
            case "SMALL_TALK" -> "일상 대화";
            case "OUT_OF_SCOPE" -> "게임 분석 범위 안내";
            case "USER_GAME_RECOMMENDATION" -> "플레이할 게임 추천";
            case "GAME_REINTERPRETATION" -> "과거 게임 재해석 후보 탐색";
            case "DEVELOPER_MARKET_ANALYSIS" -> "개발할 만한 게임/장르 분석";
            case "STREAMING_FIT_ANALYSIS" -> "방송 적합성 분석";
            case "INTERACTION_GAME_IDEA" -> "Webcam/TTS/STT 인터랙션 아이디어";
            default -> "게임 트렌드 분석";
        };
        return message == null || message.isBlank() ? goal : goal + " - " + truncate(message.strip(), 120);
    }

    private List<String> titlesFromEvidence(List<EvidenceCardResponse> evidenceCards) {
        if (evidenceCards == null) {
            return List.of();
        }
        return evidenceCards.stream()
                .map(EvidenceCardResponse::title)
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .toList();
    }

    private List<String> evidenceTitlesByType(List<EvidenceCardResponse> evidenceCards, String... types) {
        if (evidenceCards == null || evidenceCards.isEmpty()) {
            return List.of();
        }
        return evidenceCards.stream()
                .filter(card -> hasEvidenceType(card, types))
                .map(EvidenceCardResponse::title)
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .toList();
    }

    private boolean hasEvidenceType(EvidenceCardResponse card, String... types) {
        String evidenceType = normalize(firstNonBlank(card.evidenceType(), card.type()));
        for (String type : types) {
            if (evidenceType.contains(normalize(type))) {
                return true;
            }
        }
        return false;
    }

    private List<String> constraintsFromMessage(String message) {
        String normalized = normalize(message);
        List<String> constraints = new ArrayList<>();
        if (containsAny(normalized, "혼자", "1인", "개인")) {
            constraints.add("1인/혼자");
        }
        if (containsAny(normalized, "소규모", "작은 팀", "인디")) {
            constraints.add("소규모 팀");
        }
        if (containsAny(normalized, "친구랑", "친구와", "같이")) {
            constraints.add("친구와 플레이");
        }
        if (containsAny(normalized, "가볍게", "짧게")) {
            constraints.add("가볍고 짧은 플레이");
        }
        return constraints;
    }

    private List<String> merge(String previousJson, List<String> additions) {
        return mergeRaw(readList(previousJson), additions);
    }

    private List<String> mergeRaw(List<String> previous, List<String> additions) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (additions != null) {
            additions.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::strip)
                    .forEach(values::add);
        }
        if (previous != null) {
            previous.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::strip)
                    .forEach(values::add);
        }
        return values.stream().limit(MAX_LIST_SIZE).toList();
    }

    private String resolvePreferred(String current, String previous, String ignoredValue) {
        if (current != null && !current.isBlank() && !current.equalsIgnoreCase(ignoredValue)) {
            return current.strip().toUpperCase(Locale.ROOT);
        }
        if (previous != null && !previous.isBlank()) {
            return previous.strip();
        }
        return current == null ? null : current.strip().toUpperCase(Locale.ROOT);
    }

    private List<String> readList(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            log.warn("Conversation memory JSON 파싱 실패. 빈 목록으로 대체합니다. cause={}", ex.toString());
            return List.of();
        }
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Conversation memory JSON 직렬화에 실패했습니다.", ex);
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).strip();
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "없음" : value.strip();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
