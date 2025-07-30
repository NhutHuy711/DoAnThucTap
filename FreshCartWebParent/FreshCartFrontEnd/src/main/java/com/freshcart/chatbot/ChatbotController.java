package com.freshcart.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshcart.common.entity.product.Product;
import com.freshcart.common.entity.product.ProductDetail;
import com.freshcart.product.ProductClassifier;
import com.freshcart.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    @Autowired
    private ProductService productService;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/ask")
    public String ask(@RequestBody String userMessage) {
        try {
            System.out.println("Received message: " + userMessage);

            // ── Bước 1: Gemini phân tích intent & product_name ────────────
            String analysisPrompt = """
                    Hãy phân tích câu hỏi của người dùng và trả về JSON với 3 thuộc tính:
                    - "intent": ví dụ "query_stock", "order", "greeting", "promotion", "advice"
                    - "product_name": tên sản phẩm nếu có, nếu không thì để ""
                    - "category": chọn trong ["Gaming","Văn phòng","Đồ họa","Sinh viên","Khác"]
                    
                    Câu hỏi: %s
                    """.formatted(userMessage);

            String geminiAnalysisRaw = GeminiClient.sendPromptToGemini(analysisPrompt);
            String analysisText = extractGeminiText(geminiAnalysisRaw);

            String cleanedJson = analysisText.replaceAll("(?i)```json", "")
                    .replaceAll("(?i)```", "")
                    .trim();

            JsonNode extracted;
            try {
                extracted = mapper.readTree(cleanedJson);
            } catch (Exception jsonEx) {
                return toJsonReply("unknown", "", "Khác",
                        "Xin lỗi, tôi chưa hiểu rõ yêu cầu. Bạn có thể hỏi lại cụ thể hơn không?");
            }

            String productName = extracted.path("product_name").asText("");
            String intent = extracted.path("intent").asText("unknown");
            String category = extracted.path("category").asText("Khác");

            // ── Áp dụng rule keyword override ────────────────
            String lowerMsg = userMessage.toLowerCase();
            if (lowerMsg.contains("học tập") || lowerMsg.contains("sinh viên") || lowerMsg.contains("đi học")) {
                category = "Sinh viên";
            } else if (lowerMsg.contains("văn phòng") || lowerMsg.contains("soạn thảo")
                    || lowerMsg.contains("word") || lowerMsg.contains("excel")) {
                category = "Văn phòng";
            } else if (lowerMsg.contains("gaming") || lowerMsg.contains("game") || lowerMsg.contains("card rời")) {
                category = "Gaming";
            } else if (lowerMsg.contains("đồ họa") || lowerMsg.contains("thiết kế")
                    || lowerMsg.contains("render") || lowerMsg.contains("3d")) {
                category = "Đồ họa";
            }

            String productInfo;

            // ── Bước 2: Truy xuất dữ liệu sản phẩm ───────────────────────
            if (!productName.isEmpty()) {
                Product product = productService.findByProductName(productName);
                if (product != null) {
                    List<ProductDetail> details = productService.getProductDetails(product.getId());
                    String classified = ProductClassifier.classify(product, details);

                    // Nếu Gemini đoán khác classifier → ưu tiên classifier
                    if (!category.equals(classified)) {
                        category = classified;
                    }

                    productInfo = String.format(
                            "Tên: %s, số lượng còn: %d, giá: %,.0f VNĐ, dòng: %s",
                            product.getName(), product.getInStock(), product.getPrice(), category
                    );
                } else {
                    productInfo = "Không tìm thấy sản phẩm nào có tên \"" + productName + "\".";
                }
            } else if (!category.equals("Khác")) {
                // Người dùng chỉ nêu nhu cầu → lọc sản phẩm theo rule-based
                List<Product> candidates = productService.findAllEnabled();
                final String categoryFilter = category;

                List<Product> filtered = candidates.stream()
                        .filter(prod -> {
                            List<ProductDetail> details = productService.getProductDetails(prod.getId());
                            return ProductClassifier.classify(prod, details).equals(categoryFilter);
                        })
                        .collect(Collectors.toList());

                if (!filtered.isEmpty()) {
                    StringBuilder sb = new StringBuilder("Một số mẫu laptop dòng " + category + ":\n");
                    for (int i = 0; i < Math.min(filtered.size(), 3); i++) {
                        Product prod = filtered.get(i);
                        String productUrl = String.format("http://localhost:8081/FreshCart/p/%s", prod.getAlias());
                        sb.append(String.format(
                            "<li><b>%s</b>: %, .0f VNĐ<br>"
                            + "<a href='%s' target='_blank'>Xem chi tiết</a></li>",
                            prod.getName(), prod.getPrice(), productUrl
                        ));
                    }
                    sb.append("</ul>");
                    productInfo = sb.toString();
                } else {
                    productInfo = "Không tìm thấy laptop phù hợp trong dòng " + category + ".";
                }
            } else {
                productInfo = "Không xác định rõ sản phẩm hay dòng laptop nào.";
            }

            // ── Bước 3: Gửi RAG prompt để trả lời tự nhiên ───────────────
            String ragPrompt = """
                    Bạn là trợ lý tư vấn laptop. Đây là thông tin từ hệ thống:
                    
                    %s
                    
                    Lưu ý:
                        - Luôn hiển thị link "Xem chi tiết" đi kèm mỗi sản phẩm.
                        - KHÔNG sử dụng cú pháp Markdown như [Xem chi tiết](URL).
                        - Có thể thêm lời chào, nhận xét thân thiện, nhưng KHÔNG được xóa link hoặc định dạng HTML.
                        
                    Câu hỏi khách hàng: %s
                    
                     Hãy trả lời thân thiện, tự nhiên bằng tiếng Việt, sử dụng nguyên HTML đã cho.
                    """.formatted(productInfo, userMessage);

            String finalGeminiRaw = GeminiClient.sendPromptToGemini(ragPrompt);
            String finalAnswer = extractGeminiText(finalGeminiRaw);

            return toJsonReply(intent, productName, category, finalAnswer);

        } catch (Exception e) {
            e.printStackTrace();
            return toJsonReply("unknown", "", "Khác",
                    "Đã xảy ra lỗi: " + e.getMessage().replace("\"", "'"));
        }
    }

    // Hàm lấy text từ Gemini
    private String extractGeminiText(String geminiRaw) throws Exception {
        JsonNode root = mapper.readTree(geminiRaw);
        return root.at("/candidates/0/content/parts/0/text").asText().trim();
    }

    // Chuẩn hóa JSON trả về frontend
    private String toJsonReply(String intent, String productName, String category, String replyText) {
        try {
            String safeReply;
            // Nếu đã có HTML tag (<a>, <br>, <ul>, <li>), giữ nguyên
            if (replyText.contains("<a") || replyText.contains("<br") || replyText.contains("<ul") || replyText.contains("<li")) {
                safeReply = replyText;
            } else {
                safeReply = beautifyReply(replyText);
            }
    
            JsonNode node = mapper.createObjectNode()
                    .put("intent", intent)
                    .put("product_name", productName)
                    .put("category", category)
                    .put("reply", safeReply);
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"intent\":\"unknown\",\"product_name\":\"\",\"category\":\"Khác\",\"reply\":\"Lỗi tạo JSON\"}";
        }
    }

    private String beautifyReply(String reply) {
        if (reply == null || reply.isBlank()) return "";
        String text = reply
                .replaceAll("\\*\\*", "")
                .replaceAll("•", "</li><li>")
                .replaceAll("\\*", "</li><li>");
        if (text.contains("</li><li>")) {
            text = text.replaceFirst("</li><li>", "<ul><li>") + "</li></ul>";
        }
        return text;
    }

}
