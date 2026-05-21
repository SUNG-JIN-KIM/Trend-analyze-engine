package com.gametrend.agent.onboarding.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OnboardingHistoryItemResponse(
        Long id,
        Long projectId,
        Long parentHistoryId,
        String conversationId,
        String message,
        String targetPlatform,
        String teamSize,
        List<String> preferredFeatures,
        String developmentPeriod,
        String summary,
        int recommendedConceptCount,
        LocalDateTime createdAt
) {
}
