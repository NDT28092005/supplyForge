package com.supplyforge.ai.domain.industry;

import java.util.List;

/**
 * IndustryPlaybook — Knowledge Base cho từng ngành nghề.
 *
 * Thiết kế: Enum với embedded strategy object.
 * → Mỗi ngành là một "Playbook" độc lập, dễ thêm ngành mới (Pharma, Beauty...).
 * → Open/Closed Principle: Mở để thêm ngành, đóng để sửa logic cốt lõi.
 */
public enum IndustryPlaybook {

    /**
     * Thời trang nhanh — Lão hóa rất nhanh do yếu tố mùa vụ và trend.
     */
    FASHION_FAST(
        "Thời trang nhanh",
        1.8,
        60,
        List.of(
            "Cross-season Bundle",
            "Flash Sale Size-break",
            "KOL Clearance"
        ),
        List.of(
            "Lỗi thời theo mùa",
            "Rủi ro tồn lẻ size",
            "Nhạy cảm trend"
        )
    ),

    /**
     * Điện tử & Công nghệ — Lão hóa ở mức trung bình, rủi ro chính là model mới ra mắt.
     */
    ELECTRONICS_TECH(
        "Điện tử & Công nghệ",
        1.2,
        180,
        List.of(
            "B2B Wholesale",
            "Refurbishment / Spare Parts",
            "Trade-in Upgrade Program"
        ),
        List.of(
            "Lỗi thời công nghệ",
            "Ra mắt model mới làm giảm giá trị",
            "Rủi ro tương thích phần mềm"
        )
    ),

    /**
     * Thực phẩm & FMCG — Nguy hiểm nhất, cận date = hàng thành rác.
     */
    FMCG_FOOD(
        "Thực phẩm & FMCG",
        2.5,
        30,
        List.of(
            "Dynamic Markdown theo ngày",
            "Donation (Từ thiện) — LIFO Tax Shield",
            "Flash Deal cuối ngày"
        ),
        List.of(
            "Rủi ro hết hạn sử dụng",
            "Quy định an toàn thực phẩm",
            "Chi phí tiêu hủy nếu quá date"
        )
    ),

    /**
     * Nội thất & Gia dụng — Lão hóa chậm, nhưng chi phí lưu kho cao do cồng kềnh.
     */
    HOME_LIVING(
        "Nội thất & Gia dụng",
        1.0,
        120,
        List.of(
            "Showroom Display Deal",
            "B2B Contract Supply",
            "Bundle theo phòng (Room Package)"
        ),
        List.of(
            "Chi phí lưu kho cao (cồng kềnh)",
            "Xu hướng thiết kế nội thất thay đổi",
            "Rủi ro hư hại vật lý khi di chuyển"
        )
    ),

    /**
     * Mặc định — Áp dụng khi không nhận diện được ngành cụ thể.
     */
    GENERAL(
        "Tổng hợp",
        1.0,
        90,
        List.of(
            "Progressive Discount",
            "Bundle Cross-sell",
            "B2B Wholesale"
        ),
        List.of(
            "Rủi ro tồn kho tổng hợp"
        )
    );

    // ─── Fields ───────────────────────────────────────────────────────────────
    private final String displayName;

    /**
     * Hệ số lão hóa rủi ro.
     * Aging Score = daysInInventory × agingRiskMultiplier.
     * VD: 45 ngày Fashion = 81 điểm (nguy hiểm). 45 ngày Điện tử = 54 điểm (bình thường).
     */
    private final double agingRiskMultiplier;

    /** Số ngày tối đa trước khi SKU bị coi là DEAD_STOCK theo ngành. */
    private final int maxShelfLifeDays;

    /** Danh sách chiến lược thanh lý theo ngành. */
    private final List<String> liquidationStrategies;

    /** Danh sách rủi ro đặc thù của ngành (hiển thị trên UI Trust Theater). */
    private final List<String> riskFactors;

    // ─── Constructor ──────────────────────────────────────────────────────────
    IndustryPlaybook(
            String displayName,
            double agingRiskMultiplier,
            int maxShelfLifeDays,
            List<String> liquidationStrategies,
            List<String> riskFactors
    ) {
        this.displayName = displayName;
        this.agingRiskMultiplier = agingRiskMultiplier;
        this.maxShelfLifeDays = maxShelfLifeDays;
        this.liquidationStrategies = liquidationStrategies;
        this.riskFactors = riskFactors;
    }

    // ─── Core Domain Logic ────────────────────────────────────────────────────

    /**
     * Tính điểm rủi ro lão hóa thực tế của ngành.
     * Điểm này thay thế "số ngày tĩnh" trong phân loại bucket.
     *
     * @param daysInInventory Số ngày sản phẩm đã nằm kho
     * @return Aging Score (điểm rủi ro, đã nhân hệ số ngành)
     */
    public double calculateAgingScore(int daysInInventory) {
        return daysInInventory * agingRiskMultiplier;
    }

    /**
     * Phân loại bucket tồn kho dựa trên Aging Score của ngành.
     * Thay thế logic phân loại tĩnh (30/60/90 ngày cứng) ở InventoryFinancialEngineService.
     *
     * @param daysInInventory Số ngày sản phẩm đã nằm kho
     * @return "HEALTHY" | "WATCHLIST" | "SLOW_MOVING" | "DEAD_STOCK"
     */
    public String classifyAgingBucket(int daysInInventory) {
        // Dùng maxShelfLifeDays của ngành làm gốc để scale thresholds
        double shelfLife = maxShelfLifeDays;
        int agingScore = (int) calculateAgingScore(daysInInventory);

        // Thresholds tỉ lệ với maxShelfLifeDays × multiplier
        int deadThreshold     = (int)(shelfLife * agingRiskMultiplier);
        int slowThreshold     = (int)(shelfLife * agingRiskMultiplier * 0.67);
        int watchlistThreshold = (int)(shelfLife * agingRiskMultiplier * 0.33);

        if (agingScore >= deadThreshold)      return "DEAD_STOCK";
        if (agingScore >= slowThreshold)      return "SLOW_MOVING";
        if (agingScore >= watchlistThreshold) return "WATCHLIST";
        return "HEALTHY";
    }

    // ─── Getters ──────────────────────────────────────────────────────────────
    public String getDisplayName()                 { return displayName; }
    public double getAgingRiskMultiplier()         { return agingRiskMultiplier; }
    public int getMaxShelfLifeDays()               { return maxShelfLifeDays; }
    public List<String> getLiquidationStrategies() { return liquidationStrategies; }
    public List<String> getRiskFactors()           { return riskFactors; }
}
