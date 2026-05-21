package com.gametrend.agent.auth.oauth;

import com.gametrend.agent.auth.config.AppProperties;
import com.gametrend.agent.auth.entity.UserAccount;
import com.gametrend.agent.auth.exception.SocialLoginException;
import com.gametrend.agent.auth.service.JwtTokenProvider;
import com.gametrend.agent.auth.service.OAuth2UserAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2UserAccountService oAuth2UserAccountService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauthUser = oauthToken.getPrincipal();
            UserAccount user = oAuth2UserAccountService.loadOrCreateUser(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthUser.getAttributes()
            );
            String accessToken = jwtTokenProvider.createAccessToken(user);
            getRedirectStrategy().sendRedirect(request, response, successRedirectUrl(accessToken));
        } catch (SocialLoginException exception) {
            getRedirectStrategy().sendRedirect(request, response, errorRedirectUrl(exception.getMessage()));
        } catch (RuntimeException exception) {
            getRedirectStrategy().sendRedirect(request, response, errorRedirectUrl("소셜 로그인 처리에 실패했습니다."));
        }
    }

    private String successRedirectUrl(String accessToken) {
        return UriComponentsBuilder.fromUriString(appProperties.frontendUrl())
                .path("/oauth/callback")
                .queryParam("token", accessToken)
                .build()
                .encode()
                .toUriString();
    }

    private String errorRedirectUrl(String message) {
        return UriComponentsBuilder.fromUriString(appProperties.frontendUrl())
                .path("/oauth/callback")
                .queryParam("error", "oauth_failed")
                .queryParam("message", message)
                .build()
                .encode()
                .toUriString();
    }
}
