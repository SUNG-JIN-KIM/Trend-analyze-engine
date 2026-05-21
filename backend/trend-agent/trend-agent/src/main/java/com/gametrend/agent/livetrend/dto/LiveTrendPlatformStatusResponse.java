package com.gametrend.agent.livetrend.dto;

import com.gametrend.agent.livetrend.entity.LiveTrendPlatformStatus;

import java.time.LocalDateTime;

public record LiveTrendPlatformStatusResponse(
        String platform,
        boolean configured,
        String status,
        String message,
        LocalDateTime lastSuccessAt,
        LocalDateTime lastFailureAt
) {

    public static LiveTrendPlatformStatusResponse from(LiveTrendPlatformStatus status) {
        return new LiveTrendPlatformStatusResponse(
                status.getPlatform(),
                status.isConfigured(),
                status.getStatus(),
                status.getMessage(),
                status.getLastSuccessAt(),
                status.getLastFailureAt()
        );
    }
}
