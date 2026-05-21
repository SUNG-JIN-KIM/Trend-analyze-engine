package com.gametrend.agent.admin.approval.dto;

import com.gametrend.agent.admin.approval.AdminApprovalRequest;

import java.time.LocalDateTime;

public record AdminApprovalRequestResponse(
        Long id,
        Long userId,
        String requesterEmail,
        String requesterNickname,
        String status,
        String approvalEmailSentTo,
        LocalDateTime tokenExpiresAt,
        LocalDateTime requestedAt,
        String message
) {

    public static AdminApprovalRequestResponse from(AdminApprovalRequest request, String message) {
        return new AdminApprovalRequestResponse(
                request.getId(),
                request.getUserId(),
                request.getRequesterEmail(),
                request.getRequesterNickname(),
                request.getStatus().name(),
                request.getApprovalEmailSentTo(),
                request.getTokenExpiresAt(),
                request.getRequestedAt(),
                message
        );
    }
}
