package com.gametrend.agent.admin.approval;

import org.springframework.http.HttpStatus;

public class AdminApprovalException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    private AdminApprovalException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static AdminApprovalException onlyUserCanRequest() {
        return new AdminApprovalException(
                "ADMIN_APPROVAL_ONLY_USER_CAN_REQUEST",
                "일반 사용자만 관리자 권한을 신청할 수 있습니다.",
                HttpStatus.BAD_REQUEST
        );
    }

    public static AdminApprovalException alreadyPending() {
        return new AdminApprovalException(
                "ADMIN_APPROVAL_ALREADY_PENDING",
                "이미 처리 대기 중인 관리자 승인 요청이 있습니다.",
                HttpStatus.CONFLICT
        );
    }

    public static AdminApprovalException invalidToken() {
        return new AdminApprovalException(
                "ADMIN_APPROVAL_INVALID_TOKEN",
                "관리자 승인 토큰이 올바르지 않습니다.",
                HttpStatus.BAD_REQUEST
        );
    }

    public static AdminApprovalException tokenExpired() {
        return new AdminApprovalException(
                "ADMIN_APPROVAL_TOKEN_EXPIRED",
                "관리자 승인 토큰이 만료되었습니다.",
                HttpStatus.BAD_REQUEST
        );
    }

    public static AdminApprovalException tokenAlreadyUsed() {
        return new AdminApprovalException(
                "ADMIN_APPROVAL_TOKEN_ALREADY_USED",
                "이미 사용된 관리자 승인 토큰입니다.",
                HttpStatus.CONFLICT
        );
    }

    public static AdminApprovalException emailSendFailed() {
        return new AdminApprovalException(
                "ADMIN_APPROVAL_EMAIL_SEND_FAILED",
                "관리자 승인 이메일 발송에 실패했습니다. 메일 설정을 확인해주세요.",
                HttpStatus.BAD_GATEWAY
        );
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
