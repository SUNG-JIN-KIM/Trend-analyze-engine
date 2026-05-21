package com.gametrend.agent.infrastructure.steam;

public class SteamClientException extends RuntimeException {

    public SteamClientException(String message) {
        super(message);
    }

    public SteamClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
