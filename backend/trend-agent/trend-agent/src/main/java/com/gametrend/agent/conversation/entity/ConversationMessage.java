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
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table("conversation_message")
public class ConversationMessage {

    @Id
    private Long id;

    private Long conversationId;
    private String role;
    private String content;
    private String intent;
    private String evidenceJson;
    private LocalDateTime createdAt;
}
