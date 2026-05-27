package com.gametrend.agent.youtube.controller;

import com.gametrend.agent.youtube.dto.GameYoutubeTrendScoreResponse;
import com.gametrend.agent.youtube.dto.YoutubeTrendResponse;
import com.gametrend.agent.youtube.service.YoutubeTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class YoutubeTrendController {

    private final YoutubeTrendService youtubeTrendService;

    @GetMapping("/api/youtube/trends")
    public YoutubeTrendResponse findTrend(@RequestParam String keyword) {
        return youtubeTrendService.findTrend(keyword);
    }

    @GetMapping("/api/youtube/top-games")
    public List<GameYoutubeTrendScoreResponse> findTopGames(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return youtubeTrendService.findTopGames(limit);
    }
}
