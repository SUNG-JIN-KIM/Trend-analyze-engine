package com.gametrend.agent.reinterpretation.service;

import com.gametrend.agent.onboarding.dto.AgentQueryConditionResponse;
import com.gametrend.agent.reinterpretation.dto.ReinterpretationCandidateResponse;
import com.gametrend.agent.reinterpretation.entity.GameReinterpretationCandidate;
import com.gametrend.agent.reinterpretation.repository.GameReinterpretationCandidateRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ReinterpretationCandidateService {

    private final GameReinterpretationCandidateRepository candidateRepository;
    private final LegacyGameRefreshService refreshService;
    private final ReinterpretationMapper mapper;

    public ReinterpretationCandidateService(
            GameReinterpretationCandidateRepository candidateRepository,
            LegacyGameRefreshService refreshService,
            ReinterpretationMapper mapper
    ) {
        this.candidateRepository = candidateRepository;
        this.refreshService = refreshService;
        this.mapper = mapper;
    }

    public List<ReinterpretationCandidateResponse> findCandidates(int limit) {
        refreshService.ensureSeedFallbacks();
        return candidateRepository.findAllByOrderByReinterpretationScoreDesc()
                .stream()
                .limit(Math.max(1, limit))
                .map(mapper::toCandidateResponse)
                .toList();
    }

    public List<ReinterpretationCandidateResponse> findCandidates(AgentQueryConditionResponse condition, int limit) {
        refreshService.ensureSeedFallbacks();
        return candidateRepository.findAllByOrderByReinterpretationScoreDesc()
                .stream()
                .filter(candidate -> matchesInteraction(candidate, condition))
                .sorted(comparator(condition))
                .limit(Math.max(1, limit))
                .map(mapper::toCandidateResponse)
                .toList();
    }

    private boolean matchesInteraction(GameReinterpretationCandidate candidate, AgentQueryConditionResponse condition) {
        if (condition == null || condition.interactionFeatures() == null || condition.interactionFeatures().isEmpty()) {
            return true;
        }
        String hints = candidate.getInteractionHintsJson().toUpperCase(Locale.ROOT);
        return condition.interactionFeatures().stream().anyMatch(hints::contains);
    }

    private Comparator<GameReinterpretationCandidate> comparator(AgentQueryConditionResponse condition) {
        if (condition == null) {
            return Comparator.comparingDouble(GameReinterpretationCandidate::getReinterpretationScore).reversed();
        }
        return switch (condition.sortMetric()) {
            case "MARKET_SIGNAL" -> Comparator.comparingInt(GameReinterpretationCandidate::getModernTrendFitScore).reversed()
                    .thenComparing(Comparator.comparingDouble(GameReinterpretationCandidate::getReinterpretationScore).reversed());
            case "STREAM_COUNT", "STREAMER_SPREAD" -> Comparator.comparingInt(GameReinterpretationCandidate::getStreamabilityScore).reversed()
                    .thenComparing(Comparator.comparingInt(GameReinterpretationCandidate::getInteractionFitScore).reversed());
            default -> Comparator.comparingDouble(GameReinterpretationCandidate::getReinterpretationScore).reversed();
        };
    }
}
