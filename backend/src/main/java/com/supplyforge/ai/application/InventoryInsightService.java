package com.supplyforge.ai.application;

import com.supplyforge.ai.api.dto.DashboardInsightDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryInsightService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public DashboardInsightDTO getDashboardInsights(String userId) {
        DashboardInsightDTO dto = new DashboardInsightDTO();
        
        // 1. Dead Stock Calculation
        String deadStockSql = """
            SELECT p._id, p.product_name, p.item_sku, p.stock, p.cost, 
                   COALESCE(MAX(o.created_at), p.created_at) as last_date
            FROM products p
            LEFT JOIN order_items oi ON p.item_id = oi.item_id OR p.item_sku = oi.sku
            LEFT JOIN orders o ON oi.order_id = o._id AND o.created_at >= :ninetyDaysAgo
            WHERE p.user_id = :userId 
              AND p.stock > 0
              AND (p.active_on_web IS NULL OR p.active_on_web = true)
            GROUP BY p._id, p.product_name, p.item_sku, p.stock, p.cost, p.created_at
            HAVING COALESCE(MAX(o.created_at), p.created_at) < :fortyFiveDaysAgo
            ORDER BY (p.stock * COALESCE(p.cost, 0)) DESC
        """;

        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        LocalDateTime fortyFiveDaysAgo = LocalDateTime.now().minusDays(45);
        Query deadStockQuery = entityManager.createNativeQuery(deadStockSql);
        deadStockQuery.setParameter("userId", userId);
        deadStockQuery.setParameter("ninetyDaysAgo", ninetyDaysAgo);
        deadStockQuery.setParameter("fortyFiveDaysAgo", fortyFiveDaysAgo);

        List<Object[]> results = deadStockQuery.getResultList();
        
        double totalFrozenValue = 0;
        List<DashboardInsightDTO.StaleSkuDTO> staleSkus = new ArrayList<>();

        for (Object[] row : results) {
            String id = (String) row[0];
            String name = (String) row[1];
            String sku = (String) row[2];
            int stock = row[3] != null ? ((Number) row[3]).intValue() : 0;
            double cost = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            
            String lastDateStr = "N/A";
            if (row[5] != null) {
                if (row[5] instanceof Timestamp) {
                    lastDateStr = ((Timestamp) row[5]).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE);
                } else {
                    lastDateStr = row[5].toString().split(" ")[0];
                }
            }

            double frozenValue = cost * stock;
            totalFrozenValue += frozenValue;

            staleSkus.add(new DashboardInsightDTO.StaleSkuDTO(id, name, frozenValue, lastDateStr));
        }

        dto.setStaleSkuCount(staleSkus.size());
        dto.setTotalDeadStockValueVnd(totalFrozenValue);
        dto.setEstimatedLoss30DaysVnd(totalFrozenValue * 0.05); // Giả định mất 5% giá trị/tháng
        dto.setTopStaleSkus(staleSkus.size() > 5 ? staleSkus.subList(0, 5) : staleSkus);
        
        // Total Inventory Value
        String totalInvSql = "SELECT SUM(stock * COALESCE(cost, 0)) FROM products WHERE user_id = :userId";
        Query totalInvQuery = entityManager.createNativeQuery(totalInvSql);
        totalInvQuery.setParameter("userId", userId);
        Object totalInv = totalInvQuery.getSingleResult();
        dto.setTotalInventoryValueVnd(totalInv != null ? ((Number) totalInv).doubleValue() : 0.0);
        
        // Total SKU Count
        String totalSkuSql = "SELECT COUNT(*) FROM products WHERE user_id = :userId";
        Query totalSkuQuery = entityManager.createNativeQuery(totalSkuSql);
        totalSkuQuery.setParameter("userId", userId);
        dto.setTotalRootSkuCount(((Number) totalSkuQuery.getSingleResult()).intValue());

        return dto;
    }
}
