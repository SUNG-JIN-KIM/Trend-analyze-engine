package com.gametrend.agent.auth.phone;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockSmsSender implements SmsSender {

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        log.info("[MockSmsSender] phoneNumber={}, verificationCode={}", phoneNumber, code);
    }
}
