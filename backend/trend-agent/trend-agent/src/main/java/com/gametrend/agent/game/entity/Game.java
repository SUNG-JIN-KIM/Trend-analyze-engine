package com.gametrend.agent.game.entity;

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
@Table("games")
public class Game {

    @Id
    private Long id;

    private String title;
    private String genre;
    private String platform;
    private String playStyle;
    private int streamabilityScore;
    private int webcamFitScore;
    private int ttsFitScore;
    private int sttFitScore;
    private int noveltyScore;
    private int devFeasibilityScore;
    private int marketSignalScore;
    private double recommendationScore;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
