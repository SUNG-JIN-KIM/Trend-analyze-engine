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
@Table("game_youtube_trend_scores")
public class GameYoutubeTrendScore {

    @Id
    private Long id;

    private Long gameId;
    private String keyword;
    private String gameTitle;
    private long totalViewCount;
    private double averageViewCount;
    private long totalLikeCount;
    private long totalCommentCount;
    private double averageEngagementRate;
    private int videoCount;
    private double viewScore;
    private double engagementScore;
    private double volumeScore;
    private double youtubeInterestScore;
    private LocalDateTime collectedAt;
    private LocalDateTime updatedAt;
}
