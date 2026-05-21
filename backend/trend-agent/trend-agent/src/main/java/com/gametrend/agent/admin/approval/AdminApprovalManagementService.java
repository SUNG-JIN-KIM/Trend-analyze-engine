package com.gametrend.agent.admin.approval;

import com.gametrend.agent.admin.approval.dto.AdminApprovalAdminResponse;
import com.gametrend.agent.admin.audit.AdminAuditService;
import com.gametrend.agent.admin.common.AdminManagementException;
import com.gametrend.agent.admin.common.AdminPageResponse;
import com.gametrend.agent.auth.entity.UserAccount;
import com.gametrend.agent.auth.entity.UserRole;
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
public class AdminApprovalManagementService {

    private final AdminApprovalRequestRepository adminApprovalRequestRepository;
    private final UserRepository userRepository;
    private final AdminAuditService adminAuditService;

    public AdminPageResponse<AdminApprovalAdminResponse> searchRequests(
            String status,
            String search,
            int page,
            int size,
            String sort
    ) {
        var requests = StreamSupport.stream(adminApprovalRequestRepository.findAll().spliterator(), false)
                .map(this::withExpiredStatusIfNeeded)
                .filter(request -> matchesStatus(request, status))
                .filter(request -> matchesSearch(request, search))
                .sorted(requestComparator(sort))
                .map(AdminApprovalAdminResponse::from)
                .toList();

        return AdminPageResponse.of(requests, page, size);
    }

    @Transactional
    public AdminApprovalAdminResponse approve(Long actorUserId, Long requestId) {
        UserAccount actor = requireOwner(actorUserId);
        AdminApprovalRequest request = loadPendingRequest(requestId);
        UserAccount user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> AdminManagementException.notFound("승인 요청 사용자"));
        LocalDateTime now = LocalDateTime.now();

        userRepository.save(user.toBuilder()
                .role(UserRole.ADMIN)
                .updatedAt(now)
                .build());

        AdminApprovalRequest saved = adminApprovalRequestRepository.save(request.toBuilder()
                .status(AdminApprovalStatus.APPROVED)
                .approvedByUserId(actor.getId())
                .approvedAt(now)
                .usedAt(now)
                .updatedAt(now)
                .build());

        adminAuditService.log(
                actor.getId(),
                "ADMIN_APPROVAL_APPROVED",
                "USER",
                user.getId(),
                "requestId=%d,email=%s".formatted(saved.getId(), user.getEmail())
        );

        return AdminApprovalAdminResponse.from(saved);
    }

    @Transactional
    public AdminApprovalAdminResponse reject(Long actorUserId, Long requestId, String reason) {
        UserAccount actor = requireOwner(actorUserId);
        AdminApprovalRequest request = loadPendingRequest(requestId);
        LocalDateTime now = LocalDateTime.now();

        AdminApprovalRequest saved = adminApprovalRequestRepository.save(request.toBuilder()
                .status(AdminApprovalStatus.REJECTED)
                .decisionReason(normalize(reason))
                .rejectedByUserId(actor.getId())
                .rejectedAt(now)
                .usedAt(now)
                .updatedAt(now)
                .build());

        adminAuditService.log(
                actor.getId(),
                "ADMIN_APPROVAL_REJECTED",
                "ADMIN_APPROVAL_REQUEST",
                saved.getId(),
                "userId=%d,email=%s,reason=%s".formatted(
                        saved.getUserId(),
                        saved.getRequesterEmail(),
                        normalize(reason)
                )
        );

        return AdminApprovalAdminResponse.from(saved);
    }

    private AdminApprovalRequest loadPendingRequest(Long requestId) {
        AdminApprovalRequest request = adminApprovalRequestRepository.findById(requestId)
                .orElseThrow(() -> AdminManagementException.notFound("관리자 승인 요청"));
        request = withExpiredStatusIfNeeded(request);
        if (request.getStatus() != AdminApprovalStatus.PENDING || request.getUsedAt() != null) {
            throw AdminManagementException.invalidRequest("이미 처리되었거나 만료된 승인 요청입니다.");
        }
        return request;
    }

    private UserAccount requireOwner(Long actorUserId) {
        UserAccount actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> AdminManagementException.notFound("관리자 사용자"));
        if (actor.getRole() != UserRole.OWNER) {
            throw AdminManagementException.forbidden("관리자 승인/거절은 OWNER만 수행할 수 있습니다.");
        }
        return actor;
    }

    private AdminApprovalRequest withExpiredStatusIfNeeded(AdminApprovalRequest request) {
        if (request.getStatus() == AdminApprovalStatus.PENDING
                && request.getTokenExpiresAt() != null
                && !request.getTokenExpiresAt().isAfter(LocalDateTime.now())) {
            return adminApprovalRequestRepository.save(request.toBuilder()
                    .status(AdminApprovalStatus.EXPIRED)
                    .updatedAt(LocalDateTime.now())
                    .build());
        }
        return request;
    }

    private boolean matchesStatus(AdminApprovalRequest request, String status) {
        String expected = normalize(status);
        if (expected == null || "ALL".equalsIgnoreCase(expected)) {
            return true;
        }
        return request.getStatus().name().equalsIgnoreCase(expected);
    }

    private boolean matchesSearch(AdminApprovalRequest request, String search) {
        String expected = normalize(search);
        if (expected == null) {
            return true;
        }
        String lower = expected.toLowerCase(Locale.ROOT);
        return contains(request.getRequesterEmail(), lower)
                || contains(request.getRequesterNickname(), lower)
                || contains(request.getRequesterPhoneNumber(), lower);
    }

    private boolean contains(String value, String lower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lower);
    }

    private Comparator<AdminApprovalRequest> requestComparator(String sort) {
        String value = normalize(sort);
        if ("status".equalsIgnoreCase(value)) {
            return Comparator.comparing(request -> request.getStatus().name());
        }
        if ("expiresAt".equalsIgnoreCase(value)) {
            return Comparator.comparing(AdminApprovalRequest::getTokenExpiresAt, Comparator.nullsLast(LocalDateTime::compareTo));
        }
        return Comparator.comparing(AdminApprovalRequest::getRequestedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
