package com.gametrend.agent.admin.chat;

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
@Table("chats")
public class Chat {

    @Id
    private Long id;

    private Long userId;
    private Long conversationId;
    private Long conversationMessageId;
    private String role;
    private String content;
    private ChatStatus status;
    private boolean reported;
    private LocalDateTime hiddenAt;
    private Long hiddenByUserId;
    private LocalDateTime deletedAt;
    private Long deletedByUserId;
    private String moderationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
