package com.gametrend.agent.onboarding.repository;

import com.gametrend.agent.onboarding.entity.ConversationMemorySummary;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ConversationMemorySummaryRepository extends CrudRepository<ConversationMemorySummary, Long> {

    Optional<ConversationMemorySummary> findBySessionId(String sessionId);

    Optional<ConversationMemorySummary> findByConversationId(Long conversationId);
}
