package com.gametrend.agent.livetrend.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table("live_trend_game")
public class LiveTrendGame {

    @Id
    private Long id;

    private String source;
    private String title;
    private String genre;
    private String platform;
    private String sourceKeyword;
    private int liveStreamCount;
    private int totalViewerCount;
    private int viewerScore;
    private int streamCountScore;
    private int streamabilityScore;
    private int marketSignalScore;
    private double trendScore;
    private String signalStatus;
    private String dataOrigin;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
