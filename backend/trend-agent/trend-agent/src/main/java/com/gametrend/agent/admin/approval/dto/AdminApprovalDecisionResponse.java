package com.gametrend.agent.admin.approval.dto;

import com.gametrend.agent.admin.approval.AdminApprovalRequest;

import java.time.LocalDateTime;

public record AdminApprovalDecisionResponse(
        Long requestId,
        Long userId,
        String requesterEmail,
        String status,
        LocalDateTime decidedAt,
        String message
) {

    public static AdminApprovalDecisionResponse approved(AdminApprovalRequest request) {
        return new AdminApprovalDecisionResponse(
                request.getId(),
                request.getUserId(),
                request.getRequesterEmail(),
                request.getStatus().name(),
                request.getApprovedAt(),
                "관리자 승인이 완료되었습니다."
        );
    }

    public static AdminApprovalDecisionResponse rejected(AdminApprovalRequest request) {
        return new AdminApprovalDecisionResponse(
                request.getId(),
                request.getUserId(),
                request.getRequesterEmail(),
                request.getStatus().name(),
                request.getRejectedAt(),
                "관리자 승인 요청이 거절되었습니다."
        );
    }
}
