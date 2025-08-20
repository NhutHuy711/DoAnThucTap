//package com.freshcart.product;
//
//import com.freshcart.chatbot.GeminiEmbeddingService;
//import com.freshcart.common.entity.product.Product;
//import com.freshcart.common.entity.product.ProductEmbedding;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.io.IOException;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//public class EmbeddingSearchService {
//
//    private static final String EMBEDDING_API = "http://localhost:8000/embedding/search";
//
//    public List<Map<String, Object>> search(String query, List<Map<String, Object>> products) {
//        RestTemplate restTemplate = new RestTemplate();
//
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("query", query);
//        payload.put("products", products);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
//
//        try {
//            String url = EMBEDDING_API + "?min_score=0.7";
//            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
//
//            Map<String, Object> body = response.getBody();
//            if (body != null && body.containsKey("results")) {
//                return (List<Map<String, Object>>) body.get("results");
//            }
//            return Collections.emptyList();
//        } catch (Exception e) {
//            System.err.println("Error connecting to embedding service: " + e.getMessage());
//            return Collections.emptyList();
//        }
//    }
//
//    public List<Map<String, Object>> searchTopK(String query, List<Map<String, Object>> products, int k) {
//        RestTemplate restTemplate = new RestTemplate();
//
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("query", query);
//        payload.put("products", products);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
//
//        try {
//            ResponseEntity<Map> response = restTemplate.postForEntity(EMBEDDING_API, request, Map.class);
//
//            if (response.getBody() != null && response.getBody().get("results") != null) {
//                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
//
//                // trả về Top-K
//                return results.stream()
//                        .sorted((a, b) -> Double.compare((double) b.get("score"), (double) a.get("score")))
//                        .limit(k)
//                        .collect(Collectors.toList());
//            }
//            return Collections.emptyList();
//        } catch (Exception e) {
//            System.err.println("Error connecting to embedding service: " + e.getMessage());
//            return Collections.emptyList();
//        }
//    }
//
//
//}
