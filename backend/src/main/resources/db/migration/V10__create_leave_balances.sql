CREATE TABLE leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    grant_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    granted_days DECIMAL(4,1) NOT NULL,
    used_days DECIMAL(4,1) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_leave_balances_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT uq_leave_balances_employee_grant UNIQUE (employee_id, grant_date)
);

CREATE INDEX idx_leave_balances_employee ON leave_balances(employee_id);
CREATE INDEX idx_leave_balances_expiry ON leave_balances(expiry_date);
