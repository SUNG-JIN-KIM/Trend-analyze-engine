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
@Table("youtube_keyword_stats")
public class YoutubeKeywordStat {

    @Id
    private Long id;

    private String gameKeyword;
    private String statKeyword;
    private String sentiment;
    private int mentionCount;
    private String sampleText;
    private LocalDateTime collectedAt;
    private LocalDateTime updatedAt;
}
