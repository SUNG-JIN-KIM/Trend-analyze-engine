package com.gametrend.agent.auth.oauth;

import com.gametrend.agent.auth.entity.AuthType;

import java.util.Map;

public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public AuthType authType() {
        return AuthType.GOOGLE;
    }

    @Override
    public String providerId() {
        return stringValue(attributes.get("sub"));
    }

    @Override
    public String email() {
        return stringValue(attributes.get("email"));
    }

    @Override
    public String nickname() {
        return stringValue(attributes.get("name"));
    }

    @Override
    public String profileImageUrl() {
        return stringValue(attributes.get("picture"));
    }

    @Override
    public boolean emailVerified() {
        Object value = attributes.get("email_verified");
        if (value instanceof Boolean verified) {
            return verified;
        }
        return Boolean.parseBoolean(stringValue(value));
    }
}
