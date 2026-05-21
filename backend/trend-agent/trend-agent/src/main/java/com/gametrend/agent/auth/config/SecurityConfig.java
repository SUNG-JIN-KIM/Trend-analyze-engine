package com.gametrend.agent.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.admin.config.AdminApprovalProperties;
import com.gametrend.agent.admin.config.AdminMailProperties;
import com.gametrend.agent.auth.oauth.OAuth2AuthenticationFailureHandler;
import com.gametrend.agent.auth.oauth.OAuth2AuthenticationSuccessHandler;
import com.gametrend.agent.common.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({
        AuthProperties.class,
        AppProperties.class,
        PhoneVerificationProperties.class,
        SmsProperties.class,
        AdminApprovalProperties.class,
        AdminMailProperties.class
})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin/approval/approve", "/admin/approval/reject").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin/login").permitAll()
                        .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin-approval/request").authenticated()
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers("/api/conversations/**").authenticated()
                        .requestMatchers(
                                "/api/my/conversations",
                                "/api/my/conversations/**",
                                "/my/conversations",
                                "/my/conversations/**"
                        ).authenticated()
                        .requestMatchers("/api/onboarding/history", "/api/onboarding/history/**").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/live-trends/status",
                                "/api/live-trends/games/top",
                                "/api/live-trends/rankings"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/legacy-games", "/api/reinterpretation/candidates").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/onboarding/analyze").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/live-trends/refresh",
                                "/api/legacy-games/refresh",
                                "/api/reinterpretation/analyze"
                        ).authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> writeError(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "AUTH_REQUIRED",
                                "로그인이 필요한 요청입니다."
                        ))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeError(
                                response,
                                HttpStatus.FORBIDDEN,
                                "ACCESS_DENIED",
                                "접근 권한이 없습니다."
                        ))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeError(
            jakarta.servlet.http.HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ErrorResponse.of(code, message, List.of(message))
        );
    }
}
