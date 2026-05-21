package com.gametrend.agent.conversation.entity;

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
@Table("conversation")
public class Conversation {

    @Id
    private Long id;

    private Long userId;
    private String sessionId;
    private String title;
    private String lastMessage;
    private String lastIntent;
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;
    private LocalDateTime hiddenAt;
    private Long hiddenByUserId;
    private LocalDateTime deletedAt;
    private Long deletedByUserId;
    private String moderationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConversationStatus statusOrActive() {
        return status == null ? ConversationStatus.ACTIVE : status;
    }
}
