package com.gametrend.agent.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public record AdminApprovalProperties(
        String approvalEmail,
        String ownerEmail,
        long approvalTokenExpireMinutes,
        String approvalLinkBaseUrl
) {

    private static final String REQUIRED_APPROVAL_EMAIL = "ksjcloud98@gmail.com";

    public AdminApprovalProperties {
        approvalEmail = REQUIRED_APPROVAL_EMAIL;

        if (ownerEmail != null && !ownerEmail.isBlank()) {
            ownerEmail = ownerEmail.strip().toLowerCase();
        }

        if (approvalTokenExpireMinutes <= 0) {
            approvalTokenExpireMinutes = 1_440;
        }

        if (approvalLinkBaseUrl == null || approvalLinkBaseUrl.isBlank()) {
            approvalLinkBaseUrl = "http://localhost:8080";
        }
        approvalLinkBaseUrl = approvalLinkBaseUrl.replaceAll("/+$", "");
    }
}
