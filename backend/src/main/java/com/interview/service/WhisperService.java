package com.interview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@Slf4j
public class WhisperService {

    private final String whisperServiceUrl;
    private final RestTemplate restTemplate;

    public WhisperService(@Value("${whisper.service-url}") String whisperServiceUrl) {
        this.whisperServiceUrl = whisperServiceUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sends audio file to the local Whisper microservice for transcription.
     */
    public String transcribe(MultipartFile audioFile) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Wrap the file bytes in a ByteArrayResource that provides a filename
            ByteArrayResource resource = new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return audioFile.getOriginalFilename() != null
                            ? audioFile.getOriginalFilename() : "audio.webm";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    whisperServiceUrl + "/transcribe", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String text = (String) response.getBody().get("text");
                log.info("Transcription completed: {} chars", text != null ? text.length() : 0);
                return text != null ? text.trim() : "";
            }

            throw new RuntimeException("Whisper service returned non-OK status");
        } catch (Exception e) {
            log.error("Transcription failed: {}", e.getMessage());
            throw new RuntimeException("Failed to transcribe audio: " + e.getMessage(), e);
        }
    }

    /**
     * Sends raw audio bytes to the local Whisper microservice.
     */
    public String transcribe(byte[] audioBytes, String filename) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource resource = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    whisperServiceUrl + "/transcribe", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return ((String) response.getBody().get("text")).trim();
            }
            throw new RuntimeException("Whisper service returned error");
        } catch (Exception e) {
            log.error("Transcription failed: {}", e.getMessage());
            throw new RuntimeException("Failed to transcribe audio: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the Whisper service is available.
     */
    public boolean isAvailable() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    whisperServiceUrl + "/health", Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
