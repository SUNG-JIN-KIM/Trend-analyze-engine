package com.gametrend.agent.steam.dto;

import com.gametrend.agent.game.dto.GameResponse;

public record SteamImportResponse(
        GameResponse game,
        SteamReviewResponse steamReview
) {
}
