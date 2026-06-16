package com.supplyforge.ai.application;

import com.supplyforge.ai.api.dto.PortfolioCommandDTO;
import com.supplyforge.ai.api.dto.RecoveryClusterDTO;
import com.supplyforge.ai.api.dto.RecoveryClusterDTO.SkuSummaryDTO;
import com.supplyforge.ai.domain.industry.IndustryPlaybook;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PortfolioOrchestrationService — Điều phối tồn kho cấp độ Danh mục.
 *
 * Pipeline:
 *  1. Lấy toàn bộ sản phẩm tồn kho (chỉ SLOW_MOVING + DEAD_STOCK).
 *  2. Tính {@code criticalRiskScore} (0–100) cho từng SKU.
 *  3. Gom nhóm SKU (RecoveryClusterEngine) theo Industry + urgency tier.
 *  4. Capacity Limiter: chỉ trả TOP 3 clusters có averageRiskScore cao nhất.
 *
 * Thiết kế: Single Responsibility — không render UI, không tính tax/ROI chi tiết.
 * Đó là nhiệm vụ của {@link FinancialLiquidationEngine}.
 */
@Service
public class PortfolioOrchestrationService {

    // ─── Constants ──────────────────────────────────────────────────────────
    /** Số cluster tối đa trả về (Capacity Limiter). */
    private static final int MAX_CLUSTERS = 3;

    /** Mức giảm giá tương ứng mỗi giai đoạn — dùng để ước tính recoverable value. */
    private static final double PHASE_DISCOUNT_DEAD   = 0.70; // −70% thanh lý
    private static final double PHASE_DISCOUNT_SLOW   = 0.35; // −35% xả cắt máu
    private static final double PHASE_DISCOUNT_WATCH  = 0.15; // −15% khuyến mãi mồi

    /** Weights cho DeadStockPriorityScore. */
    private static final double W_FROZEN_VALUE    = 0.30;
    private static final double W_AGING_DAYS      = 0.25;
    private static final double W_SELL_THRU       = 0.20;
    private static final double W_INDUSTRY_RISK   = 0.25;

    /** Chuẩn hóa: số ngày tối đa để scale aging score về [0,1]. */
    private static final double MAX_DAYS_SCALE    = 365.0;

    /** Chuẩn hóa: giá trị vốn đóng băng cao nhất để scale về [0,1] (500 triệu ₫). */
    private static final double MAX_VALUE_SCALE   = 500_000_000.0;

    @PersistenceContext
    private EntityManager entityManager;

    private final IndustryDetectionService industryDetectionService;

    @Value("${supplyforge.portfolio.max-clusters:3}")
    private int maxClusters = MAX_CLUSTERS;

    public PortfolioOrchestrationService(IndustryDetectionService industryDetectionService) {
        this.industryDetectionService = industryDetectionService;
    }

    // ─── Public API ─────────────────────────────────────────────────────────

    /**
     * Entry point: Tính toán và trả về Portfolio Command Plan cho userId.
     */
    @Transactional(readOnly = true)
    public PortfolioCommandDTO buildCommandPlan(String userId) {
        List<ScoredSku> scoredSkus = fetchAndScore(userId);
        if (scoredSkus.isEmpty()) {
            return emptyPlan();
        }

        // Gom nhóm
        Map<String, List<ScoredSku>> grouped = clusterByIndustryAndTier(scoredSkus);

        // Build cluster DTOs & xếp hạng
        List<RecoveryClusterDTO> clusters = grouped.entrySet().stream()
            .map(e -> buildCluster(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingDouble(RecoveryClusterDTO::getAverageRiskScore).reversed())
            .limit(maxClusters)
            .collect(Collectors.toList());

        return buildCommandDTO(clusters, scoredSkus);
    }

    // ─── Step 1: Fetch + Score ───────────────────────────────────────────────

    /**
     * Query tất cả sản phẩm còn tồn kho của userId,
     * tính daysInInventory, phát hiện industry, chấm điểm rủi ro.
     * Chỉ giữ SKU có bucket = SLOW_MOVING hoặc DEAD_STOCK (bỏ qua HEALTHY/WATCHLIST).
     */
    private List<ScoredSku> fetchAndScore(String userId) {
        String sql = """
            SELECT p._id, p.product_name, p.item_sku, p.stock, p.cost, p.price,
                   COALESCE(MAX(o.created_at), p.created_at) as last_date
            FROM products p
            LEFT JOIN order_items oi ON p.item_id = oi.item_id OR p.item_sku = oi.sku
            LEFT JOIN orders o ON oi.order_id = o._id
            WHERE p.user_id = :userId
              AND p.stock > 0
              AND (p.active_on_web IS NULL OR p.active_on_web = true)
            GROUP BY p._id, p.product_name, p.item_sku, p.stock, p.cost, p.price, p.created_at
            ORDER BY (p.stock * COALESCE(p.cost, 0)) DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        LocalDateTime now = LocalDateTime.now();

        // Tìm maxTotalValue trong toàn bộ danh sách để chuẩn hóa (normalize)
        // Chạy 2 pass nhưng vẫn O(n) — chấp nhận được với inventory size thực tế.
        List<RawRow> rawRows = new ArrayList<>();
        double globalMaxValue = 1.0;

        for (Object[] row : rows) {
            RawRow r = parseRow(row, now);
            rawRows.add(r);
            if (r.totalValue > globalMaxValue) globalMaxValue = r.totalValue;
        }

        List<ScoredSku> result = new ArrayList<>();
        for (RawRow r : rawRows) {
            IndustryPlaybook playbook = industryDetectionService.detect(r.name);
            String bucket = playbook.classifyAgingBucket(r.daysInInventory);

            // Capacity Limiter tầng 1: loại bỏ HEALTHY và WATCHLIST sớm
            if ("HEALTHY".equals(bucket) || "WATCHLIST".equals(bucket)) continue;

            double score = computeRiskScore(r, playbook, bucket, globalMaxValue);
            result.add(new ScoredSku(r, playbook, bucket, score));
        }

        return result;
    }

    // ─── Step 2: DeadStockPriorityScore ─────────────────────────────────────

    /**
     * Công thức chấm điểm rủi ro tổng hợp (0–100).
     *
     * score = 100 × (
     *   W_FROZEN_VALUE  × normalizedFrozenValue   +
     *   W_AGING_DAYS    × normalizedAgingDays      +
     *   W_SELL_THRU     × sellThroughDeclineFactor +
     *   W_INDUSTRY_RISK × industryRiskFactor
     * )
     *
     * sellThroughDecline: Heuristic — bucket DEAD_STOCK = 1.0, SLOW_MOVING = 0.6.
     * industryRiskFactor: agingRiskMultiplier của playbook, scale [0.5–2.5] → [0,1].
     */
    private double computeRiskScore(RawRow r, IndustryPlaybook playbook,
                                    String bucket, double globalMaxValue) {
        double normalizedValue = Math.min(1.0, r.totalValue / globalMaxValue);
        double normalizedDays  = Math.min(1.0, r.daysInInventory / MAX_DAYS_SCALE);

        // Sell-through decline: không có order history chi tiết → dùng bucket làm proxy
        double sellThroughDecline = "DEAD_STOCK".equals(bucket) ? 1.0 : 0.6;

        // Industry risk: multiplier range [1.0, 2.5] → normalize về [0, 1]
        // Thấp nhất GENERAL/HOME=1.0 → 0.0; Cao nhất FMCG=2.5 → 1.0
        double multiplier = playbook.getAgingRiskMultiplier();
        double industryRiskFactor = Math.min(1.0, (multiplier - 1.0) / 1.5);

        double rawScore = W_FROZEN_VALUE  * normalizedValue
                        + W_AGING_DAYS    * normalizedDays
                        + W_SELL_THRU     * sellThroughDecline
                        + W_INDUSTRY_RISK * industryRiskFactor;

        return Math.round(rawScore * 100.0 * 10.0) / 10.0; // 1 decimal, max=100
    }

    // ─── Step 3: RecoveryClusterEngine ──────────────────────────────────────

    /**
     * Gom nhóm SKU theo khóa = industryCode + urgencyTier (CRITICAL / HIGH / MEDIUM).
     * Ví dụ: "FASHION_FAST_CRITICAL", "ELECTRONICS_TECH_HIGH".
     *
     * Logic: Đơn giản nhưng hiệu quả — cùng ngành + cùng mức độ cấp bách
     * → cùng chiến lược xả hàng → có thể batch execute.
     */
    private Map<String, List<ScoredSku>> clusterByIndustryAndTier(List<ScoredSku> skus) {
        Map<String, List<ScoredSku>> groups = new LinkedHashMap<>();
        for (ScoredSku sku : skus) {
            String tier = urgencyTier(sku.riskScore);
            String key  = sku.playbook.name() + "_" + tier;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(sku);
        }
        return groups;
    }

    /** Map điểm rủi ro sang mức cấp bách. */
    private String urgencyTier(double score) {
        if (score >= 70) return "CRITICAL";
        if (score >= 45) return "HIGH";
        return "MEDIUM";
    }

    // ─── Step 4: Build Cluster DTO ───────────────────────────────────────────

    private RecoveryClusterDTO buildCluster(String clusterKey, List<ScoredSku> skus) {
        RecoveryClusterDTO dto = new RecoveryClusterDTO();
        dto.setClusterId(clusterKey);

        // Parse key → industryCode + tier
        // Format: "ELECTRONICS_TECH_CRITICAL"
        String[] parts = clusterKey.split("_(?=CRITICAL|HIGH|MEDIUM)");
        String industryCode = parts[0];
        String tier = parts.length > 1 ? parts[1] : "MEDIUM";

        dto.setIndustryCode(industryCode);
        dto.setUrgencyLevel(tier);
        dto.setClusterName(buildClusterName(industryCode, tier));
        dto.setRationale(buildRationale(industryCode, tier, skus.size()));
        dto.setSkuCount(skus.size());

        double totalFrozen = skus.stream().mapToDouble(s -> s.row.totalValue).sum();
        dto.setTotalFrozenValue(totalFrozen);

        // Ước tính thu hồi dựa theo tier — dùng discount rate tương ứng
        double discountRate = "CRITICAL".equals(tier) ? PHASE_DISCOUNT_DEAD
                            : "HIGH".equals(tier)     ? PHASE_DISCOUNT_SLOW
                            :                           PHASE_DISCOUNT_WATCH;
        double recoverable = totalFrozen * (1.0 - discountRate);
        dto.setTotalPotentialRecovery(recoverable);

        // averageRiskScore
        double avgRisk = skus.stream().mapToDouble(s -> s.riskScore).average().orElse(0);
        dto.setAverageRiskScore(Math.round(avgRisk * 10.0) / 10.0);

        dto.setRecommendedBatchAction(buildBatchAction(industryCode, tier));
        dto.setEstimatedClearDays("CRITICAL".equals(tier) ? 7 : "HIGH".equals(tier) ? 14 : 30);

        // Top 3 SKU sample
        List<SkuSummaryDTO> samples = skus.stream()
            .sorted(Comparator.comparingDouble((ScoredSku s) -> s.riskScore).reversed())
            .limit(3)
            .map(s -> new SkuSummaryDTO(s.row.name, s.row.daysInInventory, s.row.totalValue, s.riskScore))
            .collect(Collectors.toList());
        dto.setTopSkuSamples(samples);

        return dto;
    }

    // ─── Step 5: Build Command DTO (Portfolio Summary) ───────────────────────

    private PortfolioCommandDTO buildCommandDTO(List<RecoveryClusterDTO> clusters,
                                                List<ScoredSku> allScoredSkus) {
        PortfolioCommandDTO cmd = new PortfolioCommandDTO();
        cmd.setPriorityClusters(clusters);

        double totalRecovery = clusters.stream()
            .mapToDouble(RecoveryClusterDTO::getTotalPotentialRecovery).sum();
        cmd.setTotalExpectedRecovery(totalRecovery);

        int totalSkus = clusters.stream().mapToInt(RecoveryClusterDTO::getSkuCount).sum();
        cmd.setTotalSkuCount(totalSkus);

        // Ước tính % kho giải phóng:
        // Proxy = totalFrozenValue trong 3 clusters / tổng frozen toàn danh mục × 100
        double portfolioFrozen = allScoredSkus.stream().mapToDouble(s -> s.row.totalValue).sum();
        double clusterFrozen   = clusters.stream().mapToDouble(RecoveryClusterDTO::getTotalFrozenValue).sum();
        double spacePct = portfolioFrozen > 0 ? (clusterFrozen / portfolioFrozen) * 100.0 : 0;
        cmd.setEstimatedWarehouseSpaceFreedPct(Math.round(spacePct * 10.0) / 10.0);

        // Horizon = max estimatedClearDays trong 3 cluster
        int horizon = clusters.stream().mapToInt(RecoveryClusterDTO::getEstimatedClearDays).max().orElse(14);
        cmd.setPlanHorizonDays(horizon);

        return cmd;
    }

    // ─── Naming Helpers ──────────────────────────────────────────────────────

    private String buildClusterName(String industryCode, String tier) {
        String industryLabel = switch (industryCode) {
            case "FASHION_FAST"      -> "Thời trang";
            case "ELECTRONICS_TECH" -> "Điện tử & Công nghệ";
            case "FMCG_FOOD"        -> "Thực phẩm & FMCG";
            case "HOME_LIVING"      -> "Nội thất & Gia dụng";
            default                  -> "Tổng hợp";
        };
        String tierLabel = switch (tier) {
            case "CRITICAL" -> "— Thanh lý khẩn cấp";
            case "HIGH"     -> "— Xả hàng cắt máu";
            default          -> "— Khuyến mãi kích cầu";
        };
        return industryLabel + tierLabel;
    }

    private String buildRationale(String industryCode, String tier, int skuCount) {
        String riskReason = switch (industryCode) {
            case "FASHION_FAST"     -> "đang lỗi mùa vụ";
            case "ELECTRONICS_TECH"-> "đối diện rủi ro model mới";
            case "FMCG_FOOD"       -> "cận hạn sử dụng";
            case "HOME_LIVING"     -> "chi phí lưu kho cao";
            default                -> "tồn kho bất thường";
        };
        return String.format("%d SKU %s — Nhóm vào chiến dịch duy nhất để thực thi hàng loạt.", skuCount, riskReason);
    }

    private String buildBatchAction(String industryCode, String tier) {
        if ("CRITICAL".equals(tier)) {
            return switch (industryCode) {
                case "FASHION_FAST"     -> "Flash Sale TikTok −70% + KOL Seeding";
                case "ELECTRONICS_TECH"-> "B2B Wholesale Bulk Deal −50%";
                case "FMCG_FOOD"       -> "Clearance Flash Deal Lazada −70%";
                case "HOME_LIVING"     -> "Showroom Display Clearance −60%";
                default                -> "Batch Clearance Sale −70%";
            };
        } else if ("HIGH".equals(tier)) {
            return switch (industryCode) {
                case "FASHION_FAST"     -> "Flash Sale Shopee −35% + Cross-season Bundle";
                case "ELECTRONICS_TECH"-> "Trade-in Upgrade Campaign −35%";
                case "FMCG_FOOD"       -> "Multi-buy Deal −30% FMCG Bundle";
                case "HOME_LIVING"     -> "Room Package Bundle −35%";
                default                -> "Promotional Campaign −35%";
            };
        } else {
            return switch (industryCode) {
                case "FASHION_FAST"     -> "Seasonal Promotion −15%";
                case "ELECTRONICS_TECH"-> "Loyalty Member Deal −15%";
                case "FMCG_FOOD"       -> "Buy-more-save-more −15%";
                case "HOME_LIVING"     -> "Home Refresh Promo −15%";
                default                -> "Early Bird Discount −15%";
            };
        }
    }

    // ─── Utilities ───────────────────────────────────────────────────────────

    private RawRow parseRow(Object[] row, LocalDateTime now) {
        String id    = (String) row[0];
        String name  = row[1] != null ? (String) row[1] : "";
        String sku   = row[2] != null ? (String) row[2] : "";
        int stock    = row[3] != null ? ((Number) row[3]).intValue() : 0;
        double cost  = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
        double price = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;

        LocalDateTime lastDate = now;
        if (row[6] != null) {
            try {
                lastDate = (row[6] instanceof Timestamp t)
                    ? t.toLocalDateTime()
                    : Timestamp.valueOf(row[6].toString()).toLocalDateTime();
            } catch (Exception ignored) {}
        }

        int days = (int) Math.max(0, ChronoUnit.DAYS.between(lastDate, now));
        double totalValue = cost * stock;

        return new RawRow(id, name, sku, stock, cost, price, days, totalValue);
    }

    private PortfolioCommandDTO emptyPlan() {
        PortfolioCommandDTO dto = new PortfolioCommandDTO();
        dto.setPriorityClusters(List.of());
        dto.setTotalExpectedRecovery(0);
        dto.setTotalSkuCount(0);
        dto.setEstimatedWarehouseSpaceFreedPct(0);
        dto.setPlanHorizonDays(0);
        return dto;
    }

    // ─── Internal Records ────────────────────────────────────────────────────

    /** Raw row từ DB trước khi score. */
    private record RawRow(String id, String name, String sku, int stock,
                          double cost, double price, int daysInInventory, double totalValue) {}

    /** SKU đã được chấm điểm và phát hiện ngành. */
    private record ScoredSku(RawRow row, IndustryPlaybook playbook, String bucket, double riskScore) {}
}
