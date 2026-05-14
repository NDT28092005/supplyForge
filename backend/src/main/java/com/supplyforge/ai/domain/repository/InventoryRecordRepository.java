package com.supplyforge.ai.domain.repository;

import com.supplyforge.ai.domain.entity.InventoryRecord;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRecordRepository extends JpaRepository<InventoryRecord, Long> {

    List<InventoryRecord> findBySkuIdIn(Collection<Long> skuIds);

    List<InventoryRecord> findBySkuId(Long skuId);

    List<InventoryRecord> findBySkuIdAndRecordDateBetween(Long skuId, LocalDate from, LocalDate to);

    List<InventoryRecord> findByDataSourceId(Long dataSourceId);
}
