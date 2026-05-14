package com.supplyforge.ai.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardInsightDTO {
    // Các trường khớp với frontend (page.tsx)
    private double totalDeadStockValueVnd;
    private double estimatedLoss30DaysVnd;
    private double totalInventoryValueVnd;
    private int staleSkuCount;
    private int totalRootSkuCount;
    private List<StaleSkuDTO> topStaleSkus;

    public static class StaleSkuDTO {
        private String skuId;
        private String name;
        private double frozenValueVnd;
        private String lastRecordDate;

        public StaleSkuDTO(String skuId, String name, double frozenValueVnd, String lastRecordDate) {
            this.skuId = skuId;
            this.name = name;
            this.frozenValueVnd = frozenValueVnd;
            this.lastRecordDate = lastRecordDate;
        }

        // Getters
        public String getSkuId() { return skuId; }
        public String getName() { return name; }
        public double getFrozenValueVnd() { return frozenValueVnd; }
        public String getLastRecordDate() { return lastRecordDate; }
    }

    // Getters & Setters cho DashboardInsightDTO
    public double getTotalDeadStockValueVnd() { return totalDeadStockValueVnd; }
    public void setTotalDeadStockValueVnd(double totalDeadStockValueVnd) { this.totalDeadStockValueVnd = totalDeadStockValueVnd; }
    public double getEstimatedLoss30DaysVnd() { return estimatedLoss30DaysVnd; }
    public void setEstimatedLoss30DaysVnd(double estimatedLoss30DaysVnd) { this.estimatedLoss30DaysVnd = estimatedLoss30DaysVnd; }
    public double getTotalInventoryValueVnd() { return totalInventoryValueVnd; }
    public void setTotalInventoryValueVnd(double totalInventoryValueVnd) { this.totalInventoryValueVnd = totalInventoryValueVnd; }
    public int getStaleSkuCount() { return staleSkuCount; }
    public void setStaleSkuCount(int staleSkuCount) { this.staleSkuCount = staleSkuCount; }
    public int getTotalRootSkuCount() { return totalRootSkuCount; }
    public void setTotalRootSkuCount(int totalRootSkuCount) { this.totalRootSkuCount = totalRootSkuCount; }
    public List<StaleSkuDTO> getTopStaleSkus() { return topStaleSkus; }
    public void setTopStaleSkus(List<StaleSkuDTO> topStaleSkus) { this.topStaleSkus = topStaleSkus; }
}
