CREATE TABLE time_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    work_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    approved_by BIGINT REFERENCES employees(id),
    approved_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_time_records_employee_date UNIQUE (employee_id, work_date)
);

CREATE INDEX idx_time_records_employee_date ON time_records (employee_id, work_date);
CREATE INDEX idx_time_records_employee_month ON time_records (employee_id, work_date);
