package com.gametrend.agent.trend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TrendRefreshResponse(
        int requestedCount,
        int refreshedCount,
        int partialCount,
        String message,
        LocalDateTime refreshedAt,
        List<TrendGameResponse> games
) {
}
