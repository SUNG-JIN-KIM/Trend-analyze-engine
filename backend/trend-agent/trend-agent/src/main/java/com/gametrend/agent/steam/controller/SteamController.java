package com.gametrend.agent.steam.controller;

import com.gametrend.agent.steam.dto.SteamImportRequest;
import com.gametrend.agent.steam.dto.SteamImportResponse;
import com.gametrend.agent.steam.dto.SteamReviewResponse;
import com.gametrend.agent.steam.service.SteamImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SteamController {

    private final SteamImportService steamImportService;

    @GetMapping("/steam/reviews/{appId}")
    public SteamReviewResponse getReviewSummary(@PathVariable int appId) {
        return steamImportService.getReviewSummary(appId);
    }

    @PostMapping("/games/import/steam")
    @ResponseStatus(HttpStatus.CREATED)
    public SteamImportResponse importGame(@Valid @RequestBody SteamImportRequest request) {
        return steamImportService.importGame(request);
    }
}
