-- FlowForge Migration - Extend Jobs for Scheduler Metadata

ALTER TABLE jobs ADD COLUMN next_fire_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE jobs ADD COLUMN last_scheduled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE jobs ADD COLUMN schedule_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_jobs_next_fire ON jobs (status, next_fire_at) WHERE status = 'ACTIVE' AND next_fire_at IS NOT NULL;
