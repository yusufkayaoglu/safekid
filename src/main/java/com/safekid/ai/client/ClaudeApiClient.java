package com.safekid.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeApiClient {

    private final RestTemplate aiRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${safekid.ai.anthropic.base-url}")
    private String baseUrl;

    @Value("${safekid.ai.anthropic.model}")
    private String model;

    @Value("${safekid.ai.anthropic.max-tokens}")
    private int maxTokens;

    public String sendMessage(String systemPrompt, String userMessage) {
        return sendMessages(systemPrompt, List.of(Map.of("role", "user", "content", userMessage)));
    }

    public String sendMessages(String systemPrompt, List<Map<String, Object>> messages) {
        String url = baseUrl + "/v1/messages";

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", messages
        );

        try {
            String response = aiRestTemplate.postForObject(url, body, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("content");
            if (content.isArray() && !content.isEmpty()) {
                return content.get(0).path("text").asText();
            }
            log.warn("Unexpected Claude API response: {}", response);
            return "";
        } catch (Exception e) {
            log.error("Claude API call failed", e);
            throw new RuntimeException("Claude API call failed: " + e.getMessage(), e);
        }
    }
}
