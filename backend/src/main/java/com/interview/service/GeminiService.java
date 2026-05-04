package com.interview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GeminiService {

    private final String apiKey;
    private final String chatModel;
    private final String embeddingModel;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.chat-model}") String chatModel,
            @Value("${gemini.embedding-model}") String embeddingModel,
            @Value("${gemini.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate text using Gemini chat model.
     */
    public String generateText(String prompt) {
        try {
            String url = String.format("%s/models/%s:generateContent?key=%s",
                    baseUrl, chatModel, apiKey);

            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contents = requestBody.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", prompt);

            // Add generation config for better responses
            ObjectNode genConfig = requestBody.putObject("generationConfig");
            genConfig.put("temperature", 0.7);
            genConfig.put("maxOutputTokens", 2048);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                JsonNode candidates = responseJson.get("candidates");
                if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode textNode = candidates.get(0)
                            .get("content").get("parts").get(0).get("text");
                    return textNode.asText();
                }
            }
            throw new RuntimeException("No response from Gemini API");
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate text: " + e.getMessage(), e);
        }
    }

    /**
     * Generate embeddings for text using Gemini embedding model.
     */
    public List<Float> generateEmbedding(String text) {
        try {
            String url = String.format("%s/models/%s:embedContent?key=%s",
                    baseUrl, embeddingModel, apiKey);

            ObjectNode requestBody = objectMapper.createObjectNode();
            ObjectNode content = requestBody.putObject("content");
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                JsonNode values = responseJson.get("embedding").get("values");
                List<Float> embedding = new ArrayList<>();
                for (JsonNode val : values) {
                    embedding.add((float) val.asDouble());
                }
                return embedding;
            }
            throw new RuntimeException("No embedding response from Gemini API");
        } catch (Exception e) {
            log.error("Gemini embedding failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a structured JSON response from Gemini.
     */
    public String generateJsonResponse(String prompt) {
        String wrappedPrompt = prompt + "\n\nIMPORTANT: Respond ONLY with valid JSON. No markdown, no code fences, no explanatory text.";
        String response = generateText(wrappedPrompt);
        // Strip any markdown code fences if present
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }
}
