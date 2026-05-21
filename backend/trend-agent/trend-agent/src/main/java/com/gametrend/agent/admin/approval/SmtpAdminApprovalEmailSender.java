package com.gametrend.agent.admin.approval;

import com.gametrend.agent.admin.config.AdminApprovalProperties;
import com.gametrend.agent.admin.config.AdminMailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.admin.mail", name = "enabled", havingValue = "true")
public class SmtpAdminApprovalEmailSender implements AdminApprovalEmailSender {

    private final JavaMailSender mailSender;
    private final AdminApprovalProperties adminApprovalProperties;
    private final AdminMailProperties adminMailProperties;

    @Override
    public void send(String to, String subject, String body) {
        if (!adminApprovalProperties.approvalEmail().equalsIgnoreCase(to)) {
            throw new IllegalArgumentException("관리자 승인 이메일은 지정된 승인 담당자에게만 발송할 수 있습니다.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (adminMailProperties.from() != null) {
            message.setFrom(adminMailProperties.from());
        }
        message.setTo(adminApprovalProperties.approvalEmail());
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("관리자 승인 이메일 발송 완료. to={}", adminApprovalProperties.approvalEmail());
        } catch (MailException ex) {
            log.warn("관리자 승인 이메일 발송 실패. to={}", adminApprovalProperties.approvalEmail(), ex);
            throw AdminApprovalException.emailSendFailed();
        }
    }
}
