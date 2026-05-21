package com.gametrend.agent.conversation.dto;

import com.gametrend.agent.conversation.entity.Conversation;
import com.gametrend.agent.conversation.entity.ConversationStatus;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        Long userId,
        String sessionId,
        String title,
        String lastMessage,
        String lastIntent,
        ConversationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getUserId(),
                conversation.getSessionId(),
                conversation.getTitle(),
                conversation.getLastMessage(),
                conversation.getLastIntent(),
                conversation.statusOrActive(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
