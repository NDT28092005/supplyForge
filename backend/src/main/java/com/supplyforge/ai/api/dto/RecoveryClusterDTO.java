package com.supplyforge.ai.api.dto;

import java.util.List;

/**
 * RecoveryClusterDTO — Nhóm SKU được gom lại theo thuộc tính chung.
 *
 * Thay thế danh sách SKU rời rạc bằng "chiến dịch" có thể thực thi hàng loạt.
 * Backend chỉ trả TOP 3 clusters theo averageRiskScore.
 */
public class RecoveryClusterDTO {

    /** Mã định danh nhóm (slug, ví dụ: "FASHION_DEAD_CRITICAL"). */
    private String clusterId;

    /** Tên chiến dịch thân thiện với người dùng (VD: "Xả hàng Quần áo Mùa Đông"). */
    private String clusterName;

    /** Mô tả ngắn lý do gom nhóm. */
    private String rationale;

    /** Tổng số SKU trong nhóm. */
    private int skuCount;

    /** Tổng giá trị vốn đóng băng (₫). */
    private double totalFrozenValue;

    /**
     * Tổng tiền kỳ vọng thu hồi (₫).
     * = totalFrozenValue × recoveryRate tương ứng với giai đoạn thanh lý.
     */
    private double totalPotentialRecovery;

    /** Điểm rủi ro trung bình (0–100) — dùng để xếp hạng ưu tiên thực thi. */
    private double averageRiskScore;

    /** Mức độ cấp bách: CRITICAL (80+), HIGH (60–79), MEDIUM (40–59). */
    private String urgencyLevel;

    /** Ngành nghề chính của nhóm (ELECTRONICS_TECH, FASHION_FAST, ...). */
    private String industryCode;

    /** Chiến lược hành động hàng loạt gợi ý (VD: "Flash Sale TikTok −18%"). */
    private String recommendedBatchAction;

    /** Thời gian dự kiến giải phóng kho (ngày). */
    private int estimatedClearDays;

    /** Top SKU đại diện trong nhóm để hiển thị preview. */
    private List<SkuSummaryDTO> topSkuSamples;

    // ─── Inner DTO: Tóm tắt SKU trong nhóm ──────────────────────────────────
    public static class SkuSummaryDTO {
        private String name;
        private int daysInInventory;
        private double totalValue;
        private double riskScore;

        public SkuSummaryDTO(String name, int daysInInventory, double totalValue, double riskScore) {
            this.name = name;
            this.daysInInventory = daysInInventory;
            this.totalValue = totalValue;
            this.riskScore = riskScore;
        }

        public String getName() { return name; }
        public int getDaysInInventory() { return daysInInventory; }
        public double getTotalValue() { return totalValue; }
        public double getRiskScore() { return riskScore; }
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public int getSkuCount() { return skuCount; }
    public void setSkuCount(int skuCount) { this.skuCount = skuCount; }

    public double getTotalFrozenValue() { return totalFrozenValue; }
    public void setTotalFrozenValue(double totalFrozenValue) { this.totalFrozenValue = totalFrozenValue; }

    public double getTotalPotentialRecovery() { return totalPotentialRecovery; }
    public void setTotalPotentialRecovery(double totalPotentialRecovery) { this.totalPotentialRecovery = totalPotentialRecovery; }

    public double getAverageRiskScore() { return averageRiskScore; }
    public void setAverageRiskScore(double averageRiskScore) { this.averageRiskScore = averageRiskScore; }

    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public String getIndustryCode() { return industryCode; }
    public void setIndustryCode(String industryCode) { this.industryCode = industryCode; }

    public String getRecommendedBatchAction() { return recommendedBatchAction; }
    public void setRecommendedBatchAction(String recommendedBatchAction) { this.recommendedBatchAction = recommendedBatchAction; }

    public int getEstimatedClearDays() { return estimatedClearDays; }
    public void setEstimatedClearDays(int estimatedClearDays) { this.estimatedClearDays = estimatedClearDays; }

    public List<SkuSummaryDTO> getTopSkuSamples() { return topSkuSamples; }
    public void setTopSkuSamples(List<SkuSummaryDTO> topSkuSamples) { this.topSkuSamples = topSkuSamples; }
}
