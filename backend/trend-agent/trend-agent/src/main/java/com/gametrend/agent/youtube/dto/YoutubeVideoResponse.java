package com.gametrend.agent.youtube.dto;

import com.gametrend.agent.youtube.entity.YoutubeVideo;

import java.time.LocalDateTime;

public record YoutubeVideoResponse(
        String videoId,
        String keyword,
        String title,
        String description,
        LocalDateTime publishedAt,
        String channelId,
        String channelTitle,
        String thumbnailUrl,
        long viewCount,
        long likeCount,
        long commentCount,
        long durationSeconds,
        LocalDateTime collectedAt
) {

    public static YoutubeVideoResponse from(YoutubeVideo video) {
        return new YoutubeVideoResponse(
                video.getVideoId(),
                video.getGameKeyword(),
                video.getTitle(),
                video.getDescription(),
                video.getPublishedAt(),
                video.getChannelId(),
                video.getChannelTitle(),
                video.getThumbnailUrl(),
                video.getViewCount(),
                video.getLikeCount(),
                video.getCommentCount(),
                video.getDurationSeconds(),
                video.getCollectedAt()
        );
    }
}
