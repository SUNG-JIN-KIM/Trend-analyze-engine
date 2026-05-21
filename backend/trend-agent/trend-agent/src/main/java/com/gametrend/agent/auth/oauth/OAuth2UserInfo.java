package com.gametrend.agent.auth.oauth;

import com.gametrend.agent.auth.entity.AuthType;

import java.util.Map;

public abstract class OAuth2UserInfo {

    protected final Map<String, Object> attributes;

    protected OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public abstract AuthType authType();

    public abstract String providerId();

    public abstract String email();

    public abstract String nickname();

    public abstract String profileImageUrl();

    public boolean emailVerified() {
        return false;
    }

    protected String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).strip();
        return text.isBlank() ? null : text;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> nestedMap(String key) {
        Object value = attributes.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
