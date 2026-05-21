package com.gametrend.agent.admin.conversation.dto;

import jakarta.validation.constraints.Size;

public record AdminConversationModerationRequest(
        @Size(max = 1000)
        String reason
) {
}
