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
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table("conversation_memory_summary")
public class ConversationMemorySummary {

    @Id
    private Long id;

    private String sessionId;
    private Long conversationId;
    private String currentUserGoal;
    private String lastIntent;
    private String lastUserRole;
    private String preferredPlatform;
    private String preferredSortMetric;
    private String mentionedGamesJson;
    private String recommendedGamesJson;
    private String developerCandidatesJson;
    private String reinterpretationCandidatesJson;
    private String interactionFeaturesJson;
    private String constraintsJson;
    private String excludedJson;
    private String summaryText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
