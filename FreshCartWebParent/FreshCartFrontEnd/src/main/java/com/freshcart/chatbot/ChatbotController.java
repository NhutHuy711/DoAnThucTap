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
    public String ask(@RequestBody String userMessage) {
        try {
            System.out.println("Received message: " + userMessage);

            // Bước 1: Dùng Gemini để phân tích câu hỏi (intent + product_name)
            String analysisPrompt = """
        Hãy phân tích câu hỏi của người dùng và trả về JSON với 2 thuộc tính:
        - "intent": ví dụ "query_stock", "order", "greeting", "promotion"
        - "product_name": tên sản phẩm nếu có, nếu không có thì để chuỗi rỗng ""
        Chỉ trả về JSON, không thêm giải thích.

        Câu hỏi: %s
        """.formatted(userMessage);

            String geminiAnalysisRaw = GeminiClient.sendPromptToGemini(analysisPrompt);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(geminiAnalysisRaw);
            String analysisText = root.at("/candidates/0/content/parts/0/text").asText().trim();

            // Làm sạch nếu Gemini trả về kèm ```json
            String cleanedJson = analysisText.replaceAll("(?i)```json", "")
                    .replaceAll("(?i)```", "")
                    .trim();

            JsonNode extracted;
            try {
                extracted = mapper.readTree(cleanedJson);
            } catch (Exception jsonEx) {
                System.out.println("Không thể phân tích JSON từ Gemini.");
                return toJsonReply("Tôi chưa hiểu rõ yêu cầu. Bạn có thể hỏi lại cụ thể hơn không?");
            }

            String productName = extracted.has("product_name") ? extracted.get("product_name").asText() : "";
            String intent = extracted.has("intent") ? extracted.get("intent").asText() : "unknown";
            String productInfo = "";

            // Bước 2: Truy xuất dữ liệu sản phẩm
            if (!productName.isEmpty()) {
               Product product = productService.findByProductName(productName);
                if (!product.getName().isEmpty()) {
                    productInfo = "Tên: " + product.getName() + ", số lượng còn: " + product.getInStock() +
                            ", giá: " + product.getPrice() + " VNĐ.";
                } else {
                    productInfo = "Không tìm thấy sản phẩm nào có tên \"" + productName + "\".";
                }
            } else {
                productInfo = "Không xác định rõ sản phẩm nào từ câu hỏi.";
            }

            // Bước 3: Gửi RAG prompt đến Gemini để sinh phản hồi tự nhiên
            String ragPrompt = """
        Bạn là trợ lý tư vấn khách hàng. Dưới đây là thông tin bạn biết được từ hệ thống:

        %s

        Câu hỏi của khách hàng: %s

        Hãy trả lời thân thiện, tự nhiên bằng tiếng Việt.
        """.formatted(productInfo, userMessage);

            String finalGeminiRaw = GeminiClient.sendPromptToGemini(ragPrompt);
            JsonNode finalRoot = mapper.readTree(finalGeminiRaw);
            String finalAnswer = finalRoot.at("/candidates/0/content/parts/0/text").asText().trim();

            return toJsonReply(finalAnswer);

        } catch (Exception e) {
            e.printStackTrace();
            return toJsonReply("Đã xảy ra lỗi: " + e.getMessage().replace("\"", "'"));
        }
    }


    private String toJsonReply(String replyText) {
        return "{\"reply\": \"" + replyText.replace("\"", "\\\"") + "\"}";
    }
}
