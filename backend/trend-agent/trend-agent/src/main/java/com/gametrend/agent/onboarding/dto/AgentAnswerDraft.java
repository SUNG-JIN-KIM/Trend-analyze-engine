package com.gametrend.agent.onboarding.dto;

import java.util.List;

public record AgentAnswerDraft(
        String summary,
        String answer,
        String report,
        List<String> followUpQuestions
) {
}
