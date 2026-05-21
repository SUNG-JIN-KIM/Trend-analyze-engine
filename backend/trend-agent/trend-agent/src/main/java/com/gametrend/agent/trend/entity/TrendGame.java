package com.gametrend.agent.trend.entity;

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
@Table("trend_game")
public class TrendGame {

    @Id
    private Long id;

    private String title;
    private String genre;
    private String platform;
    private Integer steamAppId;
    private String twitchKeyword;
    private int steamReviewScore;
    private int steamTotalReviews;
    private double steamPositiveRate;
    private int twitchLiveStreamCount;
    private int twitchTotalViewerCount;
    private int twitchViewerScore;
    private int twitchStreamCountScore;
    private int streamabilityScore;
    private int marketSignalScore;
    private double internalRecommendationScore;
    private double trendScore;
    private String signalStatus;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
