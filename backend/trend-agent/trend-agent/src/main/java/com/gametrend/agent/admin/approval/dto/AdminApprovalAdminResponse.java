package com.gametrend.agent.admin.approval.dto;

import com.gametrend.agent.admin.approval.AdminApprovalRequest;

import java.time.LocalDateTime;

public record AdminApprovalAdminResponse(
        Long id,
        Long userId,
        String requesterEmail,
        String requesterNickname,
        String requesterPhoneNumber,
        String status,
        String approvalEmailSentTo,
        String requestReason,
        String decisionReason,
        Long approvedByUserId,
        Long rejectedByUserId,
        LocalDateTime requestedAt,
        LocalDateTime tokenExpiresAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        LocalDateTime usedAt
) {

    public static AdminApprovalAdminResponse from(AdminApprovalRequest request) {
        return new AdminApprovalAdminResponse(
                request.getId(),
                request.getUserId(),
                request.getRequesterEmail(),
                request.getRequesterNickname(),
                request.getRequesterPhoneNumber(),
                request.getStatus().name(),
                request.getApprovalEmailSentTo(),
                request.getRequestReason(),
                request.getDecisionReason(),
                request.getApprovedByUserId(),
                request.getRejectedByUserId(),
                request.getRequestedAt(),
                request.getTokenExpiresAt(),
                request.getApprovedAt(),
                request.getRejectedAt(),
                request.getUsedAt()
        );
    }
}
