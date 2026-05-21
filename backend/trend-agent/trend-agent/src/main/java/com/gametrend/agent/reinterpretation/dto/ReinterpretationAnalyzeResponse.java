package com.gametrend.agent.reinterpretation.dto;

import com.gametrend.agent.onboarding.dto.AgentQueryConditionResponse;
import com.gametrend.agent.onboarding.dto.EvidenceCardResponse;

import java.util.List;

public record ReinterpretationAnalyzeResponse(
        String summary,
        String answer,
        String report,
        AgentQueryConditionResponse queryCondition,
        List<ReinterpretationCandidateResponse> candidates,
        List<EvidenceCardResponse> evidenceCards
) {
}
