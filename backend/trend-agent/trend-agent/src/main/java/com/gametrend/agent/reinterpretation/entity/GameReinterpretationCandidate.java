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
@Table("game_reinterpretation_candidate")
public class GameReinterpretationCandidate {

    @Id
    private Long id;

    private Long legacyGameId;
    private String title;
    private String source;
    private String sourceGameId;
    private Integer steamAppId;
    private int releaseYear;
    private String genresJson;
    private String tagsJson;
    private String mechanicsJson;
    private String interactionHintsJson;
    private int legacyPopularityScore;
    private int reviewSentimentScore;
    private int mechanicUniquenessScore;
    private int streamabilityScore;
    private int interactionFitScore;
    private int modernTrendFitScore;
    private int devFeasibilityScore;
    private double reinterpretationScore;
    private String reinterpretationConcept;
    private String reason;
    private String dataOrigin;
    private int reviewCount;
    private double positiveReviewRate;
    private String matchedLiveTrendSourcesJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
