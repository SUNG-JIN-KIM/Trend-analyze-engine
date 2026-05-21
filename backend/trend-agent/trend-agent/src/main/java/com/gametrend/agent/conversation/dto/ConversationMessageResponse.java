package com.gametrend.agent.conversation.dto;

import com.gametrend.agent.conversation.entity.ConversationMessage;

import java.time.LocalDateTime;

public record ConversationMessageResponse(
        Long id,
        Long conversationId,
        String role,
        String content,
        String intent,
        String evidenceJson,
        LocalDateTime createdAt
) {

    public static ConversationMessageResponse from(ConversationMessage message) {
        return new ConversationMessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getRole(),
                message.getContent(),
                message.getIntent(),
                message.getEvidenceJson(),
                message.getCreatedAt()
        );
    }
}
