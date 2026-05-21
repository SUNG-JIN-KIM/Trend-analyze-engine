package com.gametrend.agent.admin.chat.dto;

import jakarta.validation.constraints.Size;

public record AdminChatModerationRequest(
        @Size(max = 1000)
        String reason
) {
}
