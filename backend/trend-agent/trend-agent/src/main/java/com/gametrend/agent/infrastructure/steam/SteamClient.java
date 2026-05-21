package com.gametrend.agent.infrastructure.steam;

public interface SteamClient {

    SteamReviewSummary getReviewSummary(int appId);
}
