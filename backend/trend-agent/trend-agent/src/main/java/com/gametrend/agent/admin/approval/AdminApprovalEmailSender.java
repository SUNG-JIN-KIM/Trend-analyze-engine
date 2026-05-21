package com.gametrend.agent.admin.approval;

public interface AdminApprovalEmailSender {

    void send(String to, String subject, String body);
}
