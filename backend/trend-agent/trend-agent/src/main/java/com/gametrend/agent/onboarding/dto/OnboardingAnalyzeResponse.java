package com.gametrend.agent.onboarding.dto;

import java.util.List;

public record OnboardingAnalyzeResponse(
        Long historyId,
        String summary,
        List<RecommendedConceptResponse> recommendedConcepts,
        String answer,
        String report,
        String intent,
        List<String> detectedKeywords,
        List<String> followUpQuestions,
        List<EvidenceCardResponse> evidenceCards,
        Long parentHistoryId,
        String conversationId,
        AgentQueryConditionResponse queryCondition,
        AgentPlan agentPlan,
        String sessionId,
        ConversationMemorySummaryResponse memorySummary
) {
    public OnboardingAnalyzeResponse(
            Long historyId,
            String summary,
            List<RecommendedConceptResponse> recommendedConcepts,
            String answer,
            String report,
            String intent,
            List<String> detectedKeywords,
            List<String> followUpQuestions,
            List<EvidenceCardResponse> evidenceCards,
            Long parentHistoryId,
            String conversationId,
            AgentQueryConditionResponse queryCondition,
            AgentPlan agentPlan
    ) {
        this(
                historyId,
                summary,
                recommendedConcepts,
                answer,
                report,
                intent,
                detectedKeywords,
                followUpQuestions,
                evidenceCards,
                parentHistoryId,
                conversationId,
                queryCondition,
                agentPlan,
                null,
                null
        );
    }

    public OnboardingAnalyzeResponse(
            Long historyId,
            String summary,
            List<RecommendedConceptResponse> recommendedConcepts,
            String answer,
            String report,
            String intent,
            List<String> detectedKeywords,
            List<String> followUpQuestions,
            List<EvidenceCardResponse> evidenceCards,
            Long parentHistoryId,
            String conversationId,
            AgentQueryConditionResponse queryCondition
    ) {
        this(
                historyId,
                summary,
                recommendedConcepts,
                answer,
                report,
                intent,
                detectedKeywords,
                followUpQuestions,
                evidenceCards,
                parentHistoryId,
                conversationId,
                queryCondition,
                null,
                null,
                null
        );
    }

    public OnboardingAnalyzeResponse(
            Long historyId,
            String summary,
            List<RecommendedConceptResponse> recommendedConcepts,
            String answer,
            String report,
            String intent,
            List<String> detectedKeywords,
            List<String> followUpQuestions,
            List<EvidenceCardResponse> evidenceCards,
            Long parentHistoryId,
            String conversationId
    ) {
        this(
                historyId,
                summary,
                recommendedConcepts,
                answer,
                report,
                intent,
                detectedKeywords,
                followUpQuestions,
                evidenceCards,
                parentHistoryId,
                conversationId,
                null,
                null,
                null,
                null
        );
    }

    public OnboardingAnalyzeResponse(
            Long historyId,
            String summary,
            List<RecommendedConceptResponse> recommendedConcepts,
            String answer,
            String report,
            String intent,
            List<String> detectedKeywords,
            List<String> followUpQuestions,
            List<EvidenceCardResponse> evidenceCards
    ) {
        this(
                historyId,
                summary,
                recommendedConcepts,
                answer,
                report,
                intent,
                detectedKeywords,
                followUpQuestions,
                evidenceCards,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
