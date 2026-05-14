package com.supplyforge.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplyforge.ai.api.dto.AiMappingRuleDTO;
import com.supplyforge.ai.api.dto.FileSampleDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiMappingService {

    @Value("${supplyforge.gemini.api-key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    @Value("${supplyforge.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    private final ObjectMapper objectMapper;

    public AiMappingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * TASK 2: AI AUTO-MAPPING ENGINE (Triệt để theo ai.google.dev)
     */
    public AiMappingRuleDTO generateMappingRules(FileSampleDTO sampleDTO) throws Exception {
        validateConfig();

        String prompt = String.format("""
            Bạn là chuyên gia về cấu trúc dữ liệu Ecommerce. 
            Nhiệm vụ: Ánh xạ các cột tiêu đề từ file người dùng vào Schema Database chuẩn của tôi.
            
            Schema Database: [itemSku, productName, price, cost, stock, orderDate, orderId, quantity].
            
            Dữ liệu người dùng cung cấp:
            - Headers: %s
            - Sample Data: %s
            
            Yêu cầu: Trả về JSON chứa key 'mapping_rules' bao gồm: dbField, csvHeader, confidence (0-1). 
            Bỏ qua các cột rác hoặc không có giá trị ánh xạ.
            """, sampleDTO.getHeaders().toString(), sampleDTO.getSampleRows().toString());

        // Chuẩn bị JSON Body theo tài liệu ai.google.dev
        Map<String, Object> requestBody = new HashMap<>();
        
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        // Ép kiểu trả về JSON chuẩn
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        String jsonRequest = objectMapper.writeValueAsString(requestBody);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // Sử dụng Endpoint v1beta để hỗ trợ đầy đủ các tính năng mới nhất
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", 
                     geminiModel, geminiApiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            handleError(response);
        }

        String responseBody = response.body();
        // Sanitization: Remove markdown code blocks if present
        if (responseBody.contains("```json")) {
            responseBody = responseBody.substring(responseBody.indexOf("```json") + 7);
            responseBody = responseBody.substring(0, responseBody.lastIndexOf("```"));
        } else if (responseBody.contains("```")) {
            responseBody = responseBody.substring(responseBody.indexOf("```") + 3);
            responseBody = responseBody.substring(0, responseBody.lastIndexOf("```"));
        }

        return parseResponse(responseBody);
    }

    private void validateConfig() {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("LỖI: Chưa tìm thấy GEMINI_API_KEY. Hãy kiểm tra biến môi trường.");
        }
    }

    private void handleError(HttpResponse<String> response) {
        String msg = String.format("Lỗi Gemini API (%d): %s", response.statusCode(), response.body());
        if (response.statusCode() == 429) {
            msg = "Hệ thống AI đang bận hoặc hết hạn mức (Quota). Vui lòng thử lại sau 30 giây.";
        } else if (response.statusCode() == 404) {
            msg = "Model AI '" + geminiModel + "' không tồn tại hoặc không được hỗ trợ tại khu vực này.";
        }
        throw new RuntimeException(msg);
    }

    private AiMappingRuleDTO parseResponse(String body) throws Exception {
        Map<String, Object> responseMap = objectMapper.readValue(body, Map.class);
        
        // Google AI returns candidates list
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("AI không trả về kết quả gợi ý nào (Candidates empty).");
        }

        Map<String, Object> contentObj = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentObj.get("parts");
        String contentText = (String) parts.get(0).get("text").toString().trim();

        // Sanitization for inner JSON as well
        if (contentText.startsWith("```json")) {
            contentText = contentText.substring(7, contentText.length() - 3).trim();
        } else if (contentText.startsWith("```")) {
            contentText = contentText.substring(3, contentText.length() - 3).trim();
        }

        // Map snake_case from AI to camelCase for DTO if needed, or just let Jackson handle it
        try {
            return objectMapper.readValue(contentText, AiMappingRuleDTO.class);
        } catch (Exception e) {
            // Fallback: If AI returns just the list without 'mapping_rules' key
            try {
                List<AiMappingRuleDTO.MappingRule> rules = objectMapper.readValue(contentText, 
                    new com.fasterxml.jackson.core.type.TypeReference<List<AiMappingRuleDTO.MappingRule>>() {});
                AiMappingRuleDTO fallbackDto = new AiMappingRuleDTO();
                fallbackDto.setMappingRules(rules);
                return fallbackDto;
            } catch (Exception e2) {
                throw new RuntimeException("Lỗi phân giải JSON từ AI: " + e.getMessage() + ". Nội dung: " + contentText);
            }
        }
    }
}
