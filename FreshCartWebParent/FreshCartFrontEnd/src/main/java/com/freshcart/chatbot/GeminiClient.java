package com.freshcart.chatbot;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
<<<<<<< HEAD
=======
import org.springframework.beans.factory.annotation.Value;

>>>>>>> parent of d4ccf3b (Update Chatbot)
import java.util.*;

public class GeminiClient {

<<<<<<< HEAD
    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                    + GEMINI_API_KEY;
=======
    private static final String COPILOT_API_KEY = System.getenv("COPILOT_API_KEY");

    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + COPILOT_API_KEY;
>>>>>>> parent of d4ccf3b (Update Chatbot)

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Hàm gọi Gemini chung
    public static String sendPromptToGemini(String prompt) throws Exception {
        String jsonRequest = createJsonRequest(prompt);
        return sendRequest(jsonRequest).body();
    }

    // Tạo request body chuẩn
    private static String createJsonRequest(String prompt) throws Exception {
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of("contents", List.of(content));
        return objectMapper.writeValueAsString(body);
    }

    // Gửi HTTP request
    private static HttpResponse<String> sendRequest(String jsonRequest) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
