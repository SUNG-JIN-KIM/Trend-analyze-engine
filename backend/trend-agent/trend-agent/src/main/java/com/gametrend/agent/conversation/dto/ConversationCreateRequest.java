package com.gametrend.agent.conversation.dto;

import jakarta.validation.constraints.Size;

public record ConversationCreateRequest(
        @Size(max = 200)
        String title
) {
}
