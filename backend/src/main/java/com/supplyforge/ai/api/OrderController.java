package com.supplyforge.ai.api;

import com.supplyforge.ai.application.OrderSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderSyncService orderSyncService;

    public OrderController(OrderSyncService orderSyncService) {
        this.orderSyncService = orderSyncService;
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncOrders(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            orderSyncService.syncOrdersFromJson(file.getInputStream(), userId);
            return ResponseEntity.ok("Sync initiated successfully in batch mode.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Sync failed: " + e.getMessage());
        }
    }
}
