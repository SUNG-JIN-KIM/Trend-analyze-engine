package com.gametrend.agent.admin.chat;

import com.gametrend.agent.admin.audit.AdminAuditService;
import com.gametrend.agent.admin.chat.dto.AdminChatResponse;
import com.gametrend.agent.admin.common.AdminManagementException;
import com.gametrend.agent.admin.common.AdminPageResponse;
import com.gametrend.agent.auth.entity.UserAccount;
import com.gametrend.agent.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class AdminChatManagementService {

    private final ChatRepository chatRepository;
    private final ChatModerationLogRepository chatModerationLogRepository;
    private final UserRepository userRepository;
    private final AdminAuditService adminAuditService;

    public AdminPageResponse<AdminChatResponse> searchChats(
            String user,
            String keyword,
            String status,
            Boolean reported,
            int page,
            int size,
            String sort
    ) {
        var users = StreamSupport.stream(userRepository.findAll().spliterator(), false).toList();
        var chats = StreamSupport.stream(chatRepository.findAll().spliterator(), false)
                .filter(chat -> matchesUser(chat, user, users))
                .filter(chat -> containsIgnoreCase(chat.getContent(), keyword))
                .filter(chat -> matchesStatus(chat, status))
                .filter(chat -> reported == null || chat.isReported() == reported)
                .sorted(chatComparator(sort))
                .map(AdminChatResponse::from)
                .toList();

        return AdminPageResponse.of(chats, page, size);
    }

    @Transactional
    public AdminChatResponse hide(Long actorUserId, Long chatId, String reason) {
        Chat chat = loadChat(chatId);
        LocalDateTime now = LocalDateTime.now();
        Chat saved = chatRepository.save(chat.toBuilder()
                .status(ChatStatus.HIDDEN)
                .hiddenAt(now)
                .hiddenByUserId(actorUserId)
                .moderationReason(normalize(reason))
                .updatedAt(now)
                .build());
        logModeration(actorUserId, saved, "CHAT_HIDDEN", reason);
        return AdminChatResponse.from(saved);
    }

    @Transactional
    public AdminChatResponse restore(Long actorUserId, Long chatId, String reason) {
        Chat chat = loadChat(chatId);
        LocalDateTime now = LocalDateTime.now();
        Chat saved = chatRepository.save(chat.toBuilder()
                .status(ChatStatus.ACTIVE)
                .hiddenAt(null)
                .hiddenByUserId(null)
                .moderationReason(normalize(reason))
                .updatedAt(now)
                .build());
        logModeration(actorUserId, saved, "CHAT_RESTORED", reason);
        return AdminChatResponse.from(saved);
    }

    @Transactional
    public AdminChatResponse delete(Long actorUserId, Long chatId, String reason) {
        Chat chat = loadChat(chatId);
        LocalDateTime now = LocalDateTime.now();
        Chat saved = chatRepository.save(chat.toBuilder()
                .status(ChatStatus.DELETED)
                .deletedAt(now)
                .deletedByUserId(actorUserId)
                .moderationReason(normalize(reason))
                .updatedAt(now)
                .build());
        logModeration(actorUserId, saved, "CHAT_DELETED", reason);
        return AdminChatResponse.from(saved);
    }

    private Chat loadChat(Long chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> AdminManagementException.notFound("채팅"));
    }

    private void logModeration(Long actorUserId, Chat chat, String action, String reason) {
        chatModerationLogRepository.save(ChatModerationLog.builder()
                .chatId(chat.getId())
                .adminUserId(actorUserId)
                .action(action)
                .reason(normalize(reason))
                .detail("status=%s".formatted(chat.getStatus()))
                .createdAt(LocalDateTime.now())
                .build());

        adminAuditService.log(
                actorUserId,
                action,
                "CHAT",
                chat.getId(),
                "reason=%s".formatted(normalize(reason))
        );
    }

    private boolean matchesUser(Chat chat, String expected, Iterable<UserAccount> users) {
        String value = normalize(expected);
        if (value == null) {
            return true;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (chat.getUserId() != null && String.valueOf(chat.getUserId()).equals(value)) {
            return true;
        }
        return StreamSupport.stream(users.spliterator(), false)
                .filter(user -> user.getId().equals(chat.getUserId()))
                .anyMatch(user -> contains(user.getEmail(), lower)
                        || contains(user.getNickname(), lower)
                        || contains(user.getPhoneNumber(), lower));
    }

    private boolean containsIgnoreCase(String actual, String expected) {
        String value = normalize(expected);
        return value == null || contains(actual, value.toLowerCase(Locale.ROOT));
    }

    private boolean contains(String actual, String lower) {
        return actual != null && actual.toLowerCase(Locale.ROOT).contains(lower);
    }

    private boolean matchesStatus(Chat chat, String status) {
        String value = normalize(status);
        if (value == null || "ALL".equalsIgnoreCase(value)) {
            return true;
        }
        ChatStatus current = chat.getStatus() == null ? ChatStatus.ACTIVE : chat.getStatus();
        return current.name().equalsIgnoreCase(value);
    }

    private Comparator<Chat> chatComparator(String sort) {
        String value = normalize(sort);
        if ("status".equalsIgnoreCase(value)) {
            return Comparator.comparing(chat -> (chat.getStatus() == null ? ChatStatus.ACTIVE : chat.getStatus()).name());
        }
        return Comparator.comparing(Chat::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
