package com.gametrend.agent.onboarding.service;

import com.gametrend.agent.onboarding.dto.AgentQueryConditionResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentQueryConditionResolverTest {

    private final AgentQueryConditionResolver resolver = new AgentQueryConditionResolver(
            new PlatformFilterResolver(),
            new NonGameCategoryFilter()
    );

    @Test
    void resolve_extractsPlatformSortMetricPurposeAndFeatures() {
        AgentQueryConditionResponse chzzkTrend = resolver.resolve("치지직 기준으로 요즘 인기 있는 게임 알려줘");
        assertEquals("CHZZK", chzzkTrend.platformFilter());
        assertEquals("TREND_SCORE", chzzkTrend.sortMetric());
        assertEquals("TREND_ANALYSIS", chzzkTrend.analysisPurpose());

        AgentQueryConditionResponse viewerSort = resolver.resolve("시청자 수 기준으로 알려줘");
        assertEquals("VIEWER_COUNT", viewerSort.sortMetric());

        AgentQueryConditionResponse streamSort = resolver.resolve("스트리머들이 많이 하는 게임 알려줘");
        assertEquals("STREAM_COUNT", streamSort.sortMetric());
        assertEquals("STREAMING_FIT_ANALYSIS", streamSort.analysisPurpose());

        AgentQueryConditionResponse developer = resolver.resolve("게임 개발자가 참고할 만한 장르 알려줘");
        assertEquals("MARKET_SIGNAL", developer.sortMetric());
        assertEquals("DEVELOPER_MARKET_ANALYSIS", developer.analysisPurpose());

        AgentQueryConditionResponse interaction = resolver.resolve("웹캠 TTS STT로 만들만한 게임 알려줘");
        assertEquals("INTERACTION_GAME_IDEA", interaction.analysisPurpose());
        assertEquals(List.of("WEBCAM", "TTS", "STT"), interaction.interactionFeatures());
    }

    @Test
    void resolve_prioritizesGameReinterpretationPurpose() {
        AgentQueryConditionResponse legacy = resolver.resolve("과거 게임 중 지금 다시 만들면 인기 있을 만한 게임 알려줘");
        assertEquals("GAME_REINTERPRETATION", legacy.analysisPurpose());

        AgentQueryConditionResponse interactionLegacy = resolver.resolve("웹캠 TTS STT로 재해석할 만한 예전 게임 알려줘");
        assertEquals("GAME_REINTERPRETATION", interactionLegacy.analysisPurpose());
        assertEquals(List.of("WEBCAM", "TTS", "STT"), interactionLegacy.interactionFeatures());

        AgentQueryConditionResponse developer = resolver.resolve("게임 개발자가 참고할 만한 장르 알려줘");
        assertEquals("DEVELOPER_MARKET_ANALYSIS", developer.analysisPurpose());
    }

    @Test
    void resolve_prioritizesLatestUserRecommendationOverPreviousPurpose() {
        AgentQueryConditionResponse fromLegacy = resolver.resolveFollowUp(
                "요즘 할만한 게임 추천해줘",
                List.of(),
                "과거 게임 중 지금 다시 만들면 인기 있을 만한 게임 알려줘",
                List.of()
        );
        assertEquals("USER_GAME_RECOMMENDATION", fromLegacy.analysisPurpose());

        AgentQueryConditionResponse fromDeveloper = resolver.resolveFollowUp(
                "그리고 내가 요즘 할만 게임이 없는데 뭐가 좋을까",
                List.of(),
                "요즘 어떤 게임을 개발하면 좋을지 알려줘",
                List.of()
        );
        assertEquals("USER_GAME_RECOMMENDATION", fromDeveloper.analysisPurpose());
    }

    @Test
    void resolve_inheritsParentPurposeOnlyForAmbiguousFollowUp() {
        AgentQueryConditionResponse platformOnlyFollowUp = resolver.resolveFollowUp(
                "그럼 치지직 기준으로 알려줘",
                List.of(),
                "요즘 할만한 게임 추천해줘",
                List.of()
        );
        assertEquals("USER_GAME_RECOMMENDATION", platformOnlyFollowUp.analysisPurpose());
        assertEquals("CHZZK", platformOnlyFollowUp.platformFilter());

        AgentQueryConditionResponse reinterpretationFollowUp = resolver.resolveFollowUp(
                "그걸 웹캠 TTS STT로 재해석하면?",
                List.of(),
                "요즘 인기 게임 알려줘",
                List.of()
        );
        assertEquals("GAME_REINTERPRETATION", reinterpretationFollowUp.analysisPurpose());
        assertEquals(List.of("WEBCAM", "TTS", "STT"), reinterpretationFollowUp.interactionFeatures());
    }

    @Test
    void resolve_treatsFriendCorrectionAsGameRecommendation() {
        AgentQueryConditionResponse correction = resolver.resolveFollowUp(
                "친구랑 한다고 했는데",
                List.of(),
                "나 그럼 친구랑 하기 좋은 게임 추천해줘",
                List.of()
        );

        assertEquals("USER_GAME_RECOMMENDATION", correction.analysisPurpose());
    }

    @Test
    void resolve_marksNonGameCategoryExclusionUnlessExplicitlyRequested() {
        AgentQueryConditionResponse gameTrend = resolver.resolve("요즘 어떤 게임이 인기 있어?");
        assertTrue(gameTrend.excludeNonGameCategories());

        AgentQueryConditionResponse categoryTrend = resolver.resolve("Just Chatting 같은 잡담 카테고리도 알려줘");
        assertEquals(false, categoryTrend.excludeNonGameCategories());
    }
}
