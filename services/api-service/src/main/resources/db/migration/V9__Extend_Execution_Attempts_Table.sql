ALTER TABLE execution_attempts ADD COLUMN http_status INT;
ALTER TABLE execution_attempts ADD COLUMN response_size BIGINT;
ALTER TABLE execution_attempts ADD COLUMN body_truncated BOOLEAN;
ALTER TABLE execution_attempts ADD COLUMN network_error VARCHAR(50);
ALTER TABLE execution_attempts ADD COLUMN content_type VARCHAR(255);
