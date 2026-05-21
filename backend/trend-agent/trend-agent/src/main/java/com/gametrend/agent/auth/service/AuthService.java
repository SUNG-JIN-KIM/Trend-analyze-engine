package com.gametrend.agent.auth.service;

import com.gametrend.agent.auth.dto.AuthLoginRequest;
import com.gametrend.agent.auth.dto.AuthRegisterRequest;
import com.gametrend.agent.auth.dto.AuthTokenResponse;
import com.gametrend.agent.auth.dto.AuthUserResponse;
import com.gametrend.agent.auth.entity.AuthType;
import com.gametrend.agent.auth.entity.UserAccount;
import com.gametrend.agent.auth.entity.UserRole;
import com.gametrend.agent.auth.entity.UserStatus;
import com.gametrend.agent.auth.exception.AdminLoginDeniedException;
import com.gametrend.agent.auth.exception.DuplicateEmailException;
import com.gametrend.agent.auth.exception.InvalidCredentialException;
import com.gametrend.agent.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthTokenResponse register(AuthRegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        LocalDateTime now = LocalDateTime.now();
        UserAccount user = userRepository.save(UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(request.nickname().strip())
                .role(UserRole.USER)
                .phoneNumber(null)
                .phoneVerified(false)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .authType(AuthType.LOCAL)
                .failedLoginCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build());

        return createTokenResponse(user);
    }

    public AuthTokenResponse login(AuthLoginRequest request) {
        UserAccount user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialException();
        }

        UserAccount loggedInUser = updateLoginSuccess(user);

        return createTokenResponse(loggedInUser);
    }

    public AuthTokenResponse adminLogin(AuthLoginRequest request) {
        UserAccount user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialException();
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.OWNER) {
            throw new AdminLoginDeniedException();
        }

        return createTokenResponse(updateLoginSuccess(user));
    }

    private UserAccount updateLoginSuccess(UserAccount user) {
        return userRepository.save(user.toBuilder()
                .lastLoginAt(LocalDateTime.now())
                .failedLoginCount(0)
                .lockedUntil(null)
                .build());
    }

    private AuthTokenResponse createTokenResponse(UserAccount user) {
        return AuthTokenResponse.bearer(
                jwtTokenProvider.createAccessToken(user),
                AuthUserResponse.from(user)
        );
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
