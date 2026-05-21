package com.gametrend.agent.reinterpretation.entity;

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
@Table("legacy_game")
public class LegacyGame {

    @Id
    private Long id;

    private String title;
    private String source;
    private String sourceGameId;
    private Integer steamAppId;
    private int releaseYear;
    private String genresJson;
    private String tagsJson;
    private String mechanicsJson;
    private String interactionHintsJson;
    private int mechanicUniquenessScore;
    private int streamabilityScore;
    private int interactionFitSeedScore;
    private int devFeasibilityScore;
    private int reviewCount;
    private double positiveReviewRate;
    private int legacyPopularityScore;
    private int reviewSentimentScore;
    private String dataOrigin;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
