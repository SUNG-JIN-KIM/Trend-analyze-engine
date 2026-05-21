package com.gametrend.agent.auth.exception;

public class AuthRequiredException extends RuntimeException {

    public AuthRequiredException(String message) {
        super(message);
    }
}
