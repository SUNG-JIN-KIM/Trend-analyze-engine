package com.gametrend.agent.admin.common;

import org.springframework.http.HttpStatus;

public class AdminManagementException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    private AdminManagementException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static AdminManagementException notFound(String targetName) {
        return new AdminManagementException(
                "ADMIN_TARGET_NOT_FOUND",
                "%s 대상을 찾을 수 없습니다.".formatted(targetName),
                HttpStatus.NOT_FOUND
        );
    }

    public static AdminManagementException forbidden(String message) {
        return new AdminManagementException(
                "ADMIN_ACTION_FORBIDDEN",
                message,
                HttpStatus.FORBIDDEN
        );
    }

    public static AdminManagementException invalidRequest(String message) {
        return new AdminManagementException(
                "ADMIN_INVALID_REQUEST",
                message,
                HttpStatus.BAD_REQUEST
        );
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
