package com.gametrend.agent.livetrend.controller;

import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.livetrend.dto.LiveTrendRankingResponse;
import com.gametrend.agent.livetrend.dto.LiveTrendRefreshResponse;
import com.gametrend.agent.livetrend.dto.LiveTrendRefreshStatusResponse;
import com.gametrend.agent.livetrend.service.LiveTrendRefreshCoordinator;
import com.gametrend.agent.livetrend.service.LiveTrendRefreshStatusManager;
import com.gametrend.agent.livetrend.service.LiveTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/live-trends")
@RequiredArgsConstructor
public class LiveTrendController {

    private final LiveTrendService liveTrendService;
    private final LiveTrendRefreshCoordinator refreshCoordinator;
    private final LiveTrendRefreshStatusManager statusManager;

    @PostMapping("/refresh")
    public LiveTrendRefreshResponse refresh() {
        return refreshCoordinator.refreshLiveTrends();
    }

    @GetMapping("/games")
    public List<LiveTrendGameResponse> findLiveTrendGames() {
        return liveTrendService.findLiveTrendGames();
    }

    @GetMapping("/games/top")
    public List<LiveTrendGameResponse> findTopLiveTrendGames(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String platform
    ) {
        return liveTrendService.findTopLiveTrendGames(limit, platform);
    }

    @GetMapping("/rankings")
    public List<LiveTrendRankingResponse> findLiveTrendRankings(
            @RequestParam(required = false) String platform,
            @RequestParam(defaultValue = "TREND_SCORE") String sort,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return liveTrendService.findRankings(platform, sort, limit);
    }

    @GetMapping("/games/{id}")
    public LiveTrendGameResponse findLiveTrendGame(@PathVariable Long id) {
        return liveTrendService.findLiveTrendGame(id);
    }

    @GetMapping("/status")
    public LiveTrendRefreshStatusResponse getStatus() {
        return statusManager.getStatus();
    }
}
