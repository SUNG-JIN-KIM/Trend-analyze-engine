package com.gametrend.agent.onboarding.service;

import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.livetrend.service.LiveTrendService;
import com.gametrend.agent.onboarding.dto.AgentEvidenceBundle;
import com.gametrend.agent.onboarding.dto.AgentPlan;
import com.gametrend.agent.onboarding.dto.AgentQueryConditionResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentEvidenceCollectorTest {

    @Test
    void collect_filtersPartyRecommendationWithoutFallingBackToGenericTopGames() {
        AgentEvidenceCollector collector = new AgentEvidenceCollector(
                new StubLiveTrendService(List.of(
                        liveGame(1L, "TWITCH", "Dota 2", "MOBA", 69.8),
                        liveGame(2L, "TWITCH", "Slots", "Slots", 63.2),
                        liveGame(3L, "TWITCH", "Lethal Company", "Co-op Horror", 58.4)
                )),
                null,
                new NonGameCategoryFilter()
        );
        AgentPlan plan = new AgentPlan(
                "USER_GAME_RECOMMENDATION",
                "PLAYER",
                "ALL",
                "PARTY",
                "TREND_SCORE",
                "USER_GAME_RECOMMENDATION",
                List.of(),
                true,
                false,
                true,
                false,
                "",
                "",
                "대화체",
                0.8,
                "친구와 플레이 조건",
                "NORMAL"
        );
        AgentQueryConditionResponse queryCondition = new AgentQueryConditionResponse(
                "ALL",
                "TREND_SCORE",
                "USER_GAME_RECOMMENDATION",
                List.of(),
                true,
                "친구랑 한다고 했는데"
        );

        AgentEvidenceBundle evidence = collector.collect(plan, queryCondition, "친구랑 한다고 했는데");

        assertEquals(List.of("Lethal Company"), evidence.liveTrendGames().stream()
                .map(LiveTrendGameResponse::title)
                .toList());
        assertEquals(List.of("Lethal Company"), evidence.evidenceCards().stream()
                .map(card -> card.title())
                .toList());
    }

    @Test
    void collect_soloFpsRecommendationDoesNotUseCompetitiveMultiplayerLiveTrend() {
        AgentEvidenceCollector collector = new AgentEvidenceCollector(
                new StubLiveTrendService(List.of(
                        liveGame(1L, "TWITCH", "VALORANT", "FPS", 91.0),
                        liveGame(2L, "TWITCH", "Counter-Strike 2", "Tactical FPS", 89.0)
                )),
                null,
                new NonGameCategoryFilter()
        );
        AgentPlan plan = new AgentPlan(
                "USER_GAME_RECOMMENDATION",
                "PLAYER",
                "ALL",
                "FPS",
                "TREND_SCORE",
                "USER_GAME_RECOMMENDATION",
                List.of(),
                true,
                false,
                true,
                false,
                "",
                "",
                "대화체",
                0.8,
                "혼자 할 FPS 추천",
                "NORMAL"
        );
        AgentQueryConditionResponse queryCondition = new AgentQueryConditionResponse(
                "ALL",
                "TREND_SCORE",
                "USER_GAME_RECOMMENDATION",
                List.of(),
                true,
                "fps 게임으로 추천해줘 혼자 할거야"
        );

        AgentEvidenceBundle evidence = collector.collect(plan, queryCondition, "fps 게임으로 추천해줘 혼자 할거야");

        assertEquals(List.of(), evidence.liveTrendGames());
        assertEquals(List.of(), evidence.evidenceCards());
    }

    @Test
    void collect_gameRecommendationDoesNotCreateGenericClarificationEvidenceCard() {
        AgentEvidenceCollector collector = new AgentEvidenceCollector(
                new StubLiveTrendService(List.of()),
                null,
                new NonGameCategoryFilter()
        );
        AgentPlan plan = new AgentPlan(
                "USER_GAME_RECOMMENDATION",
                "PLAYER",
                "ALL",
                "RPG",
                "TREND_SCORE",
                "USER_GAME_RECOMMENDATION",
                List.of(),
                true,
                false,
                true,
                true,
                "",
                "",
                "대화체",
                0.8,
                "RPG 추천 요청이지만 추가 확인도 필요한 상태",
                "NORMAL"
        );
        AgentQueryConditionResponse queryCondition = new AgentQueryConditionResponse(
                "ALL",
                "TREND_SCORE",
                "USER_GAME_RECOMMENDATION",
                List.of(),
                true,
                "RPG도 혼자 할만한 게임 추천해줘"
        );

        AgentEvidenceBundle evidence = collector.collect(plan, queryCondition, "RPG도 혼자 할만한 게임 추천해줘");

        assertEquals(List.of(), evidence.liveTrendGames());
        assertEquals(List.of(), evidence.evidenceCards());
    }

    private static LiveTrendGameResponse liveGame(
            Long id,
            String source,
            String title,
            String genre,
            double trendScore
    ) {
        return new LiveTrendGameResponse(
                id,
                source,
                title,
                genre,
                "PC",
                title,
                10,
                1_000,
                70,
                60,
                75,
                70,
                trendScore,
                "COMPLETE",
                "REAL",
                "테스트용 라이브 트렌드",
                LocalDateTime.now()
        );
    }

    private static class StubLiveTrendService extends LiveTrendService {
        private final List<LiveTrendGameResponse> games;

        StubLiveTrendService(List<LiveTrendGameResponse> games) {
            super(null, null, null, null, List.of());
            this.games = games;
        }

        @Override
        public List<LiveTrendGameResponse> findTopLiveTrendGames(int limit, String platform) {
            return games.stream()
                    .limit(limit)
                    .toList();
        }
    }
}
