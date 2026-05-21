package com.gametrend.agent.trend.dto;

import java.util.List;

public record TrendRefreshRequest(
        List<TrendSeedGameRequest> games
) {
}
