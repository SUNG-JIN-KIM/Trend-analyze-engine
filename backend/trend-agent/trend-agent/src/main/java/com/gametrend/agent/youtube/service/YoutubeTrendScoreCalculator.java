package com.gametrend.agent.youtube.service;

import com.gametrend.agent.youtube.entity.YoutubeVideo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class YoutubeTrendScoreCalculator {

    public Score calculate(List<YoutubeVideo> videos, LocalDateTime now) {
        if (videos.isEmpty()) {
            return new Score(0, 0, 0, 0, 0);
        }

        long totalViews = videos.stream().mapToLong(YoutubeVideo::getViewCount).sum();
        long totalLikes = videos.stream().mapToLong(YoutubeVideo::getLikeCount).sum();
        long totalComments = videos.stream().mapToLong(YoutubeVideo::getCommentCount).sum();

        double engagementRate = totalViews <= 0 ? 0 : (totalLikes + totalComments) / (double) totalViews;
        double viewScore = clamp(logScore(totalViews, 9));
        double engagementScore = clamp(engagementRate * 10_000);
        double volumeScore = clamp(videos.size() / 20.0 * 100);

        double youtubeInterestScore = clamp(
                viewScore * 0.5
                        + engagementScore * 0.3
                        + volumeScore * 0.2
        );
        return new Score(
                round(viewScore),
                round(engagementScore),
                round(volumeScore),
                round(engagementRate),
                round(youtubeInterestScore)
        );
    }

    private double logScore(double value, double maxPower) {
        if (value <= 0) {
            return 0;
        }
        return Math.min(100, Math.log10(value + 1) / maxPower * 100);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record Score(
            double viewScore,
            double engagementScore,
            double volumeScore,
            double averageEngagementRate,
            double youtubeInterestScore
    ) {
    }
}
