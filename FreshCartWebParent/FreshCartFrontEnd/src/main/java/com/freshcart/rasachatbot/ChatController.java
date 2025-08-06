package com.freshcart.rasachatbot;

import com.freshcart.common.entity.product.Product;
import com.freshcart.product.EmbeddingSearchService;
import com.freshcart.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private RasaChatService rasaChatService;

    @Autowired
    private EmbeddingSearchService embeddingSearchService;

    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> payload) {
        String userMessage = payload.get("message");

        // Lấy intent bằng parse
        Map<String, Object> parseResult = rasaChatService.parseIntent(userMessage);
        Map<String, Object> intentData = (Map<String, Object>) parseResult.get("intent");
        String intent = (String) intentData.get("name");
        double confidence = (double) intentData.get("confidence");

        List<String> messages = new ArrayList<>();

        // Nếu intent là hỏi giá
        if ("ask_price".equals(intent)) {
            // kiểm tra entity phone_name
            List<Map<String, Object>> entities = (List<Map<String, Object>>) parseResult.get("entities");
            boolean hasPhoneEntity = entities.stream()
                    .anyMatch(e -> "phone_name".equals(e.get("entity")));

            if (!hasPhoneEntity) {
                messages.add("Please specify the phone model you want the price for.");
            } else {
                List<Map<String, Object>> products = loadProductsFromDB();
                List<Map<String, Object>> results = embeddingSearchService.searchTopK(userMessage, products, 3);

                if (!results.isEmpty()) {
                    messages.add("Here are the closest matches I found:");
                    for (Map<String, Object> prod : results) {
                        System.out.println("LINK: " + prod.get("alias") + prod.get("link"));
                        messages.add(
                                "The " + prod.get("name") +
                                " price $" + prod.get("price") +
                                ", <a href='http://localhost:8081" + prod.get("link") + "' target='_blank' style='color:#0aad0a;text-decoration:underline;'>View here</a>"
                        );
                    }
                } else {
                    messages.add("Sorry, I couldn't find that product.");
                }
            }
        } else {
            // fallback sang webhook
            Object rasaResponse = rasaChatService.sendMessageToRasa(userMessage);

            if (rasaResponse instanceof List) {
                List<Map<String, Object>> rasaList = (List<Map<String, Object>>) rasaResponse;
                rasaList.forEach(msg -> {
                    if (msg.get("text") != null) {
                        messages.add(msg.get("text").toString());
                    }
                });
            } else {
                messages.add("Unexpected response format from Rasa.");
            }
        }

        // Trả JSON chuẩn hóa
        Map<String, Object> response = new HashMap<>();
        response.put("intent", intent);
        response.put("confidence", confidence);
        response.put("messages", messages);

        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> loadProductsFromDB() {
        return productRepository.findAll().stream().map(product -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", product.getId());
            map.put("name", product.getName());
            map.put("price", product.getPrice());
            map.put("alias", product.getAlias());
            map.put("link", "/FreshCart/p/" + product.getAlias());
            return map;
        }).collect(Collectors.toList());
    }
}
