package com.flowforge.api.repository;

import com.flowforge.api.model.NodeExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NodeExecutionRepository extends JpaRepository<NodeExecution, Long> {
    Optional<NodeExecution> findByExecutionPublicId(UUID executionPublicId);
    List<NodeExecution> findByWorkflowRunId(Long workflowRunId);
    Optional<NodeExecution> findByWorkflowRunIdAndNodeId(Long workflowRunId, String nodeId);
}
