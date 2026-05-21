package com.gametrend.agent.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.auth.phone.MockSmsSender;
import com.gametrend.agent.auth.phone.NaverSensSmsSender;
import com.gametrend.agent.auth.phone.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsSenderConfig {

    @Bean
    public SmsSender smsSender(SmsProperties properties, ObjectMapper objectMapper) {
        if (properties.isMockProvider()) {
            log.info("SMS provider=mock. 인증번호는 실제 문자로 발송되지 않고 서버 로그에 출력됩니다.");
            return new MockSmsSender();
        }
        if (properties.isNaverSensProvider()) {
            if (properties.missingNaverSensSettings().isEmpty()) {
                log.info("SMS provider=naver-sens. 실제 문자 발송 모드로 동작합니다.");
            } else {
                log.warn("SMS provider=naver-sens 이지만 필수 설정이 비어 있습니다. missing={}", properties.missingNaverSensSettings());
            }
            return new NaverSensSmsSender(properties, objectMapper);
        }
        throw new IllegalStateException("지원하지 않는 SMS provider입니다: " + properties.provider());
    }
}
