package com.gametrend.agent.trend.controller;

import com.gametrend.agent.trend.dto.TrendGameResponse;
import com.gametrend.agent.trend.dto.TrendRefreshRequest;
import com.gametrend.agent.trend.dto.TrendRefreshResponse;
import com.gametrend.agent.trend.service.TrendGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
public class TrendController {

    private final TrendGameService trendGameService;

    @PostMapping("/refresh")
    public TrendRefreshResponse refresh(@RequestBody(required = false) TrendRefreshRequest request) {
        return trendGameService.refresh(request);
    }

    @GetMapping("/games")
    public List<TrendGameResponse> findTrendGames() {
        return trendGameService.findTrendGames();
    }

    @GetMapping("/games/top")
    public List<TrendGameResponse> findTopTrendGames(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return trendGameService.findTopTrendGames(limit);
    }

    @GetMapping("/games/{id}")
    public TrendGameResponse findTrendGame(@PathVariable Long id) {
        return trendGameService.findTrendGame(id);
    }
}
