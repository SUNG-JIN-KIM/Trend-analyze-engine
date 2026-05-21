package com.gametrend.agent.conversation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.conversation.dto.ConversationCreateRequest;
import com.gametrend.agent.conversation.dto.ConversationDetailResponse;
import com.gametrend.agent.conversation.dto.ConversationResponse;
import com.gametrend.agent.conversation.dto.ConversationUpdateRequest;
import com.gametrend.agent.conversation.entity.Conversation;
import com.gametrend.agent.conversation.entity.ConversationMessage;
import com.gametrend.agent.conversation.entity.ConversationMessageRole;
import com.gametrend.agent.conversation.entity.ConversationStatus;
import com.gametrend.agent.conversation.exception.ConversationNotFoundException;
import com.gametrend.agent.conversation.repository.ConversationMessageRepository;
import com.gametrend.agent.conversation.repository.ConversationRepository;
import com.gametrend.agent.onboarding.dto.EvidenceCardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public ConversationResponse create(Long userId, ConversationCreateRequest request) {
        return ConversationResponse.from(createConversation(userId, request.title(), null));
    }

    public List<ConversationResponse> findConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .filter(this::isVisibleToOwner)
                .map(ConversationResponse::from)
                .toList();
    }

    public ConversationDetailResponse findConversation(Long conversationId, Long userId) {
        Conversation conversation = findOwnedConversation(conversationId, userId);
        return ConversationDetailResponse.from(
                conversation,
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
        );
    }

    public ConversationResponse update(Long conversationId, Long userId, ConversationUpdateRequest request) {
        Conversation conversation = findOwnedConversation(conversationId, userId);
        LocalDateTime now = LocalDateTime.now();
        Conversation updated = conversationRepository.save(conversation.toBuilder()
                .title(resolveTitle(request.title(), conversation.getTitle()))
                .updatedAt(now)
                .build());
        return ConversationResponse.from(updated);
    }

    public void delete(Long conversationId, Long userId) {
        Conversation conversation = findOwnedConversation(conversationId, userId);
        messageRepository.deleteAll(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId));
        conversationRepository.delete(conversation);
    }

    public Conversation findOwnedConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        if (!isVisibleToOwner(conversation)) {
            throw new ConversationNotFoundException(conversationId);
        }
        return conversation;
    }

    public Conversation resolveForAnalyze(Long userId, Long conversationId, String requestedSessionId, String message) {
        if (conversationId != null) {
            return findOwnedConversation(conversationId, userId);
        }
        return createConversation(userId, titleFromMessage(message), null);
    }

    public void appendExchange(
            Conversation conversation,
            String userMessage,
            String assistantAnswer,
            String intent,
            List<EvidenceCardResponse> evidenceCards
    ) {
        LocalDateTime now = LocalDateTime.now();
        messageRepository.save(ConversationMessage.builder()
                .conversationId(conversation.getId())
                .role(ConversationMessageRole.USER.name())
                .content(stripToEmpty(userMessage))
                .intent(intent)
                .evidenceJson("[]")
                .createdAt(now)
                .build());
        messageRepository.save(ConversationMessage.builder()
                .conversationId(conversation.getId())
                .role(ConversationMessageRole.ASSISTANT.name())
                .content(stripToEmpty(assistantAnswer))
                .intent(intent)
                .evidenceJson(writeEvidence(evidenceCards))
                .createdAt(now.plusNanos(1_000_000))
                .build());
        conversationRepository.save(conversation.toBuilder()
                .lastMessage(truncate(stripToEmpty(userMessage), 500))
                .lastIntent(intent)
                .updatedAt(now)
                .build());
    }

    private Conversation createConversation(Long userId, String title, String requestedSessionId) {
        LocalDateTime now = LocalDateTime.now();
        return conversationRepository.save(Conversation.builder()
                .userId(userId)
                .sessionId(resolveSessionId(requestedSessionId))
                .title(resolveTitle(title, "새 대화"))
                .lastMessage(null)
                .lastIntent(null)
                .status(ConversationStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private boolean isVisibleToOwner(Conversation conversation) {
        return conversation != null && conversation.statusOrActive() == ConversationStatus.ACTIVE;
    }

    private String resolveSessionId(String requestedSessionId) {
        if (requestedSessionId != null && !requestedSessionId.isBlank()) {
            return truncate(requestedSessionId.strip(), 100);
        }
        return "conversation-session-" + UUID.randomUUID();
    }

    private String resolveTitle(String requestedTitle, String fallbackTitle) {
        String title = requestedTitle == null ? null : requestedTitle.strip();
        if (title == null || title.isBlank()) {
            title = fallbackTitle;
        }
        return truncate(title, 200);
    }

    private String titleFromMessage(String message) {
        String title = message == null ? "" : message.strip().replaceAll("\\s+", " ");
        if (title.isBlank()) {
            return "새 Agent 대화";
        }
        return truncate(title, 36);
    }

    private String writeEvidence(List<EvidenceCardResponse> evidenceCards) {
        try {
            return objectMapper.writeValueAsString(evidenceCards == null ? List.of() : evidenceCards);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("대화 evidence JSON 직렬화에 실패했습니다.", ex);
        }
    }

    private String stripToEmpty(String value) {
        return value == null ? "" : value.strip();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
