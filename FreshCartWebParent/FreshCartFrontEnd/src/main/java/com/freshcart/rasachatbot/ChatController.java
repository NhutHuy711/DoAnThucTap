package com.freshcart.rasachatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final Map<String, Map<String, String>> userSlots = new HashMap<>();
    private static final String FASTAPI_URL = "http://localhost:8000";
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> payload) {
        String userMessage = payload.getOrDefault("message", "");

        // Gọi thẳng Rasa (REST webhook)
        Object rasaResponse = rasaChatService.sendMessageToRasa(userMessage);

        // Chuẩn hóa output: chỉ giữ text & buttons do Rasa/Python trả về
        List<Map<String, Object>> messages = new ArrayList<>();
        if (rasaResponse instanceof List) {
            List<Map<String, Object>> rasaList = (List<Map<String, Object>>) rasaResponse;
            for (Map<String, Object> msg : rasaList) {
                Map<String, Object> m = new HashMap<>();
                if (msg.get("text") != null)    m.put("text", msg.get("text"));
                if (msg.get("buttons") != null) m.put("buttons", msg.get("buttons"));
                messages.add(m);
            }
        } else {
            messages.add(Map.of("text", "Unexpected response format from Rasa."));
        }

        // Không cần intent/confidence nữa, nhưng nếu frontend đang dùng thì để mặc định
        Map<String, Object> resp = new HashMap<>();
        resp.put("intent", "pass_through");
        resp.put("confidence", 1.0);
        resp.put("messages", messages);
        return ResponseEntity.ok(resp);
    }


}