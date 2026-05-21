package com.gametrend.agent.onboarding.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationMemorySummaryResponse(
        String sessionId,
        String currentUserGoal,
        String lastIntent,
        String lastUserRole,
        String preferredPlatform,
        String preferredSortMetric,
        List<String> mentionedGames,
        List<String> recommendedGames,
        List<String> developerCandidates,
        List<String> reinterpretationCandidates,
        List<String> interactionFeatures,
        List<String> constraints,
        List<String> excluded,
        String summaryText,
        LocalDateTime updatedAt,
        Long conversationId
) {
    public ConversationMemorySummaryResponse(
            String sessionId,
            String currentUserGoal,
            String lastIntent,
            String lastUserRole,
            String preferredPlatform,
            String preferredSortMetric,
            List<String> mentionedGames,
            List<String> recommendedGames,
            List<String> developerCandidates,
            List<String> reinterpretationCandidates,
            List<String> interactionFeatures,
            List<String> constraints,
            List<String> excluded,
            String summaryText,
            LocalDateTime updatedAt
    ) {
        this(
                sessionId,
                currentUserGoal,
                lastIntent,
                lastUserRole,
                preferredPlatform,
                preferredSortMetric,
                mentionedGames,
                recommendedGames,
                developerCandidates,
                reinterpretationCandidates,
                interactionFeatures,
                constraints,
                excluded,
                summaryText,
                updatedAt,
                null
        );
    }
}
