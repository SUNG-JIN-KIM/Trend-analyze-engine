package com.gametrend.agent.report.service;

import com.gametrend.agent.game.entity.Game;
import com.gametrend.agent.game.repository.GameRepository;
import com.gametrend.agent.infrastructure.llm.LlmClient;
import com.gametrend.agent.infrastructure.llm.LlmCallException;
import com.gametrend.agent.report.dto.ReportRequest;
import com.gametrend.agent.report.dto.ReportResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceTest {

    @Test
    void generateDraft_returnsLlmResponseWhenLlmSucceeds() {
        CountingLlmClient llmClient = new CountingLlmClient("## 추천 이유 요약\n- GEMMA4 E2B 테스트 응답입니다.");
        ReportService reportService = new ReportService(repositoryWith(List.of(game("Chat Party", 91.0))), llmClient);

        ReportResponse response = reportService.generateDraft(null);

        assertEquals("## 추천 이유 요약\n- GEMMA4 E2B 테스트 응답입니다.", response.draft());
        assertEquals(1, llmClient.callCount());
        assertTrue(llmClient.lastSystemPrompt().contains("게임 시장 트렌드 분석가"));
        assertTrue(llmClient.lastUserPrompt().contains("추천 점수 기준 상위 1개"));
        assertTrue(llmClient.lastUserPrompt().contains("Chat Party"));
    }

    @Test
    void generateDraft_returnsFallbackWhenLlmThrowsException() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        ReportService reportService = new ReportService(repositoryWith(List.of(game("Camera Quest", 84.0))), llmClient);

        ReportResponse response = reportService.generateDraft(null);

        assertTrue(response.draft().contains("이 정적 초안은 등록된 게임의 마켓 시그널"));
        assertTrue(response.draft().contains("Camera Quest"));
        assertEquals(1, llmClient.callCount());
    }

    @Test
    void generateDraft_returnsFallbackWhenLlmResponseIsBlank() {
        CountingLlmClient llmClient = new CountingLlmClient("   ");
        ReportService reportService = new ReportService(repositoryWith(List.of(game("Voice Dungeon", 78.0))), llmClient);

        ReportResponse response = reportService.generateDraft(null);

        assertTrue(response.draft().contains("이 정적 초안은 등록된 게임의 마켓 시그널"));
        assertTrue(response.draft().contains("Voice Dungeon"));
        assertEquals(1, llmClient.callCount());
    }

    @Test
    void generateDraft_usesRecommendationLimitWhenRequestProvidesLimit() {
        CountingLlmClient llmClient = new CountingLlmClient("limit 테스트 응답");
        ReportService reportService = new ReportService(repositoryWith(List.of(
                game("Top Game", 95.0),
                game("Second Game", 88.0),
                game("Excluded Game", 77.0)
        )), llmClient);

        ReportResponse response = reportService.generateDraft(new ReportRequest(2));

        assertEquals("limit 테스트 응답", response.draft());
        assertEquals(1, llmClient.callCount());
        assertTrue(llmClient.lastUserPrompt().contains("추천 점수 기준 상위 2개"));
        assertTrue(llmClient.lastUserPrompt().contains("Top Game"));
        assertTrue(llmClient.lastUserPrompt().contains("Second Game"));
        assertFalse(llmClient.lastUserPrompt().contains("Excluded Game"));
    }

    @Test
    void generateDraft_doesNotCallLlmWhenThereIsNoGameData() {
        CountingLlmClient llmClient = new CountingLlmClient("호출되면 안 됩니다.");
        ReportService reportService = new ReportService(repositoryWith(List.of()), llmClient);

        ReportResponse response = reportService.generateDraft(null);

        assertEquals("아직 등록된 게임 데이터가 없어 추천 리포트 초안을 생성할 수 없습니다.", response.draft());
        assertEquals(0, llmClient.callCount());
        assertFalse(response.draft().contains("호출되면 안 됩니다."));
        assertNull(llmClient.lastSystemPrompt());
        assertNull(llmClient.lastUserPrompt());
    }

    private GameRepository repositoryWith(List<Game> games) {
        return (GameRepository) Proxy.newProxyInstance(
                GameRepository.class.getClassLoader(),
                new Class<?>[]{GameRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByOrderByRecommendationScoreDesc".equals(method.getName())) {
                        return games;
                    }
                    if ("toString".equals(method.getName())) {
                        return "FakeGameRepository";
                    }
                    throw new UnsupportedOperationException("테스트에서 사용하지 않는 메서드입니다: " + method.getName());
                }
        );
    }

    private Game game(String title, double recommendationScore) {
        return Game.builder()
                .id(1L)
                .title(title)
                .genre("Party")
                .platform("PC")
                .playStyle("Co-op")
                .streamabilityScore(90)
                .webcamFitScore(90)
                .ttsFitScore(90)
                .sttFitScore(90)
                .noveltyScore(90)
                .devFeasibilityScore(90)
                .marketSignalScore(90)
                .recommendationScore(recommendationScore)
                .reason("테스트 추천 근거")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private static class CountingLlmClient implements LlmClient {

        private final String response;
        private final RuntimeException exception;
        private int callCount;
        private String lastSystemPrompt;
        private String lastUserPrompt;

        CountingLlmClient(String response) {
            this.response = response;
            this.exception = null;
        }

        CountingLlmClient(RuntimeException exception) {
            this.response = null;
            this.exception = exception;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            callCount++;
            lastSystemPrompt = systemPrompt;
            lastUserPrompt = userPrompt;
            if (exception != null) {
                throw exception;
            }
            return response;
        }

        int callCount() {
            return callCount;
        }

        String lastSystemPrompt() {
            return lastSystemPrompt;
        }

        String lastUserPrompt() {
            return lastUserPrompt;
        }
    }
}
