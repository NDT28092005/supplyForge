package com.supplyforge.ai.domain.repository;

import com.supplyforge.ai.domain.entity.DataSource;
import com.supplyforge.ai.domain.model.DataSourceStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSourceRepository extends JpaRepository<DataSource, Long> {

    List<DataSource> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    List<DataSource> findByWorkspaceIdAndStatus(Long workspaceId, DataSourceStatus status);
}
