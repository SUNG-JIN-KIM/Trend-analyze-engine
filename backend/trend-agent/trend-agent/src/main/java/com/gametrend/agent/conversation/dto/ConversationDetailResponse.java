package com.gametrend.agent.conversation.dto;

import com.gametrend.agent.conversation.entity.Conversation;
import com.gametrend.agent.conversation.entity.ConversationMessage;
import com.gametrend.agent.conversation.entity.ConversationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationDetailResponse(
        Long id,
        Long userId,
        String sessionId,
        String title,
        String lastMessage,
        String lastIntent,
        ConversationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ConversationMessageResponse> messages
) {

    public static ConversationDetailResponse from(Conversation conversation, List<ConversationMessage> messages) {
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getUserId(),
                conversation.getSessionId(),
                conversation.getTitle(),
                conversation.getLastMessage(),
                conversation.getLastIntent(),
                conversation.statusOrActive(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages.stream()
                        .map(ConversationMessageResponse::from)
                        .toList()
        );
    }
}
