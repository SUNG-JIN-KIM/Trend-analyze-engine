package com.gametrend.agent.auth.phone;

import com.gametrend.agent.auth.dto.AuthLoginRequest;
import com.gametrend.agent.auth.dto.AuthRegisterRequest;
import com.gametrend.agent.auth.dto.AuthTokenResponse;
import com.gametrend.agent.auth.exception.AdminLoginDeniedException;
import com.gametrend.agent.auth.repository.UserRepository;
import com.gametrend.agent.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PhoneVerificationServiceTest {

    private final PhoneVerificationService phoneVerificationService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final CapturingSmsSender smsSender;

    @Autowired
    PhoneVerificationServiceTest(
            PhoneVerificationService phoneVerificationService,
            AuthService authService,
            UserRepository userRepository,
            CapturingSmsSender smsSender
    ) {
        this.phoneVerificationService = phoneVerificationService;
        this.authService = authService;
        this.userRepository = userRepository;
        this.smsSender = smsSender;
    }

    @Test
    void sendVerifyAndRegisterWithoutPhoneVerificationRequirement() {
        String phoneNumber = "01055550123";
        String email = "phone-test@example.com";

        phoneVerificationService.sendRegisterCode(phoneNumber);
        String code = smsSender.latestCode(phoneNumber);

        assertNotNull(code);
        assertTrue(phoneVerificationService.verifyRegisterCode(phoneNumber, code).verified());

        AuthTokenResponse response = authService.register(new AuthRegisterRequest(
                email,
                "password123",
                "phone-user"
        ));

        assertNotNull(response.accessToken());
        assertEquals("USER", response.user().role());
        assertEquals(false, response.user().phoneVerified());
        assertEquals(null, response.user().phoneNumber());
        assertEquals(false, userRepository.findByEmail(email).orElseThrow().isPhoneVerified());
        assertThrows(
                AdminLoginDeniedException.class,
                () -> authService.adminLogin(new AuthLoginRequest(email, "password123"))
        );
    }

    @TestConfiguration
    static class SmsSenderTestConfig {

        @Bean
        @Primary
        CapturingSmsSender capturingSmsSender() {
            return new CapturingSmsSender();
        }
    }

    static class CapturingSmsSender implements SmsSender {
        private final Map<String, String> codes = new ConcurrentHashMap<>();

        @Override
        public void sendVerificationCode(String phoneNumber, String code) {
            codes.put(phoneNumber, code);
        }

        String latestCode(String phoneNumber) {
            return codes.get(phoneNumber);
        }
    }
}
