package com.flowforge.api.repository;

import com.flowforge.api.model.WorkflowRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, Long> {
    Optional<WorkflowRun> findByPublicId(UUID publicId);
}
