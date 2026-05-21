package com.gametrend.agent.admin.conversation;

import com.gametrend.agent.admin.audit.AdminAuditService;
import com.gametrend.agent.admin.common.AdminManagementException;
import com.gametrend.agent.admin.common.AdminPageResponse;
import com.gametrend.agent.admin.conversation.dto.AdminConversationResponse;
import com.gametrend.agent.conversation.dto.ConversationDetailResponse;
import com.gametrend.agent.conversation.entity.Conversation;
import com.gametrend.agent.conversation.entity.ConversationStatus;
import com.gametrend.agent.conversation.repository.ConversationMessageRepository;
import com.gametrend.agent.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class AdminConversationManagementService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final AdminAuditService adminAuditService;

    public AdminPageResponse<AdminConversationResponse> searchConversations(
            String user,
            String keyword,
            String status,
            int page,
            int size,
            String sort
    ) {
        var conversations = StreamSupport.stream(conversationRepository.findAll().spliterator(), false)
                .filter(conversation -> containsIgnoreCase(String.valueOf(conversation.getUserId()), user))
                .filter(conversation -> containsKeyword(conversation, keyword))
                .filter(conversation -> matchesStatus(conversation.statusOrActive(), status))
                .sorted(conversationComparator(sort))
                .map(AdminConversationResponse::from)
                .toList();

        return AdminPageResponse.of(conversations, page, size);
    }

    public ConversationDetailResponse getConversation(Long conversationId) {
        Conversation conversation = loadConversation(conversationId);
        return ConversationDetailResponse.from(
                conversation,
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
        );
    }

    @Transactional
    public AdminConversationResponse hide(Long actorUserId, Long conversationId, String reason) {
        Conversation conversation = loadConversation(conversationId);
        LocalDateTime now = LocalDateTime.now();
        Conversation saved = conversationRepository.save(conversation.toBuilder()
                .status(ConversationStatus.HIDDEN)
                .hiddenAt(now)
                .hiddenByUserId(actorUserId)
                .moderationReason(stripToNull(reason))
                .updatedAt(now)
                .build());
        adminAuditService.log(
                actorUserId,
                "CONVERSATION_HIDDEN",
                "CONVERSATION",
                saved.getId(),
                "reason=%s".formatted(stripToNull(reason))
        );
        return AdminConversationResponse.from(saved);
    }

    @Transactional
    public AdminConversationResponse restore(Long actorUserId, Long conversationId, String reason) {
        Conversation conversation = loadConversation(conversationId);
        LocalDateTime now = LocalDateTime.now();
        Conversation saved = conversationRepository.save(conversation.toBuilder()
                .status(ConversationStatus.ACTIVE)
                .hiddenAt(null)
                .hiddenByUserId(null)
                .deletedAt(null)
                .deletedByUserId(null)
                .moderationReason(stripToNull(reason))
                .updatedAt(now)
                .build());
        adminAuditService.log(
                actorUserId,
                "CONVERSATION_RESTORED",
                "CONVERSATION",
                saved.getId(),
                "reason=%s".formatted(stripToNull(reason))
        );
        return AdminConversationResponse.from(saved);
    }

    @Transactional
    public AdminConversationResponse softDelete(Long actorUserId, Long conversationId, String reason) {
        Conversation conversation = loadConversation(conversationId);
        LocalDateTime now = LocalDateTime.now();
        Conversation saved = conversationRepository.save(conversation.toBuilder()
                .status(ConversationStatus.DELETED)
                .deletedAt(now)
                .deletedByUserId(actorUserId)
                .moderationReason(stripToNull(reason))
                .updatedAt(now)
                .build());
        adminAuditService.log(
                actorUserId,
                "CONVERSATION_DELETED",
                "CONVERSATION",
                saved.getId(),
                "reason=%s".formatted(stripToNull(reason))
        );
        return AdminConversationResponse.from(saved);
    }

    private Conversation loadConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> AdminManagementException.notFound("대화"));
    }

    private boolean containsKeyword(Conversation conversation, String keyword) {
        String value = stripToNull(keyword);
        if (value == null) {
            return true;
        }
        return containsIgnoreCase(conversation.getTitle(), value)
                || containsIgnoreCase(conversation.getLastMessage(), value)
                || containsIgnoreCase(conversation.getLastIntent(), value)
                || containsIgnoreCase(conversation.getSessionId(), value);
    }

    private boolean containsIgnoreCase(String actual, String expected) {
        String expectedValue = stripToNull(expected);
        if (expectedValue == null) {
            return true;
        }
        return actual != null && actual.toLowerCase(Locale.ROOT).contains(expectedValue.toLowerCase(Locale.ROOT));
    }

    private boolean matchesStatus(ConversationStatus actual, String expected) {
        String expectedValue = stripToNull(expected);
        if (expectedValue == null || "ALL".equalsIgnoreCase(expectedValue)) {
            return true;
        }
        return actual != null && actual.name().equalsIgnoreCase(expectedValue);
    }

    private Comparator<Conversation> conversationComparator(String sort) {
        String value = stripToNull(sort);
        Comparator<Conversation> byIdDesc = Comparator.comparing(
                Conversation::getId,
                Comparator.nullsLast(Long::compareTo)
        ).reversed();
        if ("createdAt".equalsIgnoreCase(value)) {
            return Comparator.comparing(Conversation::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed()
                    .thenComparing(byIdDesc);
        }
        return Comparator.comparing(Conversation::getUpdatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed()
                .thenComparing(byIdDesc);
    }

    private String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
