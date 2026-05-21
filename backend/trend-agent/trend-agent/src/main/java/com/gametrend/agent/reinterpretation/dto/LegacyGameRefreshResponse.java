package com.gametrend.agent.reinterpretation.dto;

import java.time.LocalDateTime;

public record LegacyGameRefreshResponse(
        int requestedCount,
        int refreshedCount,
        int fallbackCount,
        String status,
        String message,
        LocalDateTime refreshedAt
) {
}
