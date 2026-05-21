package com.gametrend.agent.auth.dto;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        AuthUserResponse user
) {

    public static AuthTokenResponse bearer(String accessToken, AuthUserResponse user) {
        return new AuthTokenResponse(accessToken, "Bearer", user);
    }
}
