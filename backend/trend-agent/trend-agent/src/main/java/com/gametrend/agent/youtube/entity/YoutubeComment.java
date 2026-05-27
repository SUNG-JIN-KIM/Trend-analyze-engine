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
@Table("youtube_comments")
public class YoutubeComment {

    @Id
    private Long id;

    private String commentId;
    private String videoId;
    private String gameKeyword;
    private String authorName;
    private String text;
    private long likeCount;
    private LocalDateTime publishedAt;
    private LocalDateTime collectedAt;
}
