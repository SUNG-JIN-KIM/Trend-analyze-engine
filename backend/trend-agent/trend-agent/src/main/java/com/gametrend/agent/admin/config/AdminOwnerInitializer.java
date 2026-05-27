package com.gametrend.agent.admin.config;

import com.gametrend.agent.auth.entity.UserRole;
import com.gametrend.agent.auth.entity.UserStatus;
import com.gametrend.agent.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminOwnerInitializer implements ApplicationRunner {

    private final AdminApprovalProperties adminApprovalProperties;
    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        String ownerEmail = adminApprovalProperties.ownerEmail();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            return;
        }

        userRepository.findByEmail(ownerEmail)
                .ifPresentOrElse(user -> {
                    if (user.getRole() == UserRole.OWNER && user.getStatus() == UserStatus.ACTIVE) {
                        log.info("초기 OWNER 계정이 이미 설정되어 있습니다. email={}", ownerEmail);
                        return;
                    }

                    userRepository.save(user.toBuilder()
                            .role(UserRole.OWNER)
                            .status(UserStatus.ACTIVE)
                            .updatedAt(LocalDateTime.now())
                            .build());
                    log.info("초기 OWNER 계정을 설정했습니다. email={}", ownerEmail);
                }, () -> log.info("초기 OWNER 대상 계정이 아직 없습니다. 회원가입 후 서버를 재시작하면 승격됩니다. email={}", ownerEmail));
    }
}
