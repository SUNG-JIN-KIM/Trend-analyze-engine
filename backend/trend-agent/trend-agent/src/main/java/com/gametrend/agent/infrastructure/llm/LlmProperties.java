package com.gametrend.agent.infrastructure.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * llm.* 외부 설정 바인딩.
 * 기본값은 Ollama 기준이며, base-url 만 바꾸면 LM Studio 등 다른 OpenAI 호환 서버에 연결할 수 있다.
 */
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String baseUrl,
        String model,
        long timeoutMs,
        Double temperature
) {

    public LlmProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:11434/v1";
        }
        if (model == null || model.isBlank()) {
            model = "gemma4:e2b";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 30_000L;
        }
        if (temperature == null) {
            temperature = 0.2;
        }
    }
}
