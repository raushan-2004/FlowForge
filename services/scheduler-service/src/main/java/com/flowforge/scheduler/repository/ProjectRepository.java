package com.flowforge.scheduler.repository;

import com.flowforge.scheduler.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
