package com.supplyforge.ai.domain.repository;

import com.supplyforge.ai.domain.entity.Sku;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    List<Sku> findByWorkspaceId(Long workspaceId);

    List<Sku> findByWorkspaceIdAndParentSkuIsNull(Long workspaceId);

    List<Sku> findByParentSkuId(Long parentSkuId);
}
