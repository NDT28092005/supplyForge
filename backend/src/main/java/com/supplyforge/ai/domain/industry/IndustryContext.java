package com.supplyforge.ai.domain.industry;

import java.util.List;

/**
 * IndustryContext — Value Object truyền Industry Intelligence ra ngoài API.
 *
 * Chứa đầy đủ thông tin để Frontend (RecoveryCopilot.tsx) render:
 *   - Trust Theater: "Bộ quy tắc ngành [Tên Ngành]"
 *   - Risk Factors badges
 *   - Dynamic Action Plan (liquidationStrategies)
 *   - Aging Score và Multiplier để hiển thị visualization
 */
public class IndustryContext {

    private String industryCode;           // FASHION_FAST, ELECTRONICS_TECH, etc.
    private String industryDisplayName;    // "Thời trang nhanh", "Điện tử & Công nghệ"
    private double agingRiskMultiplier;    // 1.8, 1.2, 2.5...
    private int maxShelfLifeDays;          // 60, 180, 30...
    private double agingScore;             // daysInInventory × multiplier
    private String agingBucket;            // HEALTHY | WATCHLIST | SLOW_MOVING | DEAD_STOCK
    private List<String> liquidationStrategies;
    private List<String> riskFactors;

    public IndustryContext() {}

    /** Factory method: Build từ Playbook + số ngày tồn kho. */
    public static IndustryContext from(IndustryPlaybook playbook, int daysInInventory) {
        IndustryContext ctx = new IndustryContext();
        ctx.industryCode = playbook.name();
        ctx.industryDisplayName = playbook.getDisplayName();
        ctx.agingRiskMultiplier = playbook.getAgingRiskMultiplier();
        ctx.maxShelfLifeDays = playbook.getMaxShelfLifeDays();
        ctx.agingScore = playbook.calculateAgingScore(daysInInventory);
        ctx.agingBucket = playbook.classifyAgingBucket(daysInInventory);
        ctx.liquidationStrategies = playbook.getLiquidationStrategies();
        ctx.riskFactors = playbook.getRiskFactors();
        return ctx;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public String getIndustryCode()                      { return industryCode; }
    public void setIndustryCode(String v)                { this.industryCode = v; }

    public String getIndustryDisplayName()               { return industryDisplayName; }
    public void setIndustryDisplayName(String v)         { this.industryDisplayName = v; }

    public double getAgingRiskMultiplier()               { return agingRiskMultiplier; }
    public void setAgingRiskMultiplier(double v)         { this.agingRiskMultiplier = v; }

    public int getMaxShelfLifeDays()                     { return maxShelfLifeDays; }
    public void setMaxShelfLifeDays(int v)               { this.maxShelfLifeDays = v; }

    public double getAgingScore()                        { return agingScore; }
    public void setAgingScore(double v)                  { this.agingScore = v; }

    public String getAgingBucket()                       { return agingBucket; }
    public void setAgingBucket(String v)                 { this.agingBucket = v; }

    public List<String> getLiquidationStrategies()       { return liquidationStrategies; }
    public void setLiquidationStrategies(List<String> v) { this.liquidationStrategies = v; }

    public List<String> getRiskFactors()                 { return riskFactors; }
    public void setRiskFactors(List<String> v)           { this.riskFactors = v; }
}
