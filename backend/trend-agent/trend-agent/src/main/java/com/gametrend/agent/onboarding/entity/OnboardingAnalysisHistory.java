package com.gametrend.agent.onboarding.entity;

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
@Table("onboarding_analysis_history")
public class OnboardingAnalysisHistory {

    @Id
    private Long id;

    private Long userId;
    private Long projectId;
    private Long parentHistoryId;
    private String conversationId;
    private String message;
    private String targetPlatform;
    private String teamSize;
    private String preferredFeaturesJson;
    private String developmentPeriod;
    private String summary;
    private String recommendedConceptsJson;
    private String report;
    private LocalDateTime createdAt;
}
