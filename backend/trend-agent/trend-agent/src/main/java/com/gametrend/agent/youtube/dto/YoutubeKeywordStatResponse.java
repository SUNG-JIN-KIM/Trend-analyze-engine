package com.gametrend.agent.youtube.dto;

import com.gametrend.agent.youtube.entity.YoutubeKeywordStat;

import java.time.LocalDateTime;

public record YoutubeKeywordStatResponse(
        Long id,
        String gameKeyword,
        String statKeyword,
        String sentiment,
        int mentionCount,
        String sampleText,
        LocalDateTime updatedAt
) {

    public static YoutubeKeywordStatResponse from(YoutubeKeywordStat stat) {
        return new YoutubeKeywordStatResponse(
                stat.getId(),
                stat.getGameKeyword(),
                stat.getStatKeyword(),
                stat.getSentiment(),
                stat.getMentionCount(),
                stat.getSampleText(),
                stat.getUpdatedAt()
        );
    }
}
