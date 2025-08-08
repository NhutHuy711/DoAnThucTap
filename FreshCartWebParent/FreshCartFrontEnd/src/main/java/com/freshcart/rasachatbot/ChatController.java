package com.freshcart.rasachatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final Map<String, Map<String, String>> userSlots = new HashMap<>();

    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> payload) throws Exception {
        String userMessage = payload.get("message");
        String userId = payload.getOrDefault("userId", "web-user");

        Map<String, Object> parseResult;
        String intent;
        double confidence;

        if (userMessage.startsWith("/") && userMessage.contains("{")) {
            String intentName = userMessage.substring(1, userMessage.indexOf("{"));
            intent = intentName;
            confidence = 1.0;

            Map<String, Object> entityMap = new ObjectMapper().readValue(
                    userMessage.substring(userMessage.indexOf("{")),
                    Map.class
            );

            Map<String, String> slots = userSlots.computeIfAbsent(userId, k -> new HashMap<>());
            entityMap.forEach((k, v) -> slots.put(k, v.toString()));

            List<Map<String, Object>> entityList = entityMap.entrySet().stream()
                    .map(e -> Map.of("entity", e.getKey(), "value", e.getValue()))
                    .collect(Collectors.toList());

            parseResult = Map.of(
                    "intent", Map.of("name", intent, "confidence", confidence),
                    "entities", entityList
            );
        } else {
            List<Map<String, Object>> messages = new ArrayList<>();

// 👉 Handle custom buttons without Rasa
            switch (userMessage.trim().toLowerCase()) {
                case "i want to choose a brand":
                    messages.add(Map.of(
                            "text", "Which phone brand are you interested in?",
                            "buttons", List.of(
                                    Map.of("title", "Samsung", "text", "/ask_recommendation{\"brand\":\"Samsung\"}"),
                                    Map.of("title", "iPhone", "text", "/ask_recommendation{\"brand\":\"iPhone\"}"),
                                    Map.of("title", "Xiaomi", "text", "/ask_recommendation{\"brand\":\"Xiaomi\"}")
                            )
                    ));
                    return ResponseEntity.ok(Map.of("intent", "choose_brand", "messages", messages));

                case "i want to choose usage":
                    messages.add(Map.of(
                            "text", "What will you use the phone for?",
                            "buttons", List.of(
                                    Map.of("title", "Gaming", "text", "/ask_recommendation{\"usage\":\"gaming\"}"),
                                    Map.of("title", "Photography", "text", "/ask_recommendation{\"usage\":\"photography\"}"),
                                    Map.of("title", "Work/Study", "text", "/ask_recommendation{\"usage\":\"work\"}")
                            )
                    ));
                    return ResponseEntity.ok(Map.of("intent", "choose_usage", "messages", messages));

                case "i want to choose budget":
                    messages.add(Map.of(
                            "text", "What is your budget?",
                            "buttons", List.of(
                                    Map.of("title", "Under $300", "text", "/ask_recommendation{\"budget\":\"300\"}"),
                                    Map.of("title", "Under $500", "text", "/ask_recommendation{\"budget\":\"500\"}"),
                                    Map.of("title", "No limit", "text", "/ask_recommendation{\"budget\":\"99999\"}")
                            )
                    ));
                    return ResponseEntity.ok(Map.of("intent", "choose_budget", "messages", messages));
            }
            parseResult = rasaChatService.parseIntent(userMessage);
            Map<String, Object> intentData = (Map<String, Object>) parseResult.get("intent");
            intent = (String) intentData.get("name");
            confidence = (double) intentData.get("confidence");
        }

        List<Map<String, Object>> messages = new ArrayList<>();

        // ép intent về ask_recommendation nếu có brand entity
        if ("greet".equals(intent) && containsBrandEntity(parseResult)) {
            System.out.println("⚡ Intent ép lại từ greet → ask_recommendation");
            intent = "ask_recommendation";
        }

        if ("ask_price".equals(intent)) {
            handleAskPrice(userMessage, parseResult, messages);
        } else if ("ask_recommendation".equals(intent)) {
            handleAskRecommendation(userMessage, userId, parseResult, messages);
        } else {
            Object rasaResponse = rasaChatService.sendMessageToRasa(userMessage);

            if (rasaResponse instanceof List) {
                List<Map<String, Object>> rasaList = (List<Map<String, Object>>) rasaResponse;
                for (Map<String, Object> msg : rasaList) {
                    Map<String, Object> messageObj = new HashMap<>();
                    if (msg.get("text") != null) messageObj.put("text", msg.get("text"));
                    if (msg.get("buttons") != null) messageObj.put("buttons", msg.get("buttons"));
                    messages.add(messageObj);
                }
            } else {
                messages.add(Map.of("text", "Unexpected response format from Rasa."));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("intent", intent);
        response.put("confidence", confidence);
        response.put("messages", messages);

        return ResponseEntity.ok(response);
    }

    private void handleAskPrice(String userMessage, Map<String, Object> parseResult, List<Map<String, Object>> messages) {
        List<Map<String, Object>> entities = (List<Map<String, Object>>) parseResult.get("entities");
        boolean hasPhoneEntity = entities.stream()
                .anyMatch(e -> "phone_name".equals(e.get("entity")));

        if (!hasPhoneEntity) {
            messages.add(Map.of("text", "Please specify the phone model you want the price for."));
        } else {
            List<Map<String, Object>> products = loadProductsFromDB();
            List<Map<String, Object>> results = embeddingSearchService.searchTopK(userMessage, products, 3);

            if (!results.isEmpty()) {
                messages.add(Map.of("text", "Here are the closest matches I found:"));
                for (Map<String, Object> prod : results) {
                    messages.add(Map.of(
                            "text", "The " + prod.get("name") +
                                    " price $" + prod.get("price") +
                                    ", <a href='http://localhost:8081" + prod.get("link") +
                                    "' target='_blank' style='color:#0aad0a;text-decoration:underline;'>View here</a>"
                    ));
                }
            } else {
                messages.add(Map.of("text", "Sorry, I couldn't find that product."));
            }
        }
    }

    private void handleAskRecommendation(String userMessage, String userId,
                                         Map<String, Object> parseResult, List<Map<String, Object>> messages) {
        Map<String, String> slots = userSlots.computeIfAbsent(userId, k -> new HashMap<>());

        // Cập nhật slots từ entities
        List<Map<String, Object>> entities = (List<Map<String, Object>>) parseResult.get("entities");
        if (entities != null) {
            for (Map<String, Object> e : entities) {
                String entity = (String) e.get("entity");
                String value = e.get("value").toString();
                slots.put(entity, value);

                if ("brand".equals(entity)) {
                    System.out.println("Rasa recognized brand: " + value);
                }
            }
        }

        // Nếu chưa có brand → đoán bằng embedding
        if (!slots.containsKey("brand")) {
            Optional<String> guessedBrand = searchClosestBrand(userMessage);
            if (guessedBrand.isPresent()) {
                slots.put("brand", guessedBrand.get());
                System.out.println("Brand guessed by embedding: " + guessedBrand.get());
            } else {
                System.out.println("No brand recognized. Proceeding with usage/budget filters only.");
            }
        }

        // Nếu không có bất kỳ thông tin nào về brand, usage, hoặc budget → yêu cầu người dùng cung cấp thêm
        if (slots.get("brand") == null && slots.get("usage") == null && slots.get("budget") == null) {
            messages.add(Map.of(
                    "text", "Could you please provide more information such as the phone brand, your usage needs, or your budget? This will help me give you more accurate recommendations.",
                    "buttons", List.of(
                            Map.of("title", "Choose Brand", "payload", "/ask_brand"),
                            Map.of("title", "Choose Usage", "payload", "/ask_usage"),
                            Map.of("title", "Choose Budget", "payload", "/ask_budget")
                    )
            ));
            return;
        }

        // Load tất cả sản phẩm
        List<Map<String, Object>> products = loadProductsFromDB();
        List<Map<String, Object>> filtered;

        // Nếu có brandId trong DB thì ưu tiên filter theo brand
        Optional<Integer> brandIdOpt = Optional.empty();
        if (slots.get("brand") != null) {
            brandIdOpt = getBrandIdByName(slots.get("brand"));
        }

        if (brandIdOpt.isPresent()) {
            int brandId = brandIdOpt.get();
            System.out.println("Filtering DB products by brandId: " + brandId);
            filtered = products.stream()
                    .filter(p -> p.get("brandId") != null &&
                            Integer.parseInt(p.get("brandId").toString()) == brandId)
                    .collect(Collectors.toList());
        } else {
            System.out.println("No valid brand → fallback to all products.");
            filtered = new ArrayList<>(products); // lấy tất cả sản phẩm
        }

        // Áp dụng budget filter
        if (slots.get("budget") != null) {
            try {
                double budget = Double.parseDouble(slots.get("budget"));
                filtered = filtered.stream()
                        .filter(p -> Double.parseDouble(p.get("price").toString()) <= budget)
                        .collect(Collectors.toList());
                System.out.println("Applied budget filter: <= " + budget);
            } catch (Exception e) {
                System.out.println("Failed to parse budget value: " + slots.get("budget"));
            }
        }

        // Áp dụng usage filter
        if (slots.get("usage") != null) {
            System.out.println("Applying usage filter: " + slots.get("usage"));
            filtered = applyUsageFilter(filtered, slots.get("usage"));
        }

        // Nếu vẫn còn kết quả thì trả về
        if (!filtered.isEmpty()) {
            messages.add(Map.of("text", "Here are some phones for you:"));
            for (Map<String, Object> prod : filtered.stream().limit(3).collect(Collectors.toList())) {
                messages.add(Map.of(
                        "text", "The " + prod.get("name") +
                                " price $" + prod.get("price") +
                                ", <a href='http://localhost:8081" + prod.get("link") +
                                "' target='_blank' style='color:#0aad0a;text-decoration:underline;'>View here</a>"
                ));
            }
        } else {
            System.out.println("No matching phones found after filtering.");
            messages.add(Map.of("text", "Sorry, I couldn't find any phones matching your preferences."));
        }
    }


    private boolean containsBrandEntity(Map<String, Object> parseResult) {
        List<Map<String, Object>> entities = (List<Map<String, Object>>) parseResult.get("entities");
        if (entities == null) return false;
        return entities.stream().anyMatch(e -> "brand".equals(e.get("entity")));
    }

    private List<Map<String, Object>> loadProductsFromDB() {
        return productRepository.findAll().stream().map(product -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", product.getId());
            map.put("name", product.getName());
            map.put("price", product.getPrice());
            map.put("alias", product.getAlias());
            map.put("link", "/FreshCart/p/" + product.getAlias());
            if (product.getBrand() != null) {
                map.put("brandId", product.getBrand().getId());
                map.put("brandName", product.getBrand().getName());
            }
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> applyUsageFilter(List<Map<String, Object>> products, String usage) {
        return products.stream().filter(p -> {
            String name = p.get("name").toString().toLowerCase();
            switch (usage.toLowerCase()) {
                case "gaming":
                    return name.contains("gaming") || name.contains("pro") || name.contains("max");
                case "photography":
                    Object specs = p.get("specs");
                    return specs != null && specs.toString().toLowerCase().contains("48mp");
                case "work":
                case "work/study":
                    return name.contains("plus") || name.contains("max") || name.contains("pro");
                default:
                    return true;
            }
        }).collect(Collectors.toList());
    }

    private Optional<Integer> getBrandIdByName(String brandName) {
        return productRepository.findAll().stream()
                .filter(p -> p.getBrand() != null &&
                        p.getBrand().getName().equalsIgnoreCase(brandName))
                .map(p -> p.getBrand().getId())
                .findFirst();
    }

    private Optional<String> searchClosestBrand(String userMessage) {
        List<String> brandNames = productRepository.findAll().stream()
                .map(p -> p.getBrand() != null ? p.getBrand().getName() : null)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<Map<String, Object>> brandAsProducts = brandNames.stream()
                .map(name -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", name);
                    return m;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> results = embeddingSearchService.searchTopK(userMessage, brandAsProducts, 1);

        if (!results.isEmpty()) {
            String matchedBrand = (String) results.get(0).get("name");
            return Optional.ofNullable(matchedBrand);
        }

        return Optional.empty();
    }
}