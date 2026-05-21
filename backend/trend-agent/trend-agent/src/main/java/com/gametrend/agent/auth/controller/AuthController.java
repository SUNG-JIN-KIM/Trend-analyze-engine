package com.gametrend.agent.auth.controller;

import com.gametrend.agent.auth.dto.AuthLoginRequest;
import com.gametrend.agent.auth.dto.AuthRegisterRequest;
import com.gametrend.agent.auth.dto.AuthTokenResponse;
import com.gametrend.agent.auth.dto.AuthUserResponse;
import com.gametrend.agent.auth.exception.AuthRequiredException;
import com.gametrend.agent.auth.service.AuthService;
import com.gametrend.agent.auth.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @PostMapping("/register")
    public AuthTokenResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/admin/login")
    public AuthTokenResponse adminLogin(@Valid @RequestBody AuthLoginRequest request) {
        return authService.adminLogin(request);
    }

    @GetMapping("/me")
    public AuthUserResponse me() {
        return currentUserService.getCurrentUser()
                .map(AuthUserResponse::from)
                .orElseThrow(() -> new AuthRequiredException("로그인이 필요한 요청입니다."));
    }
}
