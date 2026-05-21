package com.gametrend.agent.admin.conversation.dto;

import com.gametrend.agent.conversation.entity.Conversation;
import com.gametrend.agent.conversation.entity.ConversationStatus;

import java.time.LocalDateTime;

public record AdminConversationResponse(
        Long id,
        Long userId,
        String sessionId,
        String title,
        String lastMessage,
        String lastIntent,
        ConversationStatus status,
        boolean reported,
        LocalDateTime hiddenAt,
        Long hiddenByUserId,
        LocalDateTime deletedAt,
        Long deletedByUserId,
        String moderationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminConversationResponse from(Conversation conversation) {
        return new AdminConversationResponse(
                conversation.getId(),
                conversation.getUserId(),
                conversation.getSessionId(),
                conversation.getTitle(),
                conversation.getLastMessage(),
                conversation.getLastIntent(),
                conversation.statusOrActive(),
                false,
                conversation.getHiddenAt(),
                conversation.getHiddenByUserId(),
                conversation.getDeletedAt(),
                conversation.getDeletedByUserId(),
                conversation.getModerationReason(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
