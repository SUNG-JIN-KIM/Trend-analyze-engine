package com.gametrend.agent.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OnboardingAnalyzeRequest(
        @NotBlank @Size(max = 2000)
        String message,

        @Size(max = 100)
        String targetPlatform,

        @Size(max = 100)
        String teamSize,

        @Size(max = 10)
        List<@Size(max = 50) String> preferredFeatures,

        @Size(max = 100)
        String developmentPeriod,

        Long parentHistoryId,

        @Size(max = 100)
        String conversationId,

        @Size(max = 100)
        String sessionId,

        Long projectId
) {
    public OnboardingAnalyzeRequest(
            String message,
            String targetPlatform,
            String teamSize,
            List<String> preferredFeatures,
            String developmentPeriod
    ) {
        this(message, targetPlatform, teamSize, preferredFeatures, developmentPeriod, null, null, null, null);
    }

    public OnboardingAnalyzeRequest(
            String message,
            String targetPlatform,
            String teamSize,
            List<String> preferredFeatures,
            String developmentPeriod,
            Long parentHistoryId,
            String conversationId
    ) {
        this(message, targetPlatform, teamSize, preferredFeatures, developmentPeriod, parentHistoryId, conversationId, null, null);
    }

    public OnboardingAnalyzeRequest(
            String message,
            String targetPlatform,
            String teamSize,
            List<String> preferredFeatures,
            String developmentPeriod,
            Long parentHistoryId,
            String conversationId,
            String sessionId
    ) {
        this(message, targetPlatform, teamSize, preferredFeatures, developmentPeriod, parentHistoryId, conversationId, sessionId, null);
    }
}
