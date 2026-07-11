-- FlowForge Migration - Create Executions and Attempts Tables

CREATE TABLE executions (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    job_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    trigger_source VARCHAR(255),
    current_status VARCHAR(20) NOT NULL,
    queued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    current_attempt_number INT NOT NULL,
    max_attempts INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_executions_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_executions_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_executions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE TABLE execution_attempts (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    execution_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    worker_id VARCHAR(255),
    duration BIGINT,
    error_category VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_execution_attempts_execution FOREIGN KEY (execution_id) REFERENCES executions(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_attempts_execution_number ON execution_attempts (execution_id, attempt_number);
