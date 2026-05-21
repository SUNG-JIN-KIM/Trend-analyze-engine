package com.gametrend.agent.auth.phone;

import org.springframework.http.HttpStatus;

public class PhoneVerificationException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    private PhoneVerificationException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static PhoneVerificationException invalidPhoneNumber() {
        return new PhoneVerificationException(
                "INVALID_PHONE_NUMBER",
                "전화번호 형식이 올바르지 않습니다.",
                HttpStatus.BAD_REQUEST
        );
    }

    public static PhoneVerificationException resendTooSoon(long seconds) {
        return new PhoneVerificationException(
                "PHONE_CODE_RESEND_TOO_SOON",
                "%d초 후 다시 인증번호를 요청할 수 있습니다.".formatted(Math.max(seconds, 1)),
                HttpStatus.TOO_MANY_REQUESTS
        );
    }

    public static PhoneVerificationException maxResendExceeded() {
        return new PhoneVerificationException(
                "PHONE_CODE_RESEND_LIMIT_EXCEEDED",
                "인증번호 재전송 가능 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.",
                HttpStatus.TOO_MANY_REQUESTS
        );
    }

    public static PhoneVerificationException dailyRequestLimitExceeded() {
        return new PhoneVerificationException(
                "PHONE_CODE_DAILY_LIMIT_EXCEEDED",
                "오늘 요청 가능한 전화번호 인증번호 발송 횟수를 초과했습니다.",
                HttpStatus.TOO_MANY_REQUESTS
        );
    }

    public static PhoneVerificationException smsSendFailed() {
        return new PhoneVerificationException(
                "SMS_SEND_FAILED",
                "인증번호 문자 발송에 실패했습니다. 잠시 후 다시 시도해주세요.",
                HttpStatus.BAD_GATEWAY
        );
    }

    public static PhoneVerificationException smsConfigMissing() {
        return new PhoneVerificationException(
                "SMS_CONFIG_MISSING",
                "문자 발송 설정이 아직 완료되지 않았습니다. 관리자에게 문의해주세요.",
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    public static PhoneVerificationException verificationNotFound() {
        return new PhoneVerificationException(
                "PHONE_VERIFICATION_NOT_FOUND",
                "전화번호 인증 요청을 찾을 수 없습니다. 인증번호를 먼저 요청해주세요.",
                HttpStatus.BAD_REQUEST
        );
    }

    public static PhoneVerificationException expired() {
        return new PhoneVerificationException(
                "PHONE_CODE_EXPIRED",
                "전화번호 인증번호가 만료되었습니다. 새 인증번호를 요청해주세요.",
                HttpStatus.BAD_REQUEST
        );
    }

    public static PhoneVerificationException maxAttemptsExceeded() {
        return new PhoneVerificationException(
                "PHONE_CODE_ATTEMPT_LIMIT_EXCEEDED",
                "인증번호 입력 가능 횟수를 초과했습니다. 새 인증번호를 요청해주세요.",
                HttpStatus.TOO_MANY_REQUESTS
        );
    }

    public static PhoneVerificationException invalidCode() {
        return new PhoneVerificationException(
                "INVALID_PHONE_CODE",
                "전화번호 인증번호가 올바르지 않습니다.",
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
