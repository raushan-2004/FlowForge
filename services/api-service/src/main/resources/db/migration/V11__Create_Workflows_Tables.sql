CREATE TABLE workflow_definitions (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    project_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    version INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    definition_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_workflow_definitions_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_workflow_definitions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE TABLE workflow_runs (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    workflow_definition_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_workflow_runs_definition FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions(id) ON DELETE CASCADE
);

CREATE TABLE node_executions (
    id BIGSERIAL PRIMARY KEY,
    workflow_run_id BIGINT NOT NULL,
    node_id VARCHAR(255) NOT NULL,
    execution_public_id UUID UNIQUE,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_node_executions_run FOREIGN KEY (workflow_run_id) REFERENCES workflow_runs(id) ON DELETE CASCADE
);

CREATE INDEX idx_node_executions_run ON node_executions (workflow_run_id);
