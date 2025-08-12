package com.freshcart.rasachatbot;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RasaChatService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String rasaWebhookUrl = "http://localhost:5005/webhooks/rest/webhook";
    private final String rasaParseUrl = "http://localhost:5005/model/parse";

    // Gửi tin nhắn để Rasa trả lời
    public Object sendMessageToRasa(String userMessage) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", "web-user");
        payload.put("message", userMessage);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(rasaWebhookUrl, request, Object.class);
            System.out.println("Raw webhook response: " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error connecting to Rasa webhook: " + e.getMessage());
            return Collections.singletonMap("error", "Cannot connect to Rasa webhook");
        }
    }

    // Gửi tin nhắn để parse intent
    public Map<String, Object> parseIntent(String userMessage) {
        Map<String, String> payload = new HashMap<>();
        payload.put("text", userMessage);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(rasaParseUrl, request, Map.class);
            System.out.println("Raw parse response: " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error connecting to Rasa parse: " + e.getMessage());
            return null;
        }
    }
}


