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
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table("chat_moderation_logs")
public class ChatModerationLog {

    @Id
    private Long id;

    private Long chatId;
    private Long adminUserId;
    private String action;
    private String reason;
    private String ipAddress;
    private String userAgent;
    private String detail;
    private LocalDateTime createdAt;
}
