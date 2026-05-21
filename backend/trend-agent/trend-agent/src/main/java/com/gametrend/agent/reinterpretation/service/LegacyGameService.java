package com.gametrend.agent.reinterpretation.service;

import com.gametrend.agent.reinterpretation.dto.LegacyGameResponse;
import com.gametrend.agent.reinterpretation.repository.LegacyGameRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LegacyGameService {

    private final LegacyGameRepository legacyGameRepository;
    private final LegacyGameRefreshService refreshService;
    private final ReinterpretationMapper mapper;

    public LegacyGameService(
            LegacyGameRepository legacyGameRepository,
            LegacyGameRefreshService refreshService,
            ReinterpretationMapper mapper
    ) {
        this.legacyGameRepository = legacyGameRepository;
        this.refreshService = refreshService;
        this.mapper = mapper;
    }

    public List<LegacyGameResponse> findLegacyGames() {
        refreshService.ensureSeedFallbacks();
        return legacyGameRepository.findAllByOrderByTitleAsc()
                .stream()
                .map(mapper::toLegacyGameResponse)
                .toList();
    }
}
