package com.gametrend.agent.livetrend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LiveTrendRefreshResponse(
        int requestedCount,
        int refreshedCount,
        int partialCount,
        String status,
        String message,
        LocalDateTime refreshedAt,
        List<LiveTrendGameResponse> games
) {
}
