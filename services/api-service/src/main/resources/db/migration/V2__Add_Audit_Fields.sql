-- FlowForge Migration - Add Audit Fields to Tenants and Projects

ALTER TABLE tenants ADD COLUMN created_by UUID NOT NULL;
ALTER TABLE tenants ADD COLUMN updated_by UUID NOT NULL;

ALTER TABLE projects ADD COLUMN created_by UUID NOT NULL;
ALTER TABLE projects ADD COLUMN updated_by UUID NOT NULL;
