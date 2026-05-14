package com.supplyforge.ai.api;

import com.supplyforge.ai.api.dto.DashboardInsightDTO;
import com.supplyforge.ai.api.dto.InventoryAgingDTO;
import com.supplyforge.ai.application.InventoryFinancialEngineService;
import com.supplyforge.ai.application.InventoryInsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/dashboard")
public class DashboardController {

    private final InventoryInsightService inventoryInsightService;
    private final InventoryFinancialEngineService inventoryFinancialEngineService;

    public DashboardController(InventoryInsightService inventoryInsightService,
                               InventoryFinancialEngineService inventoryFinancialEngineService) {
        this.inventoryInsightService = inventoryInsightService;
        this.inventoryFinancialEngineService = inventoryFinancialEngineService;
    }

    /**
     * Endpoint mà Frontend đang gọi để lấy dữ liệu Dashboard (Dead Stock cũ)
     */
    @PostMapping("/dead-stock/refresh")
    public ResponseEntity<DashboardInsightDTO> refreshDashboard(
            @PathVariable Long workspaceId,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {

        DashboardInsightDTO insights = inventoryInsightService.getDashboardInsights(userId);
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/dead-stock")
    public ResponseEntity<DashboardInsightDTO> getDashboard(
            @PathVariable Long workspaceId,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {
        return ResponseEntity.ok(inventoryInsightService.getDashboardInsights(userId));
    }

    /**
     * NEW: Inventory Aging + Hidden Cost Engine
     * Endpoint mới trả về phân loại 4 nhóm vòng đời và tổng chi phí ẩn mỗi tháng.
     * GET /api/v1/workspaces/{id}/dashboard/aging?userId=xxx
     */
    @GetMapping("/aging")
    public ResponseEntity<InventoryAgingDTO> getInventoryAging(
            @PathVariable Long workspaceId,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {
        InventoryAgingDTO aging = inventoryFinancialEngineService.calculateAgingAndHiddenCosts(userId);
        return ResponseEntity.ok(aging);
    }
}
