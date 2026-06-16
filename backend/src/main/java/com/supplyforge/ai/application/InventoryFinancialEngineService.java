package com.supplyforge.ai.application;

import com.supplyforge.ai.api.dto.InventoryAgingDTO;
import com.supplyforge.ai.api.dto.SkuAgingDTO;
import com.supplyforge.ai.domain.industry.IndustryContext;
import com.supplyforge.ai.domain.industry.IndustryPlaybook;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryFinancialEngineService {

    @PersistenceContext
    private EntityManager entityManager;

    private final IndustryDetectionService industryDetectionService;

    public InventoryFinancialEngineService(IndustryDetectionService industryDetectionService) {
        this.industryDetectionService = industryDetectionService;
    }

    @Value("${supplyforge.finance.storage-cost-yearly-rate:0.25}")
    private double storageCostYearlyRate;

    @Value("${supplyforge.finance.depreciation-cost-yearly-rate:0.10}")
    private double depreciationCostYearlyRate;

    @Transactional(readOnly = true)
    public InventoryAgingDTO calculateAgingAndHiddenCosts(String userId) {
        InventoryAgingDTO result = new InventoryAgingDTO();
        
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

        List<Object[]> rows = query.getResultList();
        LocalDateTime now = LocalDateTime.now();

        double totalHealthy = 0;
        double totalWatchlist = 0;
        double totalSlowMoving = 0;
        double totalDeadStock = 0;

        for (Object[] row : rows) {
            String id = (String) row[0];
            String name = (String) row[1];
            String sku = (String) row[2];
            int stock = row[3] != null ? ((Number) row[3]).intValue() : 0;
            double cost = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            double price = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
            
            LocalDateTime lastDate = now;
            String lastDateStr = "N/A";
            if (row[6] != null) {
                if (row[6] instanceof Timestamp) {
                    lastDate = ((Timestamp) row[6]).toLocalDateTime();
                } else {
                    try {
                        lastDate = Timestamp.valueOf(row[6].toString()).toLocalDateTime();
                    } catch (Exception e) {
                        // ignore and default to now
                    }
                }
                lastDateStr = lastDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            }

            int daysInInventory = (int) ChronoUnit.DAYS.between(lastDate, now);
            if (daysInInventory < 0) daysInInventory = 0;

            double totalValue = cost * stock;

            // ── Industry-aware bucket classification ──────────────────────────
            IndustryPlaybook playbook = industryDetectionService.detect(name);
            String bucket = playbook.classifyAgingBucket(daysInInventory);
            IndustryContext industryCtx = IndustryContext.from(playbook, daysInInventory);

            switch (bucket) {
                case "HEALTHY":      totalHealthy += totalValue; break;
                case "WATCHLIST":    totalWatchlist += totalValue; break;
                case "SLOW_MOVING": totalSlowMoving += totalValue; break;
                case "DEAD_STOCK":  totalDeadStock += totalValue; break;
            }

            SkuAgingDTO skuDto = new SkuAgingDTO(id, name, sku, stock, cost, price, totalValue, lastDateStr, daysInInventory, bucket, industryCtx);
            
            switch (bucket) {
                case "HEALTHY": result.getHealthySkus().add(skuDto); break;
                case "WATCHLIST": result.getWatchlistSkus().add(skuDto); break;
                case "SLOW_MOVING": result.getSlowMovingSkus().add(skuDto); break;
                case "DEAD_STOCK": result.getDeadStockSkus().add(skuDto); break;
            }
        }

        double totalFrozen = totalSlowMoving + totalDeadStock;
        
        // Calculate Bleeding Rate (monthly)
        double monthlyStorageRate = storageCostYearlyRate / 12.0;
        double monthlyDepreciationRate = depreciationCostYearlyRate / 12.0;
        
        double storageCostMonthly = totalFrozen * monthlyStorageRate;
        double depreciationCostMonthly = totalFrozen * monthlyDepreciationRate;
        double totalBleedingRateMonthly = storageCostMonthly + depreciationCostMonthly;

        result.setTotalHealthyValue(totalHealthy);
        result.setTotalWatchlistValue(totalWatchlist);
        result.setTotalSlowMovingValue(totalSlowMoving);
        result.setTotalDeadStockValue(totalDeadStock);
        result.setTotalFrozenValue(totalFrozen);
        result.setTotalBleedingRateMonthly(totalBleedingRateMonthly);

        return result;
    }
}
