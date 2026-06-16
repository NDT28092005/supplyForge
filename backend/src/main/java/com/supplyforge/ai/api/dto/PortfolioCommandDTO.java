package com.supplyforge.ai.api.dto;

import java.util.List;

/**
 * PortfolioCommandDTO — Response wrapper cho Portfolio Command Center.
 *
 * Chỉ chứa TOP 3 clusters (Capacity Limiter) + metadata tổng hợp
 * để Frontend render "Kế hoạch thu hồi dòng tiền tuần này".
 */
public class PortfolioCommandDTO {

    /** Tổng tiền kỳ vọng thu hồi từ 3 cluster (₫). */
    private double totalExpectedRecovery;

    /** Tổng số SKU cần xử lý (tổng 3 cluster). */
    private int totalSkuCount;

    /**
     * % không gian kho dự kiến được giải phóng.
     * Ước tính: tổng qty xả / tổng qty tồn kho × 100.
     */
    private double estimatedWarehouseSpaceFreedPct;

    /** Khung thời gian dự kiến hoàn thành (ngày). */
    private int planHorizonDays;

    /**
     * TOP 3 clusters đã được xếp hạng theo averageRiskScore.
     * Capacity Limiter đảm bảo chỉ có đúng 3 phần tử.
     */
    private List<RecoveryClusterDTO> priorityClusters;

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public double getTotalExpectedRecovery() { return totalExpectedRecovery; }
    public void setTotalExpectedRecovery(double totalExpectedRecovery) { this.totalExpectedRecovery = totalExpectedRecovery; }

    public int getTotalSkuCount() { return totalSkuCount; }
    public void setTotalSkuCount(int totalSkuCount) { this.totalSkuCount = totalSkuCount; }

    public double getEstimatedWarehouseSpaceFreedPct() { return estimatedWarehouseSpaceFreedPct; }
    public void setEstimatedWarehouseSpaceFreedPct(double estimatedWarehouseSpaceFreedPct) { this.estimatedWarehouseSpaceFreedPct = estimatedWarehouseSpaceFreedPct; }

    public int getPlanHorizonDays() { return planHorizonDays; }
    public void setPlanHorizonDays(int planHorizonDays) { this.planHorizonDays = planHorizonDays; }

    public List<RecoveryClusterDTO> getPriorityClusters() { return priorityClusters; }
    public void setPriorityClusters(List<RecoveryClusterDTO> priorityClusters) { this.priorityClusters = priorityClusters; }
}
