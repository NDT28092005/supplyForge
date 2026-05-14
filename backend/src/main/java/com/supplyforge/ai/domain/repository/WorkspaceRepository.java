package com.supplyforge.ai.domain.repository;

import com.supplyforge.ai.domain.entity.Workspace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findBySlug(String slug);
}
