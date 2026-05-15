package com.supplyforge.ai.api;

import com.supplyforge.ai.api.dto.DashboardInsightDTO;
import com.supplyforge.ai.api.dto.InventoryAgingDTO;
import com.supplyforge.ai.api.dto.LiquidationROIResult;
import com.supplyforge.ai.application.FinancialLiquidationEngine;
import com.supplyforge.ai.application.InventoryFinancialEngineService;
import com.supplyforge.ai.application.InventoryInsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/dashboard")
public class DashboardController {

    private final InventoryInsightService inventoryInsightService;
    private final InventoryFinancialEngineService inventoryFinancialEngineService;
    private final FinancialLiquidationEngine financialLiquidationEngine;

    public DashboardController(InventoryInsightService inventoryInsightService,
                               InventoryFinancialEngineService inventoryFinancialEngineService,
                               FinancialLiquidationEngine financialLiquidationEngine) {
        this.inventoryInsightService = inventoryInsightService;
        this.inventoryFinancialEngineService = inventoryFinancialEngineService;
        this.financialLiquidationEngine = financialLiquidationEngine;
    }

    @PostMapping("/dead-stock/refresh")
    public ResponseEntity<DashboardInsightDTO> refreshDashboard(
            @PathVariable Long workspaceId,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {
        return ResponseEntity.ok(inventoryInsightService.getDashboardInsights(userId));
    }

    @GetMapping("/dead-stock")
    public ResponseEntity<DashboardInsightDTO> getDashboard(
            @PathVariable Long workspaceId,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {
        return ResponseEntity.ok(inventoryInsightService.getDashboardInsights(userId));
    }

    @GetMapping("/aging")
    public ResponseEntity<InventoryAgingDTO> getInventoryAging(
            @PathVariable Long workspaceId,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId) {
        return ResponseEntity.ok(inventoryFinancialEngineService.calculateAgingAndHiddenCosts(userId));
    }

    /**
     * NEW: CFO Financial Analysis — ROI, Break-even & Matrix
     * GET /api/v1/workspaces/{id}/dashboard/financial-analysis?userId=xxx&discount=0.35&accounting=FIFO
     */
    @GetMapping("/financial-analysis")
    public ResponseEntity<LiquidationROIResult> getFinancialAnalysis(
            @PathVariable Long workspaceId,
            @RequestParam(value = "userId", defaultValue = "user_default_001") String userId,
            @RequestParam(value = "discount", defaultValue = "0.35") double discountPct,
            @RequestParam(value = "accounting", defaultValue = "FIFO") String accountingMethod) {

        InventoryAgingDTO aging = inventoryFinancialEngineService.calculateAgingAndHiddenCosts(userId);

        // Ưu tiên Dead Stock, fallback về Slow Moving
        var targetList = !aging.getDeadStockSkus().isEmpty()
                ? aging.getDeadStockSkus()
                : aging.getSlowMovingSkus();

        LiquidationROIResult roi = financialLiquidationEngine.calculateForTopDeadSku(
                targetList, discountPct, accountingMethod);

        if (roi == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(roi);
    }
}
