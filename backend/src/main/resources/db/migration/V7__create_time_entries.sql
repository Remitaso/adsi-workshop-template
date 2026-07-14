CREATE TABLE time_entries (
    id BIGSERIAL PRIMARY KEY,
    time_record_id BIGINT NOT NULL REFERENCES time_records(id),
    clock_in TIMESTAMP NOT NULL,
    clock_out TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_time_entries_record_id ON time_entries (time_record_id);
