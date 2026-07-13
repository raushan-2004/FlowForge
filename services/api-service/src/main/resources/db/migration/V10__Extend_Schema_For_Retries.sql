ALTER TABLE executions ADD COLUMN next_attempt_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE processed_completed_events (
    execution_public_id UUID NOT NULL,
    attempt_number INT NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (execution_public_id, attempt_number)
);
