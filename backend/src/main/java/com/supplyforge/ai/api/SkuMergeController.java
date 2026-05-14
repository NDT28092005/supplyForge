package com.supplyforge.ai.api;

import com.supplyforge.ai.application.SkuNormalizationService;
import com.supplyforge.ai.domain.entity.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/sku-merge")
public class SkuMergeController {

    private final SkuNormalizationService skuNormalizationService;

    public SkuMergeController(SkuNormalizationService skuNormalizationService) {
        this.skuNormalizationService = skuNormalizationService;
    }

    /**
     * DTO để trả về đúng cấu trúc Frontend (SkuMergeCard.tsx) mong đợi
     */
    public static class SkuPairDTO {
        public String skuAId;
        public String skuAName;
        public String skuBId;
        public String skuBName;
        public int distance;

        public SkuPairDTO(String skuAId, String skuAName, String skuBId, String skuBName, int distance) {
            this.skuAId = skuAId;
            this.skuAName = skuAName;
            this.skuBId = skuBId;
            this.skuBName = skuBName;
            this.distance = distance;
        }
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<SkuPairDTO>> getCandidates(
            @PathVariable Long workspaceId,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {
        
        List<List<Product>> clusters = skuNormalizationService.findDuplicateCandidates(userId);
        List<SkuPairDTO> pairs = new ArrayList<>();

                    // Chuyển đổi từ Cluster (List<Product>) sang các cặp (Pair) để hiện lên UI kiểu Tinder
                    for (List<Product> cluster : clusters) {
                        if (cluster.size() >= 2) {
                            Product pA = cluster.get(0);
                            for (int i = 1; i < cluster.size(); i++) {
                                Product pB = cluster.get(i);
                                // Tính distance giả lập hoặc lấy từ logic similarity (ở đây tạm để 2)
                                pairs.add(new SkuPairDTO(pA.getId(), pA.getProductName(), pB.getId(), pB.getProductName(), 2));
                            }
                        }
                    }
        return ResponseEntity.ok(pairs);
    }

    @PostMapping("/merge")
    public ResponseEntity<String> mergeProducts(
            @PathVariable Long workspaceId,
            @RequestBody Map<String, Object> payload) {
        try {
            // Khớp với payload từ SkuMergeCard.tsx: { parentSkuId, childSkuId }
            String parentId = payload.get("parentSkuId").toString();
            String childId = payload.get("childSkuId").toString();
            
            skuNormalizationService.mergeProducts(parentId, List.of(childId));
            return ResponseEntity.ok("Merged successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Merge failed: " + e.getMessage());
        }
    }
}
