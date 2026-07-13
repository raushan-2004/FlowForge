package com.flowforge.worker.repository;

import com.flowforge.worker.model.Worker;
import com.flowforge.worker.model.WorkerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, Long> {
    Optional<Worker> findByPublicId(UUID publicId);
    Optional<Worker> findByInstanceId(String instanceId);
    long countByStatus(WorkerStatus status);
}
