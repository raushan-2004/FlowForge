-- Seed default tenant
INSERT INTO tenants (id, public_id, name, status, created_at, updated_at, created_by, updated_by)
VALUES (1, '00000000-0000-0000-0000-000000000001', 'Default Tenant', 'ACTIVE', NOW(), NOW(), '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002')
ON CONFLICT (id) DO NOTHING;

-- Seed default user (email: login@flowforge.com, password: supersecret123)
-- BCrypt of supersecret123 is: $2a$10$v2l5Jp9J/U2Tpx9i2kUo9eZp9p4j6Uf.xP5x/N6n7y2t1T0o4sMKe
INSERT INTO users (id, public_id, email, password_hash, status, created_at, updated_at)
VALUES (1, '00000000-0000-0000-0000-000000000002', 'login@flowforge.com', '$2a$10$v2l5Jp9J/U2Tpx9i2kUo9eZp9p4j6Uf.xP5x/N6n7y2t1T0o4sMKe', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Seed tenant membership as OWNER
INSERT INTO tenant_memberships (id, tenant_id, user_id, role, created_at)
VALUES (1, 1, 1, 'OWNER', NOW())
ON CONFLICT (id) DO NOTHING;

-- Seed a default project so there is a workspace ready to go!
INSERT INTO projects (id, public_id, tenant_id, name, status, created_at, updated_at, created_by, updated_by)
VALUES (1, '00000000-0000-0000-0000-000000000003', 1, 'Main Workspace', 'ACTIVE', NOW(), NOW(), '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002')
ON CONFLICT (id) DO NOTHING;

-- Sync sequences
SELECT setval('tenants_id_seq', COALESCE((SELECT MAX(id)+1 FROM tenants), 1), false);
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id)+1 FROM users), 1), false);
SELECT setval('tenant_memberships_id_seq', COALESCE((SELECT MAX(id)+1 FROM tenant_memberships), 1), false);
SELECT setval('projects_id_seq', COALESCE((SELECT MAX(id)+1 FROM projects), 1), false);
