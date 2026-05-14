package com.supplyforge.ai.domain.repository;

import com.supplyforge.ai.domain.entity.WorkspaceMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

    List<WorkspaceMember> findByUserId(Long userId);

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);
}
