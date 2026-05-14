package com.supplyforge.ai.domain.repository;

import com.supplyforge.ai.domain.entity.InsightAnomaly;
import com.supplyforge.ai.domain.model.InsightSeverity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightAnomalyRepository extends JpaRepository<InsightAnomaly, Long> {

    List<InsightAnomaly> findByWorkspaceIdAndComputedAtAfterOrderByComputedAtDesc(
            Long workspaceId, Instant since);

    List<InsightAnomaly> findByWorkspaceIdAndInsightType(Long workspaceId, String insightType);

    List<InsightAnomaly> findByWorkspaceIdAndSeverity(Long workspaceId, InsightSeverity severity);
}
