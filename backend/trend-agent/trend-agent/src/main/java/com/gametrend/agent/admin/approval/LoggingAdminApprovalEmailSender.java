package com.gametrend.agent.admin.approval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.admin.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingAdminApprovalEmailSender implements AdminApprovalEmailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[AdminApprovalEmail] to={}, subject={}, body={}", to, subject, body);
    }
}
