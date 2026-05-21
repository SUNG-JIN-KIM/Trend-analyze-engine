package com.gametrend.agent.infrastructure.llm;

/**
 * 서비스 런타임 LLM(GEMMA4 E2B) 호출을 위한 추상화.
 * 구현체는 OpenAI 호환 엔드포인트(LM Studio, Ollama 등)를 호출한다.
 */
public interface LlmClient {

    String complete(String systemPrompt, String userPrompt);
}
