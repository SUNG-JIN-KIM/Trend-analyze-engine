package com.gametrend.agent.admin.approval;

import com.gametrend.agent.auth.entity.AuthType;
import com.gametrend.agent.auth.entity.UserAccount;
import com.gametrend.agent.auth.entity.UserRole;
import com.gametrend.agent.auth.entity.UserStatus;
import com.gametrend.agent.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AdminApprovalServiceTest {

    private static final Pattern APPROVE_TOKEN_PATTERN = Pattern.compile("/admin/approval/approve\\?token=([^\\s]+)");

    private final AdminApprovalService adminApprovalService;
    private final UserRepository userRepository;
    private final CapturingAdminApprovalEmailSender emailSender;

    @Autowired
    AdminApprovalServiceTest(
            AdminApprovalService adminApprovalService,
            UserRepository userRepository,
            CapturingAdminApprovalEmailSender emailSender
    ) {
        this.adminApprovalService = adminApprovalService;
        this.userRepository = userRepository;
        this.emailSender = emailSender;
    }

    @Test
    void requestApprovalSendsEmailToFixedApprovalEmailAndApprovesTokenOnlyOnce() {
        UserAccount user = saveUser();

        var response = adminApprovalService.requestApproval(user.getId(), "관리자 권한이 필요합니다.");
        String token = extractApproveToken(emailSender.body);

        assertEquals("ksjcloud98@gmail.com", response.approvalEmailSentTo());
        assertEquals("ksjcloud98@gmail.com", emailSender.to);
        assertNotNull(token);

        var approved = adminApprovalService.approve(token);

        assertEquals("APPROVED", approved.status());
        assertEquals(UserRole.ADMIN, userRepository.findById(user.getId()).orElseThrow().getRole());
        assertThrows(AdminApprovalException.class, () -> adminApprovalService.approve(token));
    }

    private UserAccount saveUser() {
        LocalDateTime now = LocalDateTime.now();
        String suffix = String.valueOf(System.nanoTime());
        return userRepository.save(UserAccount.builder()
                .email("approval-" + suffix + "@example.com")
                .passwordHash("encoded-password")
                .nickname("approval-user")
                .role(UserRole.USER)
                .phoneNumber("010" + suffix.substring(Math.max(0, suffix.length() - 8)))
                .phoneVerified(true)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .authType(AuthType.LOCAL)
                .failedLoginCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private String extractApproveToken(String body) {
        Matcher matcher = APPROVE_TOKEN_PATTERN.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("승인 링크에서 토큰을 찾지 못했습니다. body=" + body);
        }
        return matcher.group(1);
    }

    @TestConfiguration
    static class EmailSenderTestConfig {

        @Bean
        @Primary
        CapturingAdminApprovalEmailSender capturingAdminApprovalEmailSender() {
            return new CapturingAdminApprovalEmailSender();
        }
    }

    static class CapturingAdminApprovalEmailSender implements AdminApprovalEmailSender {
        private String to;
        private String subject;
        private String body;

        @Override
        public void send(String to, String subject, String body) {
            this.to = to;
            this.subject = subject;
            this.body = body;
        }
    }
}
