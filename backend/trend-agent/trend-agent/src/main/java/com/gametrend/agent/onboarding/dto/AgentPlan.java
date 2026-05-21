package com.gametrend.agent.onboarding.dto;

import java.util.List;

// ✅ genreFilter 필드 추가 - AgentPlanner가 "fps 게임 추천해줘"에서 "FPS"를 추출해 여기에 담음
public record AgentPlan(
        String intent,
        String userRole,
        String platformFilter,
        String genreFilter,        // ← 신규 추가: "FPS", "HORROR", "PARTY" 등, 없으면 null
        String sortMetric,
        String analysisPurpose,
        List<String> interactionFeatures,
        boolean needsLiveTrend,
        boolean needsReinterpretation,
        boolean needsGameRecommendation,
        boolean needsClarification,
        String referencedPreviousTopic,
        String resolvedTopic,
        String answerStyle,
        double confidence,
        String reasoningSummary,
        String responseDepth
) {
}