package com.gametrend.agent.auth.phone;

public interface SmsSender {

    void sendVerificationCode(String phoneNumber, String code);
}
