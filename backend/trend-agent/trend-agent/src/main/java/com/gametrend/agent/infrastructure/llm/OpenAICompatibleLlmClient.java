package com.gametrend.agent.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * OpenAI 호환 /v1/chat/completions 엔드포인트를 호출하는 기본 LLM 클라이언트.
 * Ollama (http://localhost:11434/v1) 와 LM Studio (http://localhost:1234/v1) 모두에서 동일하게 동작한다.
 */
@Slf4j
@Component
public class OpenAICompatibleLlmClient implements LlmClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON,
            StandardCharsets.UTF_8
    );

    private final RestClient llmRestClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAICompatibleLlmClient(
            @Qualifier("llmRestClient") RestClient llmRestClient,
            LlmProperties properties,
            ObjectMapper objectMapper
    ) {
        this.llmRestClient = llmRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.model(),
                List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", userPrompt)
                ),
                properties.temperature(),
                false
        );

        try {
            String rawResponse = llmRestClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(APPLICATION_JSON_UTF8)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            ChatCompletionResponse response = parseResponse(rawResponse);
            return extractContent(response);
        } catch (RestClientException ex) {
            log.warn("LLM 호출 실패: baseUrl={}, model={}, message={}",
                    properties.baseUrl(), properties.model(), ex.getMessage());
            throw new LlmCallException("LLM 호출에 실패했습니다.", ex);
        }
    }

    private ChatCompletionResponse parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new LlmCallException("LLM 응답 body 가 비어 있습니다.");
        }

        try {
            return objectMapper.readValue(rawResponse.strip(), ChatCompletionResponse.class);
        } catch (JsonProcessingException ex) {
            throw new LlmCallException("LLM 응답 JSON 파싱에 실패했습니다.", ex);
        }
    }

    private String extractContent(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new LlmCallException("LLM 응답에 choices 가 없습니다.");
        }

        return response.choices().stream()
                .map(ChatCompletionResponse.Choice::message)
                .filter(Objects::nonNull)
                .map(ChatMessage::content)
                .filter(content -> content != null && !content.isBlank())
                .findFirst()
                .orElseThrow(() -> new LlmCallException("LLM 응답에 content 가 없습니다."));
    }

    record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            double temperature,
            boolean stream
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatCompletionResponse(List<Choice> choices) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Choice(ChatMessage message) {
        }
    }
}
