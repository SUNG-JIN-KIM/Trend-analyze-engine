package com.gametrend.agent.auth.oauth;

import com.gametrend.agent.auth.entity.AuthType;

import java.util.Map;

public class NaverOAuth2UserInfo extends OAuth2UserInfo {

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public AuthType authType() {
        return AuthType.NAVER;
    }

    @Override
    public String providerId() {
        return stringValue(response().get("id"));
    }

    @Override
    public String email() {
        return stringValue(response().get("email"));
    }

    @Override
    public String nickname() {
        String nickname = stringValue(response().get("nickname"));
        return nickname == null ? stringValue(response().get("name")) : nickname;
    }

    @Override
    public String profileImageUrl() {
        return stringValue(response().get("profile_image"));
    }

    private Map<String, Object> response() {
        return nestedMap("response");
    }
}
