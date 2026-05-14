package com.supplyforge.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplyforge.ai.api.dto.DeadStockDashboardResponse;
import com.supplyforge.ai.api.dto.DeadStockDashboardResponse.StaleSkuLine;
import com.supplyforge.ai.domain.entity.InsightAnomaly;
import com.supplyforge.ai.domain.entity.InventoryRecord;
import com.supplyforge.ai.domain.entity.Sku;
import com.supplyforge.ai.domain.entity.Workspace;
import com.supplyforge.ai.domain.model.InsightSeverity;
import com.supplyforge.ai.domain.repository.InsightAnomalyRepository;
import com.supplyforge.ai.domain.repository.InventoryRecordRepository;
import com.supplyforge.ai.domain.repository.SkuRepository;
import com.supplyforge.ai.domain.repository.WorkspaceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeadStockAnalyticsService {

    public static final String INSIGHT_TYPE = "DEAD_STOCK_SUMMARY";
    private static final int STALE_DAYS = 45;
    private static final BigDecimal LOSS_RATE_30D = new BigDecimal("0.045");

    private final SkuRepository skuRepository;
    private final InventoryRecordRepository inventoryRecordRepository;
    private final InsightAnomalyRepository insightAnomalyRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public DeadStockAnalyticsService(
            SkuRepository skuRepository,
            InventoryRecordRepository inventoryRecordRepository,
            InsightAnomalyRepository insightAnomalyRepository,
            WorkspaceRepository workspaceRepository,
            ObjectMapper objectMapper) {
        this.skuRepository = skuRepository;
        this.inventoryRecordRepository = inventoryRecordRepository;
        this.insightAnomalyRepository = insightAnomalyRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DeadStockDashboardResponse getDashboard(Long workspaceId) {
        Instant now = Instant.now();
        List<InsightAnomaly> recent =
                insightAnomalyRepository.findByWorkspaceIdAndInsightType(workspaceId, INSIGHT_TYPE);
        Optional<InsightAnomaly> latest = recent.stream()
                .filter(i -> i.getValidUntil() != null && i.getValidUntil().isAfter(now))
                .max(Comparator.comparing(InsightAnomaly::getComputedAt));
        if (latest.isPresent() && latest.get().getPayload() != null) {
            return fromPayload(latest.get().getPayload());
        }
        DeadStockDashboardResponse computed = compute(workspaceId);
        saveInsightSnapshot(workspaceId, computed);
        return computed;
    }

    @Transactional
    public DeadStockDashboardResponse refresh(Long workspaceId) {
        DeadStockDashboardResponse computed = compute(workspaceId);
        saveInsightSnapshot(workspaceId, computed);
        return computed;
    }

    private void saveInsightSnapshot(Long workspaceId, DeadStockDashboardResponse computed) {
        Workspace ws = workspaceRepository.findById(workspaceId).orElse(null);
        if (ws == null) {
            return;
        }
        InsightAnomaly row = new InsightAnomaly();
        row.setWorkspace(ws);
        row.setInsightType(INSIGHT_TYPE);
        row.setSeverity(InsightSeverity.CRITICAL);
        row.setTitle("Tóm tắt tồn kho chết");
        row.setSummary("Snapshot cho dashboard — TTL 1 giờ");
        row.setPayload(toPayload(computed));
        row.setComputedAt(Instant.now());
        row.setValidUntil(Instant.now().plus(1, ChronoUnit.HOURS));
        insightAnomalyRepository.save(row);
    }

    private DeadStockDashboardResponse compute(Long workspaceId) {
        List<Sku> roots = skuRepository.findByWorkspaceIdAndParentSkuIsNull(workspaceId);
        if (roots.isEmpty()) {
            return new DeadStockDashboardResponse(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0,
                    0,
                    List.of());
        }

        // Dedup: nếu DB có SKU trùng normalizedName (do import lại nhiều lần),
        // chỉ giữ lại bản có ID nhỏ nhất (canonical/gốc nhất) để tránh đếm đôi.
        Map<String, Sku> canonicalByNorm = new HashMap<>();
        for (Sku sku : roots) {
            canonicalByNorm.merge(sku.getNormalizedName(), sku,
                    (a, b) -> a.getId() < b.getId() ? a : b);
        }
        List<Sku> dedupedRoots = new ArrayList<>(canonicalByNorm.values());

        // Thu thập TẤT CẢ InventoryRecord của mọi SKU có cùng normalizedName
        // rồi lấy record mới nhất trong nhóm
        List<Long> allIds = roots.stream().map(Sku::getId).collect(Collectors.toList());
        List<InventoryRecord> all = inventoryRecordRepository.findBySkuIdIn(allIds);

        // Map: normalizedName → InventoryRecord mới nhất
        Map<String, InventoryRecord> latestByNorm = new HashMap<>();
        for (InventoryRecord ir : all) {
            String norm = ir.getSku().getNormalizedName();
            latestByNorm.merge(norm, ir,
                    (a, b) -> a.getRecordDate().isAfter(b.getRecordDate()) ? a : b);
        }

        LocalDate cutoff = LocalDate.now().minusDays(STALE_DAYS);
        BigDecimal totalInv = BigDecimal.ZERO;
        BigDecimal dead = BigDecimal.ZERO;
        int staleCount = 0;
        List<StaleSkuLine> staleLines = new ArrayList<>();

        for (Sku sku : dedupedRoots) {
            InventoryRecord ir = latestByNorm.get(sku.getNormalizedName());
            if (ir == null || ir.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal unitValue = unitValue(ir);
            BigDecimal lineVal = ir.getQuantity().multiply(unitValue).setScale(0, RoundingMode.HALF_UP);
            totalInv = totalInv.add(lineVal);
            boolean stale = ir.getRecordDate().isBefore(cutoff);
            if (stale) {
                dead = dead.add(lineVal);
                staleCount++;
                staleLines.add(new StaleSkuLine(
                        sku.getId(),
                        sku.getOriginalName(),
                        lineVal.longValue(),
                        ir.getRecordDate().toString()));
            }
        }

        staleLines.sort(Comparator.comparing(StaleSkuLine::frozenValueVnd).reversed());
        List<StaleSkuLine> top = staleLines.stream().limit(8).collect(Collectors.toList());
        BigDecimal loss = dead.multiply(LOSS_RATE_30D).setScale(0, RoundingMode.HALF_UP);

        return new DeadStockDashboardResponse(
                dead, loss, totalInv, staleCount, dedupedRoots.size(), top);
    }

    private static BigDecimal unitValue(InventoryRecord ir) {
        if (ir.getCostPrice() != null && ir.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
            return ir.getCostPrice();
        }
        if (ir.getSellingPrice() != null && ir.getSellingPrice().compareTo(BigDecimal.ZERO) > 0) {
            return ir.getSellingPrice().multiply(new BigDecimal("0.7"));
        }
        return BigDecimal.ZERO;
    }

    private JsonNode toPayload(DeadStockDashboardResponse r) {
        return objectMapper.valueToTree(r);
    }

    private DeadStockDashboardResponse fromPayload(JsonNode n) {
        return objectMapper.convertValue(n, DeadStockDashboardResponse.class);
    }
}
