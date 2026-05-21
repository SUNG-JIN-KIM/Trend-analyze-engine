package com.gametrend.agent.conversation.exception;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(Long conversationId) {
        super("대화를 찾을 수 없습니다. conversationId=" + conversationId);
    }
}
