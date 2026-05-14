package com.supplyforge.ai.api;

import com.supplyforge.ai.api.dto.AiMappingRuleDTO;
import com.supplyforge.ai.api.dto.FileSampleDTO;
import com.supplyforge.ai.application.AiMappingService;
import com.supplyforge.ai.application.DynamicFileImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/import")
public class FileImportController {

    private final DynamicFileImportService dynamicFileImportService;
    private final AiMappingService aiMappingService;

    public FileImportController(DynamicFileImportService dynamicFileImportService, AiMappingService aiMappingService) {
        this.dynamicFileImportService = dynamicFileImportService;
        this.aiMappingService = aiMappingService;
    }

    /**
     * API Giai đoạn 1: Upload file, trích xuất mẫu và gọi AI mapping
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeFile(@RequestParam("file") MultipartFile file) {
        try {
            // Task 1: Trích xuất mẫu
            FileSampleDTO sampleDTO = dynamicFileImportService.peekSampleData(file);

            // Task 2: Gọi AI để đề xuất Mapping
            AiMappingRuleDTO mappingRuleDTO = aiMappingService.generateMappingRules(sampleDTO);

            // Gộp response trả về cho Frontend
            Map<String, Object> response = new HashMap<>();
            response.put("headers", sampleDTO.getHeaders());
            // Chỉ trả về 1 dòng sample đầu tiên cho giao diện (theo yêu cầu Task 3: hiển thị 1 giá trị mẫu)
            response.put("sampleData", sampleDTO.getSampleRows().isEmpty() ? null : sampleDTO.getSampleRows().get(0));
            response.put("aiSuggestions", mappingRuleDTO.getMappingRules());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * API Giai đoạn 2 (Task 4): Nhận file và FinalMappingRule để xử lý Batch Insert
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mappingRule") String mappingRuleJson,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, String> finalMappingRule = mapper.readValue(mappingRuleJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
            
            dynamicFileImportService.processBatchImport(file, finalMappingRule, userId);
            
            return ResponseEntity.ok(Map.of("message", "Batch import processed successfully."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearData(@RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {
        try {
            dynamicFileImportService.clearData(userId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa dữ liệu cũ thành công."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
