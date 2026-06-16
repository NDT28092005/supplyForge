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
     * DTO để trả về đúng cấu trúc Frontend mong đợi
     */
    public static class SkuPairDTO {
        public String skuAId;
        public String skuAName;
        public String skuACode;
        public double skuAPrice;
        public int skuAStock;
        public String skuASource;

        public String skuBId;
        public String skuBName;
        public String skuBCode;
        public double skuBPrice;
        public int skuBStock;
        public String skuBSource;

        public int distance;

        public SkuPairDTO(Product a, Product b, int distance) {
            this.skuAId = a.getId();
            this.skuAName = a.getProductName();
            this.skuACode = a.getItemSku();
            this.skuAPrice = a.getPrice() != null ? a.getPrice().doubleValue() : 0;
            this.skuAStock = a.getStock() != null ? a.getStock() : 0;
            this.skuASource = a.getPlatform() != null ? a.getPlatform() : "Hệ thống";

            this.skuBId = b.getId();
            this.skuBName = b.getProductName();
            this.skuBCode = b.getItemSku();
            this.skuBPrice = b.getPrice() != null ? b.getPrice().doubleValue() : 0;
            this.skuBStock = b.getStock() != null ? b.getStock() : 0;
            this.skuBSource = b.getPlatform() != null ? b.getPlatform() : "Hệ thống";

            this.distance = distance;
        }
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<SkuPairDTO>> getCandidates(
            @PathVariable Long workspaceId,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {
        
        List<List<Product>> clusters = skuNormalizationService.findDuplicateCandidates(userId);
        List<SkuPairDTO> pairs = new ArrayList<>();

        for (List<Product> cluster : clusters) {
            if (cluster.size() >= 2) {
                Product pA = cluster.get(0);
                for (int i = 1; i < cluster.size(); i++) {
                    Product pB = cluster.get(i);
                    pairs.add(new SkuPairDTO(pA, pB, 2));
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
