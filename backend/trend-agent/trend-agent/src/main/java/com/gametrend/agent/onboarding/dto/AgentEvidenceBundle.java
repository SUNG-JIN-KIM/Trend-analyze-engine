package com.gametrend.agent.onboarding.dto;

import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.reinterpretation.dto.ReinterpretationCandidateResponse;

import java.util.List;

public record AgentEvidenceBundle(
        List<LiveTrendGameResponse> liveTrendGames,
        List<ReinterpretationCandidateResponse> reinterpretationCandidates,
        List<EvidenceCardResponse> evidenceCards
) {
    public static AgentEvidenceBundle empty() {
        return new AgentEvidenceBundle(List.of(), List.of(), List.of());
    }
}
