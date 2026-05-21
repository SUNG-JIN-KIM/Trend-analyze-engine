package com.gametrend.agent.auth.oauth;

import com.gametrend.agent.auth.entity.AuthType;

import java.util.Map;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public AuthType authType() {
        return AuthType.KAKAO;
    }

    @Override
    public String providerId() {
        return stringValue(attributes.get("id"));
    }

    @Override
    public String email() {
        return stringValue(kakaoAccount().get("email"));
    }

    @Override
    public String nickname() {
        return stringValue(profile().get("nickname"));
    }

    @Override
    public String profileImageUrl() {
        return stringValue(profile().get("profile_image_url"));
    }

    @Override
    public boolean emailVerified() {
        Object value = kakaoAccount().get("is_email_verified");
        if (value instanceof Boolean verified) {
            return verified;
        }
        return Boolean.parseBoolean(stringValue(value));
    }

    private Map<String, Object> kakaoAccount() {
        return nestedMap("kakao_account");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> profile() {
        Object value = kakaoAccount().get("profile");
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
