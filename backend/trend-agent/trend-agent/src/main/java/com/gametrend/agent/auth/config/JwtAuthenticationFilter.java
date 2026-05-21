package com.gametrend.agent.auth.config;

import com.gametrend.agent.auth.repository.UserRepository;
import com.gametrend.agent.auth.service.CurrentUser;
import com.gametrend.agent.auth.service.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            jwtTokenProvider.parseAndValidate(token)
                    .flatMap(claims -> userRepository.findById(claims.userId()))
                    .ifPresent(user -> {
                        CurrentUser principal = new CurrentUser(
                                user.getId(),
                                user.getEmail(),
                                user.getNickname(),
                                user.getRole().name(),
                                user.getStatus() == null ? "ACTIVE" : user.getStatus().name(),
                                user.getAuthType() == null ? "LOCAL" : user.getAuthType().name(),
                                user.getProvider(),
                                user.getProfileImageUrl(),
                                user.isEmailVerified(),
                                user.getPhoneNumber(),
                                user.isPhoneVerified()
                        );
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        principal,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).strip();
        return token.isBlank() ? null : token;
    }
}
