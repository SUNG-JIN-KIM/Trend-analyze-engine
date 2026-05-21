package com.gametrend.agent.auth.service;

import com.gametrend.agent.auth.entity.AuthType;
import com.gametrend.agent.auth.entity.UserAccount;
import com.gametrend.agent.auth.entity.UserRole;
import com.gametrend.agent.auth.entity.UserStatus;
import com.gametrend.agent.auth.exception.SocialLoginException;
import com.gametrend.agent.auth.oauth.OAuth2UserInfo;
import com.gametrend.agent.auth.oauth.OAuth2UserInfoFactory;
import com.gametrend.agent.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2UserAccountService {

    private final UserRepository userRepository;

    public UserAccount loadOrCreateUser(String registrationId, Map<String, Object> attributes) {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.create(registrationId, attributes);
        String provider = userInfo.authType().name();
        String providerId = stripToNull(userInfo.providerId());
        if (providerId == null) {
            throw new SocialLoginException("소셜 로그인 사용자 식별자를 찾을 수 없습니다.");
        }

        return userRepository.findByProviderAndProviderId(provider, providerId)
                .map(user -> updateSocialProfile(user, userInfo))
                .orElseGet(() -> findByEmailOrCreate(userInfo));
    }

    private UserAccount findByEmailOrCreate(OAuth2UserInfo userInfo) {
        String email = normalizeEmail(userInfo.email());
        if (email == null) {
            return createSocialUser(userInfo, null);
        }

        return userRepository.findByEmail(email)
                .map(user -> linkSocialAccount(user, userInfo))
                .orElseGet(() -> createSocialUser(userInfo, email));
    }

    private UserAccount linkSocialAccount(UserAccount user, OAuth2UserInfo userInfo) {
        String provider = userInfo.authType().name();
        String providerId = stripToNull(userInfo.providerId());
        if (hasValue(user.getProvider())
                && hasValue(user.getProviderId())
                && (!provider.equals(user.getProvider()) || !providerId.equals(user.getProviderId()))) {
            throw new SocialLoginException("이미 다른 소셜 계정으로 연결된 이메일입니다.");
        }
        return updateSocialProfile(user, userInfo);
    }

    private UserAccount updateSocialProfile(UserAccount user, OAuth2UserInfo userInfo) {
        LocalDateTime now = LocalDateTime.now();
        String email = normalizeEmail(userInfo.email());
        return userRepository.save(user.toBuilder()
                .email(email == null ? user.getEmail() : email)
                .nickname(firstNonBlank(userInfo.nickname(), user.getNickname(), userInfo.authType().name() + " 사용자"))
                .provider(userInfo.authType().name())
                .providerId(stripToNull(userInfo.providerId()))
                .profileImageUrl(stripToNull(userInfo.profileImageUrl()))
                .emailVerified(userInfo.emailVerified())
                .authType(userInfo.authType())
                .updatedAt(now)
                .build());
    }

    private UserAccount createSocialUser(OAuth2UserInfo userInfo, String email) {
        LocalDateTime now = LocalDateTime.now();
        AuthType authType = userInfo.authType();
        return userRepository.save(UserAccount.builder()
                .email(email)
                .passwordHash("")
                .nickname(firstNonBlank(userInfo.nickname(), authType.name() + " 사용자"))
                .role(UserRole.USER)
                .phoneVerified(false)
                .status(UserStatus.ACTIVE)
                .provider(authType.name())
                .providerId(stripToNull(userInfo.providerId()))
                .profileImageUrl(stripToNull(userInfo.profileImageUrl()))
                .emailVerified(userInfo.emailVerified())
                .authType(authType)
                .failedLoginCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private String normalizeEmail(String email) {
        String value = stripToNull(email);
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = stripToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return "소셜 사용자";
    }

    private String stripToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
