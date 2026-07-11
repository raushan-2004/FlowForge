-- FlowForge Migration - Create Jobs Table

CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    http_method VARCHAR(10) NOT NULL,
    target_url VARCHAR(2048) NOT NULL,
    request_headers TEXT,
    request_body TEXT,
    timeout_seconds INT NOT NULL,
    retry_max_attempts INT,
    retry_strategy VARCHAR(50),
    retry_base_delay_seconds INT,
    schedule_type VARCHAR(20) NOT NULL,
    cron_expression VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT fk_jobs_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_jobs_project_name_lower ON jobs (project_id, LOWER(name));
