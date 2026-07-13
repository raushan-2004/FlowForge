package com.flowforge.scheduler.repository;

import com.flowforge.scheduler.model.ExecutionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionAttemptRepository extends JpaRepository<ExecutionAttempt, Long> {
}
