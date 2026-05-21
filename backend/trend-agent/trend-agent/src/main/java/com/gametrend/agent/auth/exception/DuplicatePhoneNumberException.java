package com.gametrend.agent.auth.exception;

public class DuplicatePhoneNumberException extends RuntimeException {

    public DuplicatePhoneNumberException() {
        super("이미 사용 중인 전화번호입니다.");
    }
}
