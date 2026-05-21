package com.gametrend.agent.onboarding.dto;

import java.util.List;

public record AgentQueryConditionResponse(
        String platformFilter,
        String sortMetric,
        String analysisPurpose,
        List<String> interactionFeatures,
        boolean excludeNonGameCategories,
        String originalMessage
) {
}
