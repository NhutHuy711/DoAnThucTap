package com.freshcart.chatbot;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class GeminiClient {
    private static final String API_KEY = "AIzaSyBVwnSWHESCFRgV29xyv9BWuFfJuFVx4Es";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    public static String sendMessageToGemini(String userMessage) throws Exception {
        String prompt = """
        Hãy phân tích câu hỏi người dùng bên dưới và trả về kết quả dưới dạng JSON với 2 thuộc tính:
        - "intent": xác định mục đích như "query_stock", "order", "promotion", v.v.
        - "product_name": tên sản phẩm nếu có, ngược lại để trống.
        Chỉ trả về JSON, không giải thích gì thêm.

        Câu hỏi: %s
        """.formatted(userMessage);

        // Tạo JSON request đúng cách
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of("contents", List.of(content));

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonRequest = objectMapper.writeValueAsString(body);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
