package com.supplyforge.ai.api.dto;

public class LiquidationROIResult {

    private String skuId;
    private String skuName;
    private int quantity;
    private double originalCostBasis;           // Giá vốn ban đầu (cost * qty)
    private double accumulatedStorageCost;       // Phí lưu kho lũy kế
    private double totalSunkCost;               // originalCost + storageCost
    private double expectedRevenue;             // Doanh thu xả hàng dự kiến
    private double marketingAndShippingCost;    // Chi phí marketing + vận chuyển ước tính
    private double grossRecoveryRate;           // Tỷ lệ thu hồi gộp (%)
    private double netRecoveryRate;             // Tỷ lệ thu hồi ròng (%)
    private double breakEvenDiscountPct;        // Ngưỡng giảm giá tối đa trước khi âm dòng tiền
    private double taxShieldValueVnd;           // Giá trị lá chắn thuế ước tính
    private String matrixQuadrant;             // HIGH_VALUE_NEW | MID_VALUE_OLD | LOW_VALUE_OLD
    private String accountingMethod;           // FIFO | LIFO
    private String writeOffStrategy;           // Chiến lược ghi nhận lỗ
    private String aiRecommendation;           // Khuyến nghị chiến lược tổng hợp
    private boolean isTaxShieldRecommended;    // True khi hàng không còn khả năng xả hiệu quả

    public LiquidationROIResult() {}

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public String getSkuId() { return skuId; }
    public void setSkuId(String skuId) { this.skuId = skuId; }

    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getOriginalCostBasis() { return originalCostBasis; }
    public void setOriginalCostBasis(double originalCostBasis) { this.originalCostBasis = originalCostBasis; }

    public double getAccumulatedStorageCost() { return accumulatedStorageCost; }
    public void setAccumulatedStorageCost(double accumulatedStorageCost) { this.accumulatedStorageCost = accumulatedStorageCost; }

    public double getTotalSunkCost() { return totalSunkCost; }
    public void setTotalSunkCost(double totalSunkCost) { this.totalSunkCost = totalSunkCost; }

    public double getExpectedRevenue() { return expectedRevenue; }
    public void setExpectedRevenue(double expectedRevenue) { this.expectedRevenue = expectedRevenue; }

    public double getMarketingAndShippingCost() { return marketingAndShippingCost; }
    public void setMarketingAndShippingCost(double marketingAndShippingCost) { this.marketingAndShippingCost = marketingAndShippingCost; }

    public double getGrossRecoveryRate() { return grossRecoveryRate; }
    public void setGrossRecoveryRate(double grossRecoveryRate) { this.grossRecoveryRate = grossRecoveryRate; }

    public double getNetRecoveryRate() { return netRecoveryRate; }
    public void setNetRecoveryRate(double netRecoveryRate) { this.netRecoveryRate = netRecoveryRate; }

    public double getBreakEvenDiscountPct() { return breakEvenDiscountPct; }
    public void setBreakEvenDiscountPct(double breakEvenDiscountPct) { this.breakEvenDiscountPct = breakEvenDiscountPct; }

    public double getTaxShieldValueVnd() { return taxShieldValueVnd; }
    public void setTaxShieldValueVnd(double taxShieldValueVnd) { this.taxShieldValueVnd = taxShieldValueVnd; }

    public String getMatrixQuadrant() { return matrixQuadrant; }
    public void setMatrixQuadrant(String matrixQuadrant) { this.matrixQuadrant = matrixQuadrant; }

    public String getAccountingMethod() { return accountingMethod; }
    public void setAccountingMethod(String accountingMethod) { this.accountingMethod = accountingMethod; }

    public String getWriteOffStrategy() { return writeOffStrategy; }
    public void setWriteOffStrategy(String writeOffStrategy) { this.writeOffStrategy = writeOffStrategy; }

    public String getAiRecommendation() { return aiRecommendation; }
    public void setAiRecommendation(String aiRecommendation) { this.aiRecommendation = aiRecommendation; }

    public boolean isTaxShieldRecommended() { return isTaxShieldRecommended; }
    public void setTaxShieldRecommended(boolean taxShieldRecommended) { isTaxShieldRecommended = taxShieldRecommended; }
}
