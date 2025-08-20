//package com.freshcart.chatbot;
//
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//@Service
//public class GeminiEmbeddingService {
//    private final String GEMINI_EMBEDDING_URL =
//            "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent";
//    private final String API_KEY = System.getenv("GEMINI_API_KEY");
//    private final RestTemplate restTemplate = new RestTemplate();
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    public double[] getEmbedding(String text) throws Exception {
//        String url = GEMINI_EMBEDDING_URL + "?key=" + API_KEY;
//
//        // Payload đúng chuẩn cho Gemini
//        String requestJson = String.format(
//                "{\"model\": \"models/text-embedding-004\", \"content\": {\"parts\": [{\"text\": \"%s\"}]}}",
//                text.replace("\"", "\\\"")
//        );
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
//        ResponseEntity<String> response =
//                restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//
//        System.out.println("Gemini response: " + response.getBody());
//
//        JsonNode root = mapper.readTree(response.getBody());
//        JsonNode vectorNode = root.at("/embedding/values"); // <-- sửa lại đúng path
//
//        if (vectorNode.isMissingNode()) {
//            throw new IllegalStateException("Không nhận được embedding từ Gemini API: " + response.getBody());
//        }
//
//        double[] embedding = new double[vectorNode.size()];
//        for (int i = 0; i < vectorNode.size(); i++) {
//            embedding[i] = vectorNode.get(i).asDouble();
//        }
//
//        return embedding;
//    }
//}
