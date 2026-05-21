package com.gametrend.agent.auth.phone;

import com.gametrend.agent.auth.phone.dto.PhoneCodeSendRequest;
import com.gametrend.agent.auth.phone.dto.PhoneCodeVerifyRequest;
import com.gametrend.agent.auth.phone.dto.PhoneVerificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/phone")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/send-code")
    public PhoneVerificationResponse sendCode(@Valid @RequestBody PhoneCodeSendRequest request) {
        return phoneVerificationService.sendRegisterCode(request.phoneNumber());
    }

    @PostMapping("/verify")
    public PhoneVerificationResponse verify(@Valid @RequestBody PhoneCodeVerifyRequest request) {
        return phoneVerificationService.verifyRegisterCode(request.phoneNumber(), request.code());
    }
}
