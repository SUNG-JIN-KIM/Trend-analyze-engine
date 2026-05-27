package com.gametrend.agent.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.auth.config.AuthProperties;
import com.gametrend.agent.auth.entity.UserAccount;
import com.gametrend.agent.auth.entity.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    @Test
    void tokenIssuedBeforeServerRestartIsRejected() {
        AuthProperties authProperties = new AuthProperties("test-secret-for-restart", 86_400_000L);
        ObjectMapper objectMapper = new ObjectMapper();
        JwtTokenProvider firstServer = new JwtTokenProvider(authProperties, objectMapper);
        JwtTokenProvider restartedServer = new JwtTokenProvider(authProperties, objectMapper);

        String token = firstServer.createAccessToken(UserAccount.builder()
                .id(1L)
                .email("user@example.com")
                .nickname("user")
                .role(UserRole.USER)
                .build());

        assertTrue(firstServer.parseAndValidate(token).isPresent());
        assertTrue(restartedServer.parseAndValidate(token).isEmpty());
    }
}
