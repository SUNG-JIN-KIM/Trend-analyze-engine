package com.gametrend.agent.conversation.repository;

import com.gametrend.agent.conversation.entity.Conversation;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends CrudRepository<Conversation, Long> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<Conversation> findByIdAndUserId(Long id, Long userId);
}
