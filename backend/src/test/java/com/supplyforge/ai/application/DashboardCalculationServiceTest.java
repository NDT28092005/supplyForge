package com.supplyforge.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplyforge.ai.api.dto.DeadStockDashboardResponse;
import com.supplyforge.ai.domain.entity.InventoryRecord;
import com.supplyforge.ai.domain.entity.Sku;
import com.supplyforge.ai.domain.entity.Workspace;
import com.supplyforge.ai.domain.repository.InsightAnomalyRepository;
import com.supplyforge.ai.domain.repository.InventoryRecordRepository;
import com.supplyforge.ai.domain.repository.SkuRepository;
import com.supplyforge.ai.domain.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardCalculationServiceTest {

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private InventoryRecordRepository inventoryRecordRepository;

    @Mock
    private InsightAnomalyRepository insightAnomalyRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DeadStockAnalyticsService dashboardCalculationService;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Feed dữ liệu giả và assert tổng tiền dead stock trả về chính xác")
    void testDeadStockCalculation() {
        // Arrange
        Long workspaceId = 1L;
        Workspace ws = new Workspace();
        ws.setId(workspaceId);
        
        when(insightAnomalyRepository.findByWorkspaceIdAndInsightType(workspaceId, DeadStockAnalyticsService.INSIGHT_TYPE))
            .thenReturn(List.of()); // force recompute

        Sku skuDead = new Sku();
        skuDead.setId(10L);
        skuDead.setOriginalName("Hàng ế > 90 ngày");

        Sku skuFresh = new Sku();
        skuFresh.setId(11L);
        skuFresh.setOriginalName("Hàng bán chạy");
        
        Sku skuDeadNoCost = new Sku();
        skuDeadNoCost.setId(12L);
        skuDeadNoCost.setOriginalName("Hàng ế không giá vốn");

        when(skuRepository.findByWorkspaceIdAndParentSkuIsNull(workspaceId))
                .thenReturn(List.of(skuDead, skuFresh, skuDeadNoCost));

        // Record 1: Tồn kho chết, quá 90 ngày (ngưỡng hệ thống đang để là 45 ngày)
        // Qty: 10, Cost: 50.000 -> Value: 500.000
        InventoryRecord rec1 = new InventoryRecord();
        rec1.setSku(skuDead);
        rec1.setQuantity(new BigDecimal("10"));
        rec1.setCostPrice(new BigDecimal("50000"));
        rec1.setRecordDate(LocalDate.now().minusDays(90));

        // Record 2: Tồn kho fresh, mới nhập hôm qua
        // Qty: 20, Cost: 10.000 -> Value: 200.000 (Không tính vào dead stock)
        InventoryRecord rec2 = new InventoryRecord();
        rec2.setSku(skuFresh);
        rec2.setQuantity(new BigDecimal("20"));
        rec2.setCostPrice(new BigDecimal("10000"));
        rec2.setRecordDate(LocalDate.now().minusDays(1));

        // Record 3: Tồn kho chết, quá 50 ngày, không có cost_price, selling_price = 100.000
        // Hệ thống lấy 70% selling_price = 70.000 làm giá vốn
        // Qty: 5 -> Value = 5 * 70.000 = 350.000
        InventoryRecord rec3 = new InventoryRecord();
        rec3.setSku(skuDeadNoCost);
        rec3.setQuantity(new BigDecimal("5"));
        rec3.setSellingPrice(new BigDecimal("100000"));
        rec3.setRecordDate(LocalDate.now().minusDays(50));

        when(inventoryRecordRepository.findBySkuIdIn(List.of(10L, 11L, 12L)))
                .thenReturn(List.of(rec1, rec2, rec3));

        // Act
        DeadStockDashboardResponse result = dashboardCalculationService.getDashboard(workspaceId);

        // Assert
        // Total Inv = 500.000 + 200.000 + 350.000 = 1.050.000
        assertEquals(new BigDecimal("1050000"), result.totalInventoryValueVnd());
        
        // Dead Stock = 500.000 + 350.000 = 850.000
        assertEquals(new BigDecimal("850000"), result.totalDeadStockValueVnd());
        
        // Loss Rate = 4.5% của 850.000 = 38.250
        assertEquals(new BigDecimal("38250"), result.estimatedLoss30DaysVnd());
        
        assertEquals(2, result.staleSkuCount());
    }
}
