package com.gametrend.agent.reinterpretation.controller;

import com.gametrend.agent.reinterpretation.dto.LegacyGameRefreshResponse;
import com.gametrend.agent.reinterpretation.dto.LegacyGameResponse;
import com.gametrend.agent.reinterpretation.service.LegacyGameRefreshService;
import com.gametrend.agent.reinterpretation.service.LegacyGameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/legacy-games")
public class LegacyGameController {

    private final LegacyGameRefreshService refreshService;
    private final LegacyGameService legacyGameService;

    public LegacyGameController(
            LegacyGameRefreshService refreshService,
            LegacyGameService legacyGameService
    ) {
        this.refreshService = refreshService;
        this.legacyGameService = legacyGameService;
    }

    @PostMapping("/refresh")
    public LegacyGameRefreshResponse refreshLegacyGames() {
        return refreshService.refresh();
    }

    @GetMapping
    public List<LegacyGameResponse> findLegacyGames() {
        return legacyGameService.findLegacyGames();
    }
}
