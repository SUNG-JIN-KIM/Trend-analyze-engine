package com.gametrend.agent.admin.chat.dto;

import com.gametrend.agent.admin.chat.Chat;

import java.time.LocalDateTime;

public record AdminChatResponse(
        Long id,
        Long userId,
        Long conversationId,
        Long conversationMessageId,
        String role,
        String content,
        String status,
        boolean reported,
        LocalDateTime hiddenAt,
        Long hiddenByUserId,
        LocalDateTime deletedAt,
        Long deletedByUserId,
        String moderationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminChatResponse from(Chat chat) {
        return new AdminChatResponse(
                chat.getId(),
                chat.getUserId(),
                chat.getConversationId(),
                chat.getConversationMessageId(),
                chat.getRole(),
                chat.getContent(),
                chat.getStatus() == null ? "ACTIVE" : chat.getStatus().name(),
                chat.isReported(),
                chat.getHiddenAt(),
                chat.getHiddenByUserId(),
                chat.getDeletedAt(),
                chat.getDeletedByUserId(),
                chat.getModerationReason(),
                chat.getCreatedAt(),
                chat.getUpdatedAt()
        );
    }
}
