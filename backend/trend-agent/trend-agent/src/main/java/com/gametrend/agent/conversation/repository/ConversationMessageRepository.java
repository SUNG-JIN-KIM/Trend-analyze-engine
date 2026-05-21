package com.gametrend.agent.conversation.repository;

import com.gametrend.agent.conversation.entity.ConversationMessage;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ConversationMessageRepository extends CrudRepository<ConversationMessage, Long> {

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
