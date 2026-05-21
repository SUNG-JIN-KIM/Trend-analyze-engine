package com.gametrend.agent.auth.oauth;

import com.gametrend.agent.auth.exception.SocialLoginException;

import java.util.Locale;
import java.util.Map;

public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {
    }

    public static OAuth2UserInfo create(String registrationId, Map<String, Object> attributes) {
        String provider = registrationId == null ? "" : registrationId.toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            case "naver" -> new NaverOAuth2UserInfo(attributes);
            default -> throw new SocialLoginException("지원하지 않는 소셜 로그인 provider입니다: " + registrationId);
        };
    }
}
