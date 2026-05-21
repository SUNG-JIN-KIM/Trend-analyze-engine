package com.gametrend.agent.onboarding.dto;

import java.util.List;

public record ConversationMemoryUpdateContext(
        String sessionId,
        Long conversationId,
        String userMessage,
        AgentPlan agentPlan,
        List<EvidenceCardResponse> evidenceCards,
        String answerSummary,
        String answer
) {
    public ConversationMemoryUpdateContext(
            String sessionId,
            String userMessage,
            AgentPlan agentPlan,
            List<EvidenceCardResponse> evidenceCards,
            String answerSummary,
            String answer
    ) {
        this(sessionId, null, userMessage, agentPlan, evidenceCards, answerSummary, answer);
    }
}
