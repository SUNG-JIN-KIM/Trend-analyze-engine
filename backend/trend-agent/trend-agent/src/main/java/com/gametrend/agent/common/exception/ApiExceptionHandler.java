package com.gametrend.agent.common.exception;

import com.gametrend.agent.admin.approval.AdminApprovalException;
import com.gametrend.agent.admin.common.AdminManagementException;
import com.gametrend.agent.infrastructure.steam.SteamClientException;
import com.gametrend.agent.auth.exception.AdminLoginDeniedException;
import com.gametrend.agent.auth.exception.AuthRequiredException;
import com.gametrend.agent.auth.exception.DuplicateEmailException;
import com.gametrend.agent.auth.exception.DuplicatePhoneNumberException;
import com.gametrend.agent.auth.exception.InvalidCredentialException;
import com.gametrend.agent.auth.exception.SocialLoginException;
import com.gametrend.agent.auth.phone.PhoneVerificationException;
import com.gametrend.agent.conversation.exception.ConversationNotFoundException;
import com.gametrend.agent.onboarding.exception.OnboardingHistoryNotFoundException;
import com.gametrend.agent.project.exception.UserProjectNotFoundException;
import com.gametrend.agent.trend.exception.TrendGameNotFoundException;
import com.gametrend.agent.youtube.service.YoutubeTrendException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthRequiredException.class)
    public ResponseEntity<ErrorResponse> handleAuthRequiredException(AuthRequiredException exception) {
        ErrorResponse response = ErrorResponse.of(
                "AUTH_REQUIRED",
                "로그인이 필요한 기능입니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(InvalidCredentialException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialException(InvalidCredentialException exception) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_CREDENTIALS",
                "로그인 정보가 올바르지 않습니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AdminLoginDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAdminLoginDeniedException(AdminLoginDeniedException exception) {
        ErrorResponse response = ErrorResponse.of(
                "ADMIN_LOGIN_DENIED",
                "관리자 로그인이 허용되지 않은 계정입니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmailException(DuplicateEmailException exception) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_EMAIL",
                "이미 가입된 이메일입니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DuplicatePhoneNumberException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePhoneNumberException(DuplicatePhoneNumberException exception) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_PHONE_NUMBER",
                "이미 사용 중인 전화번호입니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(PhoneVerificationException.class)
    public ResponseEntity<ErrorResponse> handlePhoneVerificationException(PhoneVerificationException exception) {
        ErrorResponse response = ErrorResponse.of(
                exception.code(),
                resolveMessage(exception),
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(exception.status()).body(response);
    }

    @ExceptionHandler(AdminApprovalException.class)
    public ResponseEntity<ErrorResponse> handleAdminApprovalException(AdminApprovalException exception) {
        ErrorResponse response = ErrorResponse.of(
                exception.code(),
                resolveMessage(exception),
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(exception.status()).body(response);
    }

    @ExceptionHandler(AdminManagementException.class)
    public ResponseEntity<ErrorResponse> handleAdminManagementException(AdminManagementException exception) {
        ErrorResponse response = ErrorResponse.of(
                exception.code(),
                resolveMessage(exception),
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(exception.status()).body(response);
    }

    @ExceptionHandler(SocialLoginException.class)
    public ResponseEntity<ErrorResponse> handleSocialLoginException(SocialLoginException exception) {
        ErrorResponse response = ErrorResponse.of(
                "SOCIAL_LOGIN_FAILED",
                "소셜 로그인 처리에 실패했습니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(SteamClientException.class)
    public ResponseEntity<ErrorResponse> handleSteamClientException(SteamClientException exception) {
        ErrorResponse response = ErrorResponse.of(
                "STEAM_CLIENT_ERROR",
                "Steam 리뷰 데이터를 조회하지 못했습니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(UserProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserProjectNotFoundException(UserProjectNotFoundException exception) {
        ErrorResponse response = ErrorResponse.of(
                "PROJECT_NOT_FOUND",
                "프로젝트를 찾을 수 없습니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConversationNotFoundException(ConversationNotFoundException exception) {
        ErrorResponse response = ErrorResponse.of(
                "CONVERSATION_NOT_FOUND",
                "대화를 찾을 수 없습니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(OnboardingHistoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOnboardingHistoryNotFoundException(
            OnboardingHistoryNotFoundException exception
    ) {
        ErrorResponse response = ErrorResponse.of(
                "ONBOARDING_HISTORY_NOT_FOUND",
                "온보딩 분석 이력을 찾을 수 없습니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(TrendGameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTrendGameNotFoundException(
            TrendGameNotFoundException exception
    ) {
        ErrorResponse response = ErrorResponse.of(
                "TREND_GAME_NOT_FOUND",
                "트렌드 게임을 찾을 수 없습니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(YoutubeTrendException.class)
    public ResponseEntity<ErrorResponse> handleYoutubeTrendException(YoutubeTrendException exception) {
        ErrorResponse response = ErrorResponse.of(
                "YOUTUBE_TREND_ERROR",
                "YouTube 트렌드 데이터를 처리하지 못했습니다.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception
    ) {
        String message = "keyword".equals(exception.getParameterName())
                ? "게임 키워드를 입력해주세요."
                : "%s 파라미터는 필수입니다.".formatted(exception.getParameterName());
        ErrorResponse response = ErrorResponse.of(
                "MISSING_REQUEST_PARAMETER",
                message,
                List.of(message)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
                .toList();

        ErrorResponse response = ErrorResponse.of(
                "VALIDATION_ERROR",
                "Request validation failed.",
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected server error occurred.",
                List.of(resolveMessage(exception))
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String resolveMessage(Exception exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}
