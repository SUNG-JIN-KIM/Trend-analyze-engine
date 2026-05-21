package com.gametrend.agent.conversation.dto;

import jakarta.validation.constraints.Size;

public record ConversationUpdateRequest(
        @Size(max = 200)
        String title
) {
}
