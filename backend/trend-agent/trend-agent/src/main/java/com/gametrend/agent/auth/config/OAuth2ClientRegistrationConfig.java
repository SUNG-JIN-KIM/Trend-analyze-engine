package com.gametrend.agent.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class OAuth2ClientRegistrationConfig {

    private static final String REDIRECT_URI = "{baseUrl}/login/oauth2/code/{registrationId}";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(Environment environment) {
        return new InMemoryClientRegistrationRepository(
                googleRegistration(environment),
                kakaoRegistration(environment),
                naverRegistration(environment)
        );
    }

    private ClientRegistration googleRegistration(Environment environment) {
        return ClientRegistration.withRegistrationId("google")
                .clientId(property(environment, "spring.security.oauth2.client.registration.google.client-id",
                        "google-client-id-not-configured"))
                .clientSecret(property(environment, "spring.security.oauth2.client.registration.google.client-secret",
                        "google-client-secret-not-configured"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(REDIRECT_URI)
                .scope("email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
    }

    private ClientRegistration kakaoRegistration(Environment environment) {
        String clientSecret = property(environment, "spring.security.oauth2.client.registration.kakao.client-secret", "");

        return ClientRegistration.withRegistrationId("kakao")
                .clientId(property(environment, "spring.security.oauth2.client.registration.kakao.client-id",
                        "kakao-client-id-not-configured"))
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(REDIRECT_URI)
                .scope("profile_nickname", "account_email")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
    }

    private ClientRegistration naverRegistration(Environment environment) {
        return ClientRegistration.withRegistrationId("naver")
                .clientId(property(environment, "spring.security.oauth2.client.registration.naver.client-id",
                        "naver-client-id-not-configured"))
                .clientSecret(property(environment, "spring.security.oauth2.client.registration.naver.client-secret",
                        "naver-client-secret-not-configured"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(REDIRECT_URI)
                .scope("name", "email")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me")
                .userNameAttributeName("response")
                .clientName("Naver")
                .build();
    }

    private String property(Environment environment, String key, String defaultValue) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.strip();
    }
}
