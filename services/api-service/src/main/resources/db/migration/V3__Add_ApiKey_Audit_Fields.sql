-- FlowForge Migration - Add Audit Fields to API Keys

ALTER TABLE api_keys ADD COLUMN created_by UUID NOT NULL;
ALTER TABLE api_keys ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL;
ALTER TABLE api_keys ADD COLUMN updated_by UUID NOT NULL;
