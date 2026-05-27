package com.gametrend.agent.youtube.dto;

import com.gametrend.agent.youtube.entity.YoutubeCollectLog;

import java.time.LocalDateTime;

public record YoutubeCollectLogResponse(
        Long id,
        String keyword,
        String status,
        String message,
        int videoCount,
        int scoreCount,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public static YoutubeCollectLogResponse from(YoutubeCollectLog log) {
        return new YoutubeCollectLogResponse(
                log.getId(),
                log.getKeyword(),
                log.getStatus(),
                log.getMessage(),
                log.getVideoCount(),
                log.getScoreCount(),
                log.getStartedAt(),
                log.getCompletedAt()
        );
    }
}
