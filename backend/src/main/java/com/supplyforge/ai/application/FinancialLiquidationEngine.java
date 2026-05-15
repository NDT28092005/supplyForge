package com.supplyforge.ai.application;

import com.supplyforge.ai.api.dto.LiquidationROIResult;
import com.supplyforge.ai.api.dto.SkuAgingDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * FinancialLiquidationEngine — CFO-grade financial analysis engine.
 *
 * Thực hiện:
 *   A. Ma trận Quyết định 2D (Value × Age)
 *   B. Tính toán FIFO/LIFO Write-off Impact
 *   C. ROI & Break-even Engine
 */
@Service
public class FinancialLiquidationEngine {

    // ─── Constants từ application.yml ─────────────────────────────────────────
    @Value("${supplyforge.finance.storage-cost-yearly-rate:0.25}")
    private double storageCostYearlyRate;

    @Value("${supplyforge.finance.depreciation-cost-yearly-rate:0.10}")
    private double depreciationCostYearlyRate;

    @Value("${supplyforge.finance.tax-rate:0.20}")
    private double corporateTaxRate; // Thuế TNDN mặc định 20%

    @Value("${supplyforge.finance.marketing-cost-rate:0.08}")
    private double marketingCostRate; // Chi phí marketing ~ 8% doanh thu

    @Value("${supplyforge.finance.high-value-threshold:10000000}")
    private double highValueThreshold; // Ngưỡng "hàng giá trị cao" (10 triệu VND / lô)

    @Value("${supplyforge.finance.mid-value-threshold:1000000}")
    private double midValueThreshold; // Ngưỡng "hàng giá trị trung bình" (1 triệu VND / lô)

    @Value("${supplyforge.aging.slow-moving-max-days:90}")
    private int slowMovingMaxDays;

    /**
     * A. Ma trận Quyết định 2D: Value × Age
     * Phân loại SKU vào 1 trong 3 góc phần tư chiến lược.
     */
    public String classifyInventoryMatrix(double totalValue, int daysInInventory) {
        boolean isOld = daysInInventory > slowMovingMaxDays;
        boolean isHighValue = totalValue >= highValueThreshold;
        boolean isMidValue = totalValue >= midValueThreshold && totalValue < highValueThreshold;

        if (!isOld && isHighValue) return "HIGH_VALUE_NEW";     // Giá trị cao, Mới → Bundle, Kênh ngách
        if (!isOld && isMidValue) return "MID_VALUE_NEW";       // Giá trị TB, Mới → Bình thường
        if (!isOld)               return "LOW_VALUE_NEW";       // Giá trị thấp, Mới → Bình thường
        if (isHighValue)          return "HIGH_VALUE_OLD";      // Giá trị cao, Cũ → Giảm giá lũy tiến, Bán sỉ
        if (isMidValue)           return "MID_VALUE_OLD";       // Giá trị TB, Cũ → Flash sale, Wholesale
        return                           "LOW_VALUE_OLD";       // Giá trị thấp, Cũ → Thanh lý đáy, Quyên góp
    }

    /**
     * B. Write-off Strategy theo phương pháp kế toán FIFO/LIFO
     */
    public String calculateWriteOffImpact(String accountingMethod, double costBasis) {
        double taxSaving = costBasis * corporateTaxRate;
        if ("LIFO".equalsIgnoreCase(accountingMethod)) {
            return String.format(
                "LIFO: Ghi nhận hàng mới nhất (giá vốn cao nhất). Tối đa hóa khấu trừ thuế. " +
                "Lá chắn thuế ước tính: %.0f VND (%.0f%% thuế TNDN × giá vốn). " +
                "Phù hợp khi cần tối ưu nghĩa vụ thuế cuối năm.",
                taxSaving, corporateTaxRate * 100
            );
        }
        // Default FIFO
        return String.format(
            "FIFO: Ghi nhận hàng nhập trước (giá vốn thấp hơn). Bảo toàn dòng tiền hiện tại. " +
            "Lá chắn thuế ước tính: %.0f VND (tác động giảm thuế thấp hơn LIFO). " +
            "Phù hợp khi ưu tiên tính nhất quán báo cáo tài chính.",
            taxSaving, corporateTaxRate * 100
        );
    }

    /**
     * C. ROI & Break-even Engine
     * Tính toán đầy đủ các chỉ số tài chính cho 1 SKU.
     *
     * @param sku           Dữ liệu SKU từ Aging Engine
     * @param discountPct   Mức giảm giá dự kiến áp dụng (0.0 → 1.0)
     * @param accountingMethod FIFO hoặc LIFO
     */
    public LiquidationROIResult calculateLiquidationROI(
            SkuAgingDTO sku,
            double discountPct,
            String accountingMethod
    ) {
        LiquidationROIResult result = new LiquidationROIResult();
        result.setSkuId(sku.getId());
        result.setSkuName(sku.getName());
        result.setQuantity(sku.getQuantity());

        // ── 1. Chi phí chìm (Sunk Cost) ──────────────────────────────────────
        double originalCostBasis = sku.getCostPrice() * sku.getQuantity();

        // Phí lưu kho lũy kế = giá vốn × tỷ lệ/năm × (số tháng kẹt kho)
        double monthsInInventory = Math.max(1.0, sku.getDaysInInventory() / 30.0);
        double monthlyStorageRate = storageCostYearlyRate / 12.0;
        double accumulatedStorageCost = originalCostBasis * monthlyStorageRate * monthsInInventory;

        double totalSunkCost = originalCostBasis + accumulatedStorageCost;

        // ── 2. Doanh thu & Chi phí xả hàng ───────────────────────────────────
        double salePrice = sku.getSellingPrice() * (1.0 - discountPct);
        // Edge-case: giá bán sau giảm không thể âm
        salePrice = Math.max(0, salePrice);
        double expectedRevenue = salePrice * sku.getQuantity();

        // Chi phí marketing + vận chuyển ước tính
        double marketingCost = expectedRevenue * marketingCostRate;
        double netRevenue = expectedRevenue - marketingCost;

        // ── 3. Tỷ lệ thu hồi ─────────────────────────────────────────────────
        // Gross Recovery Rate = Doanh thu / Giá vốn ban đầu
        double grossRecoveryRate = (originalCostBasis > 0)
                ? (expectedRevenue / originalCostBasis) * 100.0
                : 0.0;

        // Net Recovery Rate = (Doanh thu ròng - Chi phí lưu kho lũy kế) / Giá vốn ban đầu
        double netRecoveryRate = (originalCostBasis > 0)
                ? ((netRevenue - accumulatedStorageCost) / originalCostBasis) * 100.0
                : 0.0;

        // ── 4. Break-even Discount (Ngưỡng giảm giá tối đa) ──────────────────
        // Tại điểm hòa vốn: Net Revenue = Total Sunk Cost
        // salePrice_be × qty × (1 - marketingRate) = totalSunkCost
        // salePrice_be = totalSunkCost / (qty × (1 - marketingRate))
        double breakEvenSalePrice = (sku.getQuantity() > 0 && (1.0 - marketingCostRate) > 0)
                ? totalSunkCost / (sku.getQuantity() * (1.0 - marketingCostRate))
                : sku.getSellingPrice();

        double breakEvenDiscountPct = (sku.getSellingPrice() > 0)
                ? ((sku.getSellingPrice() - breakEvenSalePrice) / sku.getSellingPrice()) * 100.0
                : 0.0;
        // Clamp: không âm, không vượt quá 100%
        breakEvenDiscountPct = Math.min(100.0, Math.max(0.0, breakEvenDiscountPct));

        // ── 5. Ma trận Quyết định & Khuyến nghị ──────────────────────────────
        String matrixQuadrant = classifyInventoryMatrix(originalCostBasis, sku.getDaysInInventory());

        // ── 6. Lá chắn thuế ──────────────────────────────────────────────────
        // Nếu âm dòng tiền: khoản lỗ ghi nhận được = tax shield
        double writeOffLoss = Math.max(0, totalSunkCost - expectedRevenue);
        double taxShieldValue = writeOffLoss * corporateTaxRate;

        // Tax Shield được đề xuất khi Net Recovery âm VÀ là hàng giá trị thấp/cũ
        boolean isTaxShieldRecommended = netRecoveryRate < 0 ||
                "LOW_VALUE_OLD".equals(matrixQuadrant);

        // ── 7. AI Recommendation text ─────────────────────────────────────────
        String aiRecommendation = buildAiRecommendation(matrixQuadrant, breakEvenDiscountPct,
                netRecoveryRate, accountingMethod);

        // ── Assemble result ───────────────────────────────────────────────────
        result.setOriginalCostBasis(originalCostBasis);
        result.setAccumulatedStorageCost(accumulatedStorageCost);
        result.setTotalSunkCost(totalSunkCost);
        result.setExpectedRevenue(expectedRevenue);
        result.setMarketingAndShippingCost(marketingCost);
        result.setGrossRecoveryRate(round2(grossRecoveryRate));
        result.setNetRecoveryRate(round2(netRecoveryRate));
        result.setBreakEvenDiscountPct(round2(breakEvenDiscountPct));
        result.setTaxShieldValueVnd(taxShieldValue);
        result.setMatrixQuadrant(matrixQuadrant);
        result.setAccountingMethod(accountingMethod.toUpperCase());
        result.setWriteOffStrategy(calculateWriteOffImpact(accountingMethod, originalCostBasis));
        result.setAiRecommendation(aiRecommendation);
        result.setTaxShieldRecommended(isTaxShieldRecommended);

        return result;
    }

    /**
     * Tính toán ROI cho top dead SKU trong danh sách (tiện ích cho Controller)
     */
    public LiquidationROIResult calculateForTopDeadSku(
            List<SkuAgingDTO> deadSkus,
            double discountPct,
            String accountingMethod
    ) {
        if (deadSkus == null || deadSkus.isEmpty()) return null;

        // Chọn SKU có giá trị tổng cao nhất để phân tích
        SkuAgingDTO topSku = deadSkus.stream()
                .max(Comparator.comparingDouble(SkuAgingDTO::getTotalValue))
                .orElse(deadSkus.get(0));

        return calculateLiquidationROI(topSku, discountPct, accountingMethod);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────
    private String buildAiRecommendation(String quadrant, double breakEvenPct,
                                          double netRecovery, String method) {
        return switch (quadrant) {
            case "HIGH_VALUE_NEW" ->
                "Hàng giá trị cao, vòng đời còn tốt. Ưu tiên Bundle cao cấp & phân phối kênh ngách. " +
                "Tránh Flash Sale — sẽ phá vỡ định vị thương hiệu.";
            case "HIGH_VALUE_OLD" ->
                String.format("Hàng giá trị cao nhưng đã lâu ngày. Áp dụng giảm giá lũy tiến tối đa %.1f%% " +
                "(ngưỡng hòa vốn). Hoặc tiếp cận kênh B2B Wholesale để bảo toàn margin.", breakEvenPct);
            case "MID_VALUE_OLD" ->
                String.format("Thực hiện Flash Sale với mức giảm không quá %.1f%% để giữ Net Recovery dương. " +
                "Kết hợp Bundle với sản phẩm hot.", breakEvenPct);
            case "LOW_VALUE_OLD" ->
                String.format("Net Recovery ròng: %.1f%%. Xả hàng không còn hiệu quả tài chính tối ưu. " +
                "AI đề xuất: Quyên góp từ thiện hoặc Tiêu hủy để kích hoạt Lá chắn thuế (%s).",
                netRecovery, method.toUpperCase());
            default ->
                "Tiếp tục theo dõi. Vòng đời tồn kho đang trong ngưỡng an toàn.";
        };
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
