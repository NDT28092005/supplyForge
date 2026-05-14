package com.supplyforge.ai.api;

import com.supplyforge.ai.application.ProductSyncService;
import com.supplyforge.ai.domain.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductSyncService productSyncService;
    private final ProductRepository productRepository;

    public ProductController(ProductSyncService productSyncService, ProductRepository productRepository) {
        this.productSyncService = productSyncService;
        this.productRepository = productRepository;
    }

    @GetMapping("/check-data")
    public ResponseEntity<Map<String, Object>> checkData() {
        long count = productRepository.count();
        return ResponseEntity.ok(Map.of("hasData", count > 0, "count", count));
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncProducts(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {
        try {
            productSyncService.syncProductsFromJson(file.getInputStream(), userId);
            return ResponseEntity.ok("Products synced successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Sync failed: " + e.getMessage());
        }
    }
}
