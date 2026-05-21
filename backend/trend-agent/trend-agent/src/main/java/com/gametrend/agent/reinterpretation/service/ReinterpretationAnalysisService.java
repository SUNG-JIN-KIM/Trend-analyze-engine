package com.gametrend.agent.reinterpretation.service;

import com.gametrend.agent.gameimage.GameImageResolver;
import com.gametrend.agent.infrastructure.llm.LlmClient;
import com.gametrend.agent.onboarding.dto.AgentQueryConditionResponse;
import com.gametrend.agent.onboarding.dto.EvidenceCardResponse;
import com.gametrend.agent.onboarding.service.AgentQueryConditionResolver;
import com.gametrend.agent.reinterpretation.dto.ReinterpretationAnalyzeRequest;
import com.gametrend.agent.reinterpretation.dto.ReinterpretationAnalyzeResponse;
import com.gametrend.agent.reinterpretation.dto.ReinterpretationCandidateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReinterpretationAnalysisService {

    private static final String SYSTEM_PROMPT = """
            당신은 GEMMA4 E2B 기반 Game Reinterpretation Insight Agent입니다.
            과거 게임의 성공 요인, Steam 리뷰 신호, 현재 라이브 트렌드, Webcam/TTS/STT 재해석 가능성을
            개발자 관점에서 한국어로 분석합니다.
            Claude, Anthropic, OpenAI SDK 없이 현재 전달된 후보 데이터만 근거로 사용합니다.
            """;

    private final ReinterpretationCandidateService candidateService;
    private final AgentQueryConditionResolver queryConditionResolver;
    private final LlmClient llmClient;

    public ReinterpretationAnalysisService(
            ReinterpretationCandidateService candidateService,
            AgentQueryConditionResolver queryConditionResolver,
            LlmClient llmClient
    ) {
        this.candidateService = candidateService;
        this.queryConditionResolver = queryConditionResolver;
        this.llmClient = llmClient;
    }

    public ReinterpretationAnalyzeResponse analyze(ReinterpretationAnalyzeRequest request) {
        String message = request == null ? "" : request.message();
        List<String> features = request == null ? List.of() : request.preferredInteractionFeatures();
        int limit = request == null || request.limit() == null ? 5 : Math.max(1, request.limit());
        String conditionMessage = request == null || request.targetPlatform() == null || request.targetPlatform().isBlank()
                ? message
                : "%s %s".formatted(message, request.targetPlatform());
        AgentQueryConditionResponse condition = queryConditionResolver.resolve(conditionMessage, features);
        List<ReinterpretationCandidateResponse> candidates = candidateService.findCandidates(condition, limit);
        List<EvidenceCardResponse> evidenceCards = toEvidenceCards(candidates);
        String summary = buildSummary(condition, candidates);
        String answer = buildAnswer(condition, candidates);
        String report = buildReportWithLlmOrFallback(message, condition, candidates, summary, answer);

        return new ReinterpretationAnalyzeResponse(
                summary,
                answer,
                report,
                condition,
                candidates,
                evidenceCards
        );
    }

    private String buildReportWithLlmOrFallback(
            String message,
            AgentQueryConditionResponse condition,
            List<ReinterpretationCandidateResponse> candidates,
            String summary,
            String answer
    ) {
        String prompt = """
                사용자 질문:
                %s

                queryCondition:
                - platformFilter=%s
                - sortMetric=%s
                - analysisPurpose=%s
                - interactionFeatures=%s

                후보:
                %s

                초안 요약:
                %s

                초안 답변:
                %s

                위 후보를 바탕으로 개발자에게 왜 지금 다시 재해석하면 가능성이 있는지 설명해 주세요.
                Steam 리뷰 신호, 과거 메커니즘, 현재 트렌드 적합도, Webcam/TTS/STT 적용 아이디어를 함께 다뤄 주세요.
                """.formatted(
                message,
                condition.platformFilter(),
                condition.sortMetric(),
                condition.analysisPurpose(),
                String.join(", ", condition.interactionFeatures()),
                candidateLines(candidates),
                summary,
                answer
        );
        try {
            String response = llmClient.complete(SYSTEM_PROMPT, prompt);
            if (response == null || response.isBlank()) {
                return fallbackReport(summary, answer, candidates);
            }
            return response.trim();
        } catch (RuntimeException ex) {
            log.warn("재해석 인사이트 LLM 분석 실패. fallback을 반환합니다. cause={}", ex.toString());
            return fallbackReport(summary, answer, candidates);
        }
    }

    private String buildSummary(AgentQueryConditionResponse condition, List<ReinterpretationCandidateResponse> candidates) {
        if (candidates.isEmpty()) {
            return "아직 재해석 후보 데이터가 부족합니다. /api/legacy-games/refresh를 먼저 실행하면 seed와 Steam 리뷰 신호를 갱신할 수 있습니다.";
        }
        ReinterpretationCandidateResponse top = candidates.get(0);
        return "%s 기준으로 과거 게임 재해석 후보를 보면 %s가 %.1f점으로 가장 높습니다."
                .formatted(condition.sortMetric(), top.title(), top.reinterpretationScore());
    }

    private String buildAnswer(AgentQueryConditionResponse condition, List<ReinterpretationCandidateResponse> candidates) {
        if (candidates.isEmpty()) {
            return "fallback seed가 아직 준비되지 않았습니다. refresh 후 다시 분석해 주세요.";
        }
        ReinterpretationCandidateResponse top = candidates.get(0);
        return "%s는 %s로 재해석할 수 있습니다. 과거 핵심 메커니즘은 %s이고, 현재 관점에서는 interactionFitScore %d점과 modernTrendFitScore %d점이 강점입니다."
                .formatted(
                        top.title(),
                        top.reinterpretationConcept(),
                        String.join(", ", top.mechanics()),
                        top.interactionFitScore(),
                        top.modernTrendFitScore()
                );
    }

    private String fallbackReport(
            String summary,
            String answer,
            List<ReinterpretationCandidateResponse> candidates
    ) {
        return """
                ## 분석 요약
                %s

                ## 개발자 관점 답변
                %s

                ## 후보 근거
                %s

                ## 참고
                이 리포트는 LLM 호출 실패 또는 빈 응답에 대비한 fallback 분석 결과입니다.
                """.formatted(summary, answer, candidateLines(candidates));
    }

    private String candidateLines(List<ReinterpretationCandidateResponse> candidates) {
        if (candidates.isEmpty()) {
            return "- 후보 없음";
        }
        return candidates.stream()
                .map(candidate -> "- %s | reinterpretationScore=%.1f | concept=%s | reviewCount=%,d | positiveRate=%.2f | reason=%s"
                        .formatted(
                                candidate.title(),
                                candidate.reinterpretationScore(),
                                candidate.reinterpretationConcept(),
                                candidate.reviewCount(),
                                candidate.positiveReviewRate(),
                                candidate.reason()
                        ))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private List<EvidenceCardResponse> toEvidenceCards(List<ReinterpretationCandidateResponse> candidates) {
        return candidates.stream()
                .limit(3)
                .map(candidate -> new EvidenceCardResponse(
                        candidate.title(),
                        "REINTERPRETATION_CANDIDATE",
                        candidate.reinterpretationConcept(),
                        candidate.reinterpretationScore(),
                        null,
                        null,
                        null,
                        candidate.streamabilityScore(),
                        candidate.modernTrendFitScore(),
                        candidate.reason(),
                        candidate.source(),
                        String.join(", ", candidate.genres()),
                        null,
                        null,
                        null,
                        candidate.dataOrigin(),
                        "REINTERPRETATION",
                        "REINTERPRETATION_CANDIDATE",
                        String.join(", ", candidate.genres()),
                        candidate.reinterpretationConcept(),
                        candidate.reinterpretationScore(),
                        candidate.legacyPopularityScore(),
                        candidate.reviewSentimentScore(),
                        candidate.mechanicUniquenessScore(),
                        candidate.interactionFitScore(),
                        candidate.modernTrendFitScore(),
                        candidate.devFeasibilityScore(),
                        GameImageResolver.resolveImageUrl(candidate.title(), null, candidate.steamAppId())
                ))
                .toList();
    }
}
