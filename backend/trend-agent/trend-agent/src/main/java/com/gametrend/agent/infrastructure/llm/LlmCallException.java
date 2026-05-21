package com.gametrend.agent.infrastructure.llm;

/**
 * LLM 호출 실패 시 던지는 런타임 예외.
 * 호출자는 catch 후 정적 fallback 으로 대체할 수 있다.
 */
public class LlmCallException extends RuntimeException {

    public LlmCallException(String message) {
        super(message);
    }

    public LlmCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
