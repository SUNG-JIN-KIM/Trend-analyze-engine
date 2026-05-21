package com.gametrend.agent.onboarding.dto;

import java.util.List;

public record AgentPlanningContext(
        String previousMessage,
        String previousSummary,
        String previousReport,
        List<String> previousConceptTitles,
        ConversationMemorySummaryResponse memorySummary
) {
    public AgentPlanningContext(
            String previousMessage,
            String previousSummary,
            String previousReport,
            List<String> previousConceptTitles
    ) {
        this(previousMessage, previousSummary, previousReport, previousConceptTitles, null);
    }

    public static AgentPlanningContext empty() {
        return new AgentPlanningContext(null, null, null, List.of(), null);
    }
}
