package com.gametrend.agent.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.infrastructure.llm.LlmClient;
import com.gametrend.agent.onboarding.dto.AgentPlan;
import com.gametrend.agent.onboarding.dto.AgentPlanningContext;
import com.gametrend.agent.onboarding.dto.ConversationMemorySummaryResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPlannerTest {

    private final AgentQueryConditionResolver fallbackResolver = new AgentQueryConditionResolver(
            new PlatformFilterResolver(),
            new NonGameCategoryFilter()
    );

    @Test
    void plan_parsesLlmJsonPlan() {
        AgentPlanner planner = new AgentPlanner(
                new StaticLlmClient("""
                        {
                          "intent": "DEVELOPER_MARKET_ANALYSIS",
                          "userRole": "DEVELOPER",
                          "platformFilter": "ALL",
                          "sortMetric": "MARKET_SIGNAL",
                          "analysisPurpose": "DEVELOPER_MARKET_ANALYSIS",
                          "interactionFeatures": [],
                          "needsLiveTrend": true,
                          "needsReinterpretation": false,
                          "needsGameRecommendation": false,
                          "needsClarification": false,
                          "referencedPreviousTopic": "",
                          "resolvedTopic": "",
                          "answerStyle": "대화체",
                          "confidence": 0.91,
                          "reasoningSummary": "개발 가능성 질문"
                        }
                        """),
                new ObjectMapper(),
                fallbackResolver
        );

        AgentPlan plan = planner.plan("요즘 어떤 게임을 개발하면 좋을지 알려줘", List.of(), AgentPlanningContext.empty());

        assertEquals("DEVELOPER_MARKET_ANALYSIS", plan.intent());
        assertEquals("DEVELOPER", plan.userRole());
        assertTrue(plan.needsLiveTrend());
        assertEquals("NORMAL", plan.responseDepth());
        assertEquals(0.91, plan.confidence(), 0.001);
    }

    @Test
    void plan_parsesGenreFilterFromLlmJsonPlan() {
        AgentPlanner planner = new AgentPlanner(
                new StaticLlmClient("""
                        {
                          "intent": "USER_GAME_RECOMMENDATION",
                          "userRole": "PLAYER",
                          "platformFilter": "ALL",
                          "genreFilter": "horror",
                          "sortMetric": "TREND_SCORE",
                          "analysisPurpose": "USER_GAME_RECOMMENDATION",
                          "interactionFeatures": [],
                          "needsLiveTrend": true,
                          "needsReinterpretation": false,
                          "needsGameRecommendation": true,
                          "needsClarification": false,
                          "referencedPreviousTopic": "",
                          "resolvedTopic": "",
                          "answerStyle": "대화체",
                          "confidence": 0.86,
                          "reasoningSummary": "공포 게임 추천 요청"
                        }
                        """),
                new ObjectMapper(),
                fallbackResolver
        );

        AgentPlan plan = planner.plan("친구랑 할 공포 게임 추천해줘", List.of(), AgentPlanningContext.empty());

        assertEquals("USER_GAME_RECOMMENDATION", plan.intent());
        assertEquals("HORROR", plan.genreFilter());
    }

    @Test
    void plan_detectsGreetingBeforeCallingLlm() {
        CountingStaticLlmClient llmClient = new CountingStaticLlmClient("이 응답은 사용되면 안 됩니다.");
        AgentPlanner planner = new AgentPlanner(
                llmClient,
                new ObjectMapper(),
                fallbackResolver
        );

        AgentPlan plan = planner.plan("안녕", List.of(), AgentPlanningContext.empty());

        assertEquals("GREETING", plan.intent());
        assertEquals("GREETING", plan.analysisPurpose());
        assertEquals("SHORT", plan.responseDepth());
        assertTrue(!plan.needsLiveTrend());
        assertTrue(!plan.needsReinterpretation());
        assertTrue(!plan.needsGameRecommendation());
        assertEquals(0, llmClient.callCount());
    }

    @Test
    void plan_detectsHelpBeforeCallingLlm() {
        CountingStaticLlmClient llmClient = new CountingStaticLlmClient("이 응답은 사용되면 안 됩니다.");
        AgentPlanner planner = new AgentPlanner(
                llmClient,
                new ObjectMapper(),
                fallbackResolver
        );

        AgentPlan plan = planner.plan("뭐 할 수 있어?", List.of(), AgentPlanningContext.empty());

        assertEquals("HELP", plan.intent());
        assertEquals("HELP", plan.analysisPurpose());
        assertEquals("SHORT", plan.responseDepth());
        assertTrue(!plan.needsLiveTrend());
        assertEquals(0, llmClient.callCount());
    }

    @Test
    void plan_setsDetailedResponseDepthForDetailedQuestion() {
        AgentPlanner planner = new AgentPlanner(
                new StaticLlmClient("JSON이 아닌 응답"),
                new ObjectMapper(),
                fallbackResolver
        );

        AgentPlan plan = planner.plan("요즘 게임 트렌드 자세히 분석해줘", List.of(), AgentPlanningContext.empty());

        assertEquals("TREND_ANALYSIS", plan.intent());
        assertEquals("DETAILED", plan.responseDepth());
    }

    @Test
    void plan_fallbackExtractsGenreFilterWhenLlmReturnsInvalidJson() {
        AgentPlanner planner = new AgentPlanner(
                new StaticLlmClient("JSON이 아닌 응답"),
                new ObjectMapper(),
                fallbackResolver
        );

        AgentPlan plan = planner.plan("배그 같은 fps 게임 추천해줘", List.of(), AgentPlanningContext.empty());

        assertEquals("USER_GAME_RECOMMENDATION", plan.intent());
        assertEquals("FPS", plan.genreFilter());
    }

    @Test
    void plan_fallbackExtractsRpgGenreFilterWhenLlmReturnsInvalidJson() {
        AgentPlanner planner = new AgentPlanner(
                new StaticLlmClient("JSON이 아닌 응답"),
                new ObjectMapper(),
                fallbackResolver
        );

        AgentPlan plan = planner.plan("RPG 게임도 추천해줘", List.of(), AgentPlanningContext.empty());

        assertEquals("USER_GAME_RECOMMENDATION", plan.intent());
        assertEquals("RPG", plan.genreFilter());
    }

    @Test
    void plan_fallbackTreatsFriendCorrectionAsPartyRecommendation() {
        AgentPlanner planner = new AgentPlanner(
                new StaticLlmClient("JSON이 아닌 응답"),
                new ObjectMapper(),
                fallbackResolver
        );

        AgentPlan plan = planner.plan(
                "친구랑 한다고 했는데",
                List.of(),
                new AgentPlanningContext(
                        "나 그럼 친구랑 하기 좋은 게임 추천해줘",
                        "친구와 플레이할 게임 추천",
                        "리포트",
                        List.of("Dota 2", "League of Legends")
                )
        );

        assertEquals("USER_GAME_RECOMMENDATION", plan.intent());
        assertEquals("PLAYER", plan.userRole());
        assertEquals("PARTY", plan.genreFilter());
        assertTrue(plan.needsGameRecommendation());
    }

    @Test
    void plan_fallsBackToLatestUserIntentWhenLlmReturnsInvalidJson() {
        AgentPlanner planner = new AgentPlanner(
                new StaticLlmClient("JSON이 아닌 응답"),
                new ObjectMapper(),
                fallbackResolver
        );

        AgentPlan plan = planner.plan(
                "근데 내가 요즘 게임을 하고 싶은데 할만한 게임이 뭐가 있을까?",
                List.of(),
                new AgentPlanningContext(
                        "과거 게임 중 지금 다시 만들면 인기 있을 만한 게임 알려줘",
                        "과거 게임 재해석 요약",
                        "리포트",
                        List.of("Phasmophobia")
                )
        );

        assertEquals("USER_GAME_RECOMMENDATION", plan.intent());
        assertEquals("PLAYER", plan.userRole());
        assertTrue(plan.needsGameRecommendation());
    }

    @Test
    void plan_fallbackResolvesPreviousTopicReference() {
        AgentPlanner planner = new AgentPlanner(
                new StaticLlmClient(""),
                new ObjectMapper(),
                fallbackResolver
        );

        AgentPlan plan = planner.plan(
                "오 그거 재미있겠다. 네가 알려준 게임도 개발해볼 가치가 있을까?",
                List.of(),
                new AgentPlanningContext(
                        "요즘 할만한 게임 추천해줘",
                        "Counter-Strike 2를 추천했습니다.",
                        "리포트",
                        List.of("Counter-Strike 2")
                )
        );

        assertEquals("DEVELOPER_MARKET_ANALYSIS", plan.intent());
        assertEquals("Counter-Strike 2", plan.resolvedTopic());
    }

    @Test
    void plan_fallbackUsesSessionMemoryForPreviousTopicReference() {
        AgentPlanner planner = new AgentPlanner(
                new StaticLlmClient(""),
                new ObjectMapper(),
                fallbackResolver
        );
        ConversationMemorySummaryResponse memory = new ConversationMemorySummaryResponse(
                "session-1",
                "플레이할 게임 추천",
                "USER_GAME_RECOMMENDATION",
                "PLAYER",
                "CHZZK",
                "TREND_SCORE",
                List.of("Counter-Strike 2"),
                List.of("Counter-Strike 2"),
                List.of(),
                List.of("Phasmophobia"),
                List.of(),
                List.of("친구와 플레이"),
                List.of(),
                "이전에는 Counter-Strike 2를 플레이 추천 후보로 제안했습니다.",
                LocalDateTime.now()
        );

        AgentPlan plan = planner.plan(
                "오 그거 재미있겠다. 그 게임도 개발해볼 가치가 있어?",
                List.of(),
                new AgentPlanningContext(
                        null,
                        null,
                        null,
                        List.of(),
                        memory
                )
        );

        assertEquals("DEVELOPER_MARKET_ANALYSIS", plan.intent());
        assertEquals("Counter-Strike 2", plan.resolvedTopic());
    }

    private record StaticLlmClient(String response) implements LlmClient {
        @Override
        public String complete(String systemPrompt, String userPrompt) {
            return response;
        }
    }

    private static class CountingStaticLlmClient implements LlmClient {
        private final String response;
        private int callCount;

        CountingStaticLlmClient(String response) {
            this.response = response;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            callCount++;
            return response;
        }

        int callCount() {
            return callCount;
        }
    }
}
