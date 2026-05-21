package com.gametrend.agent.auth.oauth;

import com.gametrend.agent.auth.config.AppProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AppProperties appProperties;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        getRedirectStrategy().sendRedirect(request, response, errorRedirectUrl());
    }

    private String errorRedirectUrl() {
        return UriComponentsBuilder.fromUriString(appProperties.frontendUrl())
                .path("/oauth/callback")
                .queryParam("error", "oauth_failed")
                .build()
                .encode()
                .toUriString();
    }
}
