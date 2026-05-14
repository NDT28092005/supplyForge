package com.supplyforge.ai.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record DeadStockDashboardResponse(
        BigDecimal totalDeadStockValueVnd,
        BigDecimal estimatedLoss30DaysVnd,
        BigDecimal totalInventoryValueVnd,
        int staleSkuCount,
        int totalRootSkuCount,
        List<StaleSkuLine> topStaleSkus) {

    public record StaleSkuLine(long skuId, String name, long frozenValueVnd, String lastRecordDate) {}
}
