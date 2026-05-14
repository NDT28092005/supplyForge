package com.supplyforge.ai.infrastructure.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supplyforge.ai.config.GeminiProperties;
import com.supplyforge.ai.infrastructure.excel.ExcelPeekResult;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GeminiColumnMappingService {

    private static final String SYSTEM_PROMPT =
            """
            Bạn là chuyên gia dữ liệu Ecommerce Đông Nam Á. Nhiệm vụ: từ header + vài dòng mẫu, \
            xác định chỉ số cột (0-based) map vào: SKU/tên hàng, số lượng, giá bán (hoặc giá vốn nếu chỉ có một cột giá).
            Trả về DUY NHẤT một JSON object với các khóa:
            sku_column_index (int),
            quantity_column_index (int),
            selling_price_column_index (int hoặc null nếu không có),
            cost_price_column_index (int hoặc null),
            record_date_column_index (int hoặc null),
            price_is_cost (boolean, true nếu cột giá duy nhất là giá vốn),
            confidence (string: high|medium|low),
            notes (string ngắn).
            Tên cột có thể tiếng Việt sai chính tả. Nếu không chắc, chọn mức confidence thấp và giải thích ngắn trong notes.""";

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    public GeminiColumnMappingService(
            RestClient geminiRestClient, GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.geminiRestClient = geminiRestClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    public JsonNode inferColumnMapping(ExcelPeekResult peek) {
        if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
            return heuristicMapping(peek);
        }
        try {
            // Gemini generateContent body structure
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode contents = body.putArray("contents");
            ObjectNode part = contents.addObject().putArray("parts").addObject();
            
            String prompt = SYSTEM_PROMPT + "\n\n" + buildUserContent(peek);
            part.put("text", prompt);

            ObjectNode generationConfig = body.putObject("generationConfig");
            generationConfig.put("response_mime_type", "application/json");
            generationConfig.put("temperature", 0.1);

            // POST /v1beta/models/{model}:generateContent
            String uri = "/v1beta/models/" + geminiProperties.getModel() + ":generateContent";

            JsonNode response = geminiRestClient
                    .post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("candidates")) {
                return heuristicMapping(peek);
            }

            String content = response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
            JsonNode mapping = objectMapper.readTree(content);
            if (!mapping.has("sku_column_index")) {
                return heuristicMapping(peek);
            }
            ((ObjectNode) mapping).put("source", "gemini");
            return mapping;
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return heuristicMapping(peek);
        }
    }

    private JsonNode heuristicMapping(ExcelPeekResult peek) {
        List<String> h = peek.headers().stream().map(String::toLowerCase).toList();
        ObjectNode o = objectMapper.createObjectNode();

        o.put("sku_column_index", findIndex(h, List.of("sku", "mã", "code"), 0));
        o.put("quantity_column_index", findIndex(h, List.of("quantity", "số lượng", "qty", "sl"), 1));
        o.put("selling_price_column_index", findIndex(h, List.of("selling_price", "giá bán", "price", "giá"), 2));
        o.put("cost_price_column_index", findIndex(h, List.of("cost_price", "giá vốn", "cost", "vốn"), 3));
        o.put("record_date_column_index", findIndex(h, List.of("record_date", "ngày", "date"), 5));

        o.put("price_is_cost", false);
        o.put("confidence", "medium");
        o.put("notes", "Heuristic Mapping (không dùng AI do thiếu API Key).");
        o.put("source", "heuristic_improved");
        return o;
    }

    private int findIndex(List<String> headers, List<String> keywords, int fallback) {
        for (int i = 0; i < headers.size(); i++) {
            String head = headers.get(i);
            for (String kw : keywords) {
                if (head.contains(kw)) return i;
            }
        }
        return fallback;
    }

    private String buildUserContent(ExcelPeekResult peek) {
        StringBuilder sb = new StringBuilder();
        sb.append("Header (cột 0..n-1):\n");
        sb.append(String.join("\t", peek.headers()));
        sb.append("\n\nMẫu tối đa 5 dòng:\n");
        for (List<String> row : peek.sampleRows()) {
            sb.append(String.join("\t", row)).append('\n');
        }
        return sb.toString();
    }
}
