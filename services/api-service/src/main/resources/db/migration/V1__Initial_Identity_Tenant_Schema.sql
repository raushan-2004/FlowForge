-- FlowForge Initial Schema - Identity, Tenant, Membership, Project, and API-Key Persistence

-- 1. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

-- Unique index for case-insensitive email (email normalization protection at SQL level)
CREATE UNIQUE INDEX idx_users_email_lower ON users (LOWER(email));

-- 2. Tenants Table
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

-- 3. Tenant Memberships Table (User <-> Tenant)
CREATE TABLE tenant_memberships (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_membership_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_tenant_user UNIQUE (tenant_id, user_id),
    CONSTRAINT chk_membership_role CHECK (role IN ('OWNER', 'ADMIN', 'DEVELOPER', 'VIEWER'))
);

-- 4. Projects Table
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_project_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT uq_project_composite UNIQUE (id, tenant_id), -- Composite key for API key validation
    CONSTRAINT chk_project_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

-- Tenant-scoped case-insensitive unique project name index
CREATE UNIQUE INDEX idx_projects_tenant_name_lower ON projects (tenant_id, LOWER(name));

-- 5. API Keys Table
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    key_id VARCHAR(255) UNIQUE NOT NULL,
    display_prefix VARCHAR(50) NOT NULL,
    secret_hash VARCHAR(255) NOT NULL,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expiry_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    -- Composite foreign key guarantees that project_id and tenant_id match the projects table
    CONSTRAINT fk_api_key_project_tenant FOREIGN KEY (project_id, tenant_id) REFERENCES projects(id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT chk_api_key_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);
