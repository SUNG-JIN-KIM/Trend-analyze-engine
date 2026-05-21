package com.gametrend.agent.report.service;

import com.gametrend.agent.game.entity.Game;
import com.gametrend.agent.game.repository.GameRepository;
import com.gametrend.agent.infrastructure.llm.LlmClient;
import com.gametrend.agent.report.dto.ReportRequest;
import com.gametrend.agent.report.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;

    private static final String SYSTEM_PROMPT = """
            당신은 게임 시장 트렌드 분석가입니다.
            제공되는 게임 후보 목록을 바탕으로 백엔드 포트폴리오에 어울리는 게임 트렌드 분석 보고서 초안을 작성합니다.
            답변은 반드시 한국어로만 작성합니다. 제목, 섹션명, 글머리표, 결론까지 모두 한국어를 사용합니다.
            보고서는 마크다운 헤더(##)와 글머리표(-)를 활용해 구조적으로 작성하고, 다음 다섯 영역을 반드시 포함합니다.
              1. 추천 이유 요약
              2. 시장성 및 마켓 시그널 해석
              3. 웹캠 / TTS / STT 인터랙션 활용 가능성
              4. 장르 적합성 평가
              5. 개발 방향성과 구현 범위 제안
            과장된 마케팅 표현은 피하고, 데이터에 근거해 간결하고 실무적인 톤으로 작성합니다.
            """;

    private final GameRepository gameRepository;
    private final LlmClient llmClient;

    public ReportResponse generateDraft(ReportRequest request) {
        int limit = resolveLimit(request);
        List<Game> recommendedGames = gameRepository.findAllByOrderByRecommendationScoreDesc()
                .stream()
                .limit(limit)
                .toList();

        String draft = buildDraftWithLlmOrFallback(recommendedGames);

        return new ReportResponse(
                "Game Trend Insight Draft",
                draft,
                LocalDateTime.now()
        );
    }

    private int resolveLimit(ReportRequest request) {
        if (request == null || request.recommendationLimit() == null) {
            return DEFAULT_RECOMMENDATION_LIMIT;
        }
        return request.recommendationLimit();
    }

    private String buildDraftWithLlmOrFallback(List<Game> games) {
        if (games.isEmpty()) {
            return buildStaticDraft(games);
        }
        try {
            String userPrompt = buildUserPrompt(games);
            String content = llmClient.complete(SYSTEM_PROMPT, userPrompt);
            if (content == null || content.isBlank()) {
                log.warn("LLM 응답이 비어 있어 정적 보고서로 대체합니다.");
                return buildStaticDraft(games);
            }
            return content.trim();
        } catch (RuntimeException ex) {
            log.warn("LLM 보고서 생성 실패, 정적 보고서로 대체합니다. cause={}", ex.toString());
            return buildStaticDraft(games);
        }
    }

    private String buildUserPrompt(List<Game> games) {
        String gameLines = games.stream()
                .map(this::formatGameLine)
                .collect(Collectors.joining(System.lineSeparator()));

        return """
                추천 점수 기준 상위 %d개 게임 후보 데이터입니다.

                %s

                위 후보들을 종합해 게임 트렌드 분석 보고서 초안을 작성해 주세요.
                각 후보 개별 분석보다 후보 전체에서 드러나는 패턴과 인사이트 위주로 정리해 주세요.
                """.formatted(games.size(), gameLines);
    }

    private String formatGameLine(Game game) {
        return ("- 제목=%s | 장르=%s | 플랫폼=%s | playStyle=%s"
                + " | streamability=%d | webcamFit=%d | ttsFit=%d | sttFit=%d"
                + " | novelty=%d | devFeasibility=%d | marketSignal=%d"
                + " | 추천점수=%.1f | 등록 reason=%s"
        ).formatted(
                game.getTitle(),
                game.getGenre(),
                game.getPlatform(),
                game.getPlayStyle(),
                game.getStreamabilityScore(),
                game.getWebcamFitScore(),
                game.getTtsFitScore(),
                game.getSttFitScore(),
                game.getNoveltyScore(),
                game.getDevFeasibilityScore(),
                game.getMarketSignalScore(),
                game.getRecommendationScore(),
                resolveReason(game)
        );
    }

    private String buildStaticDraft(List<Game> games) {
        if (games.isEmpty()) {
            return "아직 등록된 게임 데이터가 없어 추천 리포트 초안을 생성할 수 없습니다.";
        }

        String recommendations = games.stream()
                .map(game -> "- %s (%s, %s): 추천 점수 %.1f. %s".formatted(
                        game.getTitle(),
                        game.getGenre(),
                        game.getPlatform(),
                        game.getRecommendationScore(),
                        resolveReason(game)
                ))
                .collect(Collectors.joining(System.lineSeparator()));

        return """
                이 정적 초안은 등록된 게임의 마켓 시그널, 스트리밍 적합도, Webcam/TTS/STT 인터랙션 적합도를 바탕으로 생성되었습니다.

                추천 후보:
                %s

                Ollama 또는 LM Studio 기반 LLM 호출이 정상 동작하면 각 후보의 장르 포지셔닝, 인터랙션 아이디어, 포트폴리오 구현 범위를 더 구체적으로 확장할 수 있습니다.
                """.formatted(recommendations);
    }

    private String resolveReason(Game game) {
        if (game.getReason() == null || game.getReason().isBlank()) {
            return "인터랙션 적합도 점수와 마켓 시그널 점수를 추천 근거로 사용했습니다.";
        }
        return game.getReason();
    }
}
