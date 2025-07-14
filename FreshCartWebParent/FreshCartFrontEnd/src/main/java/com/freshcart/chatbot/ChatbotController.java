package com.freshcart.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshcart.common.entity.product.Product;
import com.freshcart.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    @Autowired
    private ProductService productService;

    @PostMapping("/ask")
    public String ask(@RequestBody String message) {
        try {
            System.out.println("Received message: " + message);

            String geminiRaw = GeminiClient.sendMessageToGemini(message);
            System.out.println("Gemini raw response: " + geminiRaw);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(geminiRaw);
            String contentText = root.at("/candidates/0/content/parts/0/text").asText().trim();
            System.out.println("Gemini contentText: " + contentText);

            // Làm sạch phần mã nếu Gemini trả về dạng ```json
            String cleanedText = contentText
                    .replaceAll("(?i)```json", "")
                    .replaceAll("(?i)```", "")
                    .trim();

            // Nếu không phải JSON, trả lại nguyên văn như lời chào
            JsonNode extracted;
            try {
                extracted = mapper.readTree(cleanedText);
            } catch (Exception jsonEx) {
                System.out.println("Not valid JSON, returning as plain text.");
                return toJsonReply(contentText);
            }

            String intent = extracted.has("intent") ? extracted.get("intent").asText() : "unknown";
            String productName = extracted.has("product_name") ? extracted.get("product_name").asText() : "";

            switch (intent) {
                case "greeting":
                    return toJsonReply("Xin chào! Tôi có thể giúp gì cho bạn?");
                case "query_stock":
                    if (productName.isEmpty()) {
                        return toJsonReply("Bạn muốn kiểm tra tồn kho sản phẩm nào?");
                    }
                    Product product = productService.search(productName, 1).stream()
                            .findFirst().orElse(null);
                    if (product != null) {
                        return toJsonReply("Còn " + product.getInStock() + " chiếc " + product.getName() + " trong kho.");
                    } else {
                        return toJsonReply("Không tìm thấy sản phẩm '" + productName + "'.");
                    }
                default:
                    return toJsonReply("Tôi chưa hiểu yêu cầu của bạn. Bạn có thể hỏi lại không?");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return toJsonReply("Đã xảy ra lỗi: " + e.getMessage().replace("\"", "'"));
        }
    }

    private String toJsonReply(String replyText) {
        return "{\"reply\": \"" + replyText.replace("\"", "\\\"") + "\"}";
    }
}
