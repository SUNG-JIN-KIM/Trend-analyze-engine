package com.gametrend.agent.admin.dashboard;

import com.gametrend.agent.admin.approval.AdminApprovalRequestRepository;
import com.gametrend.agent.admin.approval.AdminApprovalStatus;
import com.gametrend.agent.admin.chat.ChatRepository;
import com.gametrend.agent.admin.chat.ChatReportRepository;
import com.gametrend.agent.admin.chat.ChatStatus;
import com.gametrend.agent.admin.dashboard.dto.AdminDashboardResponse;
import com.gametrend.agent.auth.entity.UserAccount;
import com.gametrend.agent.auth.entity.UserRole;
import com.gametrend.agent.auth.repository.UserRepository;
import com.gametrend.agent.conversation.entity.ConversationStatus;
import com.gametrend.agent.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final AdminApprovalRequestRepository adminApprovalRequestRepository;
    private final ChatRepository chatRepository;
    private final ChatReportRepository chatReportRepository;
    private final ConversationRepository conversationRepository;

    public AdminDashboardResponse getDashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime recentLoginCutoff = LocalDateTime.now().minusHours(24);

        var users = StreamSupport.stream(userRepository.findAll().spliterator(), false).toList();

        var conversations = StreamSupport.stream(conversationRepository.findAll().spliterator(), false).toList();

        return new AdminDashboardResponse(
                users.size(),
                countByRole(users, UserRole.USER),
                countByRole(users, UserRole.ADMIN),
                countByRole(users, UserRole.OWNER),
                StreamSupport.stream(adminApprovalRequestRepository.findAll().spliterator(), false)
                        .filter(request -> request.getStatus() == AdminApprovalStatus.PENDING)
                        .count(),
                users.stream()
                        .filter(user -> user.getCreatedAt() != null && !user.getCreatedAt().isBefore(todayStart))
                        .count(),
                users.stream()
                        .filter(user -> user.getLastLoginAt() != null && !user.getLastLoginAt().isBefore(recentLoginCutoff))
                        .count(),
                chatReportRepository.count(),
                StreamSupport.stream(chatRepository.findAll().spliterator(), false)
                        .filter(chat -> chat.getStatus() == ChatStatus.HIDDEN)
                        .count(),
                conversations.size(),
                chatReportRepository.count(),
                conversations.stream()
                        .filter(conversation -> conversation.statusOrActive() == ConversationStatus.HIDDEN)
                        .count()
        );
    }

    private long countByRole(Iterable<UserAccount> users, UserRole role) {
        return StreamSupport.stream(users.spliterator(), false)
                .filter(user -> user.getRole() == role)
                .count();
    }
}
