package com.freshcart.chatbot;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class GeminiClient {

    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");

    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Dùng cho bước RAG: sinh phản hồi tự nhiên
    public static String sendPromptToGemini(String prompt) throws Exception {
        String jsonRequest = createJsonRequest(prompt);
        HttpResponse<String> response = sendRequest(jsonRequest);
        return response.body();
    }

    // Dùng cho bước phân tích intent/product_name (trả về JSON)
    public static String sendPromptToGeminiAsJson(String userMessage) throws Exception {
        String prompt = """
        Hãy phân tích câu hỏi người dùng bên dưới và trả về kết quả dưới dạng JSON với 2 thuộc tính:
        - "intent": xác định mục đích như "query_stock", "order", "promotion", v.v.
        - "product_name": tên sản phẩm nếu có, ngược lại để trống.
        Chỉ trả về JSON, không giải thích gì thêm.

        Câu hỏi: %s
        """.formatted(userMessage);

        String jsonRequest = createJsonRequest(prompt);
        HttpResponse<String> response = sendRequest(jsonRequest);
        return response.body();
    }

    // Tạo phần thân JSON đúng chuẩn Google Gemini
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
