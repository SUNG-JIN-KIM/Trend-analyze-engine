package com.gametrend.agent.admin.approval;

import com.gametrend.agent.admin.approval.dto.AdminApprovalDecisionResponse;
import com.gametrend.agent.admin.approval.dto.AdminApprovalRequestResponse;
import com.gametrend.agent.admin.audit.AdminAuditService;
import com.gametrend.agent.admin.config.AdminApprovalProperties;
import com.gametrend.agent.auth.entity.UserAccount;
import com.gametrend.agent.auth.entity.UserRole;
import com.gametrend.agent.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminApprovalService {

    private final AdminApprovalRequestRepository adminApprovalRequestRepository;
    private final UserRepository userRepository;
    private final AdminApprovalTokenService tokenService;
    private final AdminApprovalEmailSender emailSender;
    private final AdminApprovalProperties properties;
    private final AdminAuditService adminAuditService;

    @Transactional
    public AdminApprovalRequestResponse requestApproval(Long userId, String reason) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(AdminApprovalException::onlyUserCanRequest);
        if (user.getRole() != UserRole.USER) {
            throw AdminApprovalException.onlyUserCanRequest();
        }

        LocalDateTime now = LocalDateTime.now();
        adminApprovalRequestRepository.findActivePendingByUserId(user.getId(), now)
                .ifPresent(existing -> {
                    throw AdminApprovalException.alreadyPending();
                });

        String rawToken = tokenService.generateRawToken();
        String tokenHash = tokenService.hashToken(rawToken);
        LocalDateTime expiresAt = now.plusMinutes(properties.approvalTokenExpireMinutes());

        AdminApprovalRequest saved = adminApprovalRequestRepository.save(AdminApprovalRequest.builder()
                .userId(user.getId())
                .requesterEmail(user.getEmail())
                .requesterNickname(user.getNickname())
                .requesterPhoneNumber(user.getPhoneNumber())
                .status(AdminApprovalStatus.PENDING)
                .tokenHash(tokenHash)
                .tokenExpiresAt(expiresAt)
                .approvalEmailSentTo(properties.approvalEmail())
                .requestReason(stripToNull(reason))
                .requestedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());

        emailSender.send(
                properties.approvalEmail(),
                "[Game Trend] 관리자 권한 승인 요청",
                buildApprovalEmailBody(saved, rawToken)
        );

        adminAuditService.log(
                null,
                "ADMIN_APPROVAL_REQUESTED",
                "ADMIN_APPROVAL_REQUEST",
                saved.getId(),
                "userId=%d,email=%s,approvalEmail=%s".formatted(
                        user.getId(),
                        user.getEmail(),
                        properties.approvalEmail()
                )
        );

        return AdminApprovalRequestResponse.from(
                saved,
                "관리자 승인 요청을 보냈습니다. 승인 담당자에게 이메일이 발송되었습니다."
        );
    }

    @Transactional
    public AdminApprovalDecisionResponse approve(String token) {
        AdminApprovalRequest request = loadUsableRequest(token);
        UserAccount user = userRepository.findById(request.getUserId())
                .orElseThrow(AdminApprovalException::invalidToken);
        LocalDateTime now = LocalDateTime.now();

        userRepository.save(user.toBuilder()
                .role(UserRole.ADMIN)
                .updatedAt(now)
                .build());

        AdminApprovalRequest approved = adminApprovalRequestRepository.save(request.toBuilder()
                .status(AdminApprovalStatus.APPROVED)
                .approvedAt(now)
                .usedAt(now)
                .updatedAt(now)
                .build());

        adminAuditService.log(
                null,
                "ADMIN_APPROVAL_APPROVED",
                "USER",
                user.getId(),
                "requestId=%d,email=%s".formatted(approved.getId(), user.getEmail())
        );

        return AdminApprovalDecisionResponse.approved(approved);
    }

    @Transactional
    public AdminApprovalDecisionResponse reject(String token, String reason) {
        AdminApprovalRequest request = loadUsableRequest(token);
        LocalDateTime now = LocalDateTime.now();

        AdminApprovalRequest rejected = adminApprovalRequestRepository.save(request.toBuilder()
                .status(AdminApprovalStatus.REJECTED)
                .decisionReason(stripToNull(reason))
                .rejectedAt(now)
                .usedAt(now)
                .updatedAt(now)
                .build());

        adminAuditService.log(
                null,
                "ADMIN_APPROVAL_REJECTED",
                "ADMIN_APPROVAL_REQUEST",
                rejected.getId(),
                "userId=%d,email=%s,reason=%s".formatted(
                        rejected.getUserId(),
                        rejected.getRequesterEmail(),
                        stripToNull(reason)
                )
        );

        return AdminApprovalDecisionResponse.rejected(rejected);
    }

    private AdminApprovalRequest loadUsableRequest(String rawToken) {
        String tokenHash = tokenService.hashToken(rawToken);
        AdminApprovalRequest request = adminApprovalRequestRepository.findByTokenHash(tokenHash)
                .orElseThrow(AdminApprovalException::invalidToken);

        LocalDateTime now = LocalDateTime.now();
        if (request.getUsedAt() != null || request.getStatus() != AdminApprovalStatus.PENDING) {
            throw AdminApprovalException.tokenAlreadyUsed();
        }
        if (!request.getTokenExpiresAt().isAfter(now)) {
            adminApprovalRequestRepository.save(request.toBuilder()
                    .status(AdminApprovalStatus.EXPIRED)
                    .updatedAt(now)
                    .build());
            throw AdminApprovalException.tokenExpired();
        }
        return request;
    }

    private String buildApprovalEmailBody(AdminApprovalRequest request, String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String approveUrl = properties.approvalLinkBaseUrl()
                + "/admin/approval/approve?token=" + encodedToken;
        String rejectUrl = properties.approvalLinkBaseUrl()
                + "/admin/approval/reject?token=" + encodedToken;

        return """
                관리자 권한 승인 요청이 도착했습니다.

                요청 ID: %d
                사용자 ID: %d
                이메일: %s
                닉네임: %s
                전화번호: %s
                요청 사유: %s
                만료 시간: %s

                승인 링크:
                %s

                거절 링크:
                %s
                """.formatted(
                request.getId(),
                request.getUserId(),
                request.getRequesterEmail(),
                request.getRequesterNickname(),
                displayValue(request.getRequesterPhoneNumber()),
                displayValue(request.getRequestReason()),
                request.getTokenExpiresAt(),
                approveUrl,
                rejectUrl
        );
    }

    private String displayValue(String value) {
        String stripped = stripToNull(value);
        return stripped == null ? "-" : stripped;
    }

    private String stripToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
