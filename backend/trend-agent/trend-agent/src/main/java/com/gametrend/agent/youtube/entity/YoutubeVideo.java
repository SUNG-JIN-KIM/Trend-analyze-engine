package com.gametrend.agent.youtube.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table("youtube_videos")
public class YoutubeVideo {

    @Id
    private Long id;

    private String videoId;

    private String gameKeyword;
    private String keyword;
    private String title;
    private String description;
    private LocalDateTime publishedAt;
    private String channelId;
    private String channelTitle;
    private String thumbnailUrl;
    private long viewCount;
    private long likeCount;
    private long commentCount;
    private long durationSeconds;
    private LocalDateTime collectedAt;
    private LocalDateTime updatedAt;
}
