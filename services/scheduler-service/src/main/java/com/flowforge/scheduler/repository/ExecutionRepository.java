package com.flowforge.scheduler.repository;

import com.flowforge.scheduler.model.Execution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {
}
