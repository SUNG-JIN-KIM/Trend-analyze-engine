package com.gametrend.agent.auth.exception;

public class AdminLoginDeniedException extends RuntimeException {

    public AdminLoginDeniedException() {
        super("관리자 권한이 있는 계정만 관리자 로그인을 사용할 수 있습니다.");
    }
}
