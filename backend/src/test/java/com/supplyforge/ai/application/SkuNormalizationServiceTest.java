package com.supplyforge.ai.application;

import com.supplyforge.ai.api.dto.SkuDuplicatePairResponse;
import com.supplyforge.ai.domain.entity.Sku;
import com.supplyforge.ai.domain.entity.Workspace;
import com.supplyforge.ai.domain.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkuNormalizationServiceTest {

    @Mock
    private SkuRepository skuRepository;

    @InjectMocks
    private SkuMergeService skuNormalizationService;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Unit test logic gom nhóm SKU dựa trên string similarity (Levenshtein distance <= 2)")
    void testSkuGroupingBySimilarity() {
        // Arrange
        Long workspaceId = 1L;
        Workspace ws = new Workspace();
        ws.setId(workspaceId);

        Sku sku1 = new Sku();
        sku1.setId(10L);
        sku1.setOriginalName("Áo thun đỏ");
        sku1.setNormalizedName("áo thun đỏ");

        Sku sku2 = new Sku();
        sku2.setId(11L);
        sku2.setOriginalName("Ao thun đỏ");
        sku2.setNormalizedName("ao thun đỏ"); // distance 1 (á->a)

        Sku sku3 = new Sku();
        sku3.setId(12L);
        sku3.setOriginalName("Giày tây xanh");
        sku3.setNormalizedName("giày tây xanh");

        when(skuRepository.findByWorkspaceIdAndParentSkuIsNull(workspaceId)).thenReturn(List.of(sku1, sku2, sku3));

        // Act
        List<SkuDuplicatePairResponse> candidates = skuNormalizationService.listDuplicateCandidates(workspaceId);

        // Assert
        // Chỉ có 1 cặp (sku1, sku2) được gợi ý vì distance là 1 (<= 2)
        assertEquals(1, candidates.size());
        assertEquals(10L, candidates.get(0).skuAId());
        assertEquals(11L, candidates.get(0).skuBId());
        assertEquals(1, candidates.get(0).distance());
    }
}
