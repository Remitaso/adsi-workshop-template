-- 初期マスタデータ
INSERT INTO departments (id, name, created_at, updated_at) VALUES
(1, '開発部', NOW(), NOW()),
(2, '人事部', NOW(), NOW());

INSERT INTO sections (id, name, department_id, created_at, updated_at) VALUES
(1, '第1開発課', 1, NOW(), NOW()),
(2, '人事課', 2, NOW(), NOW());

INSERT INTO teams (id, name, section_id, created_at, updated_at) VALUES
(1, '開発1チーム', 1, NOW(), NOW()),
(2, '人事チーム', 2, NOW(), NOW());

-- パスワード: password123 (BCrypt)
INSERT INTO employees (id, employee_code, name, email, password, role, team_id, hire_date, active, version, created_at, updated_at) VALUES
(1, 'EMP001', '田中太郎', 'tanaka@example.com', '$2b$12$PYivQam4XySOXuy4YKDAEeCkmCaUBGUB0BkZDZxRaMmLJs.FUqer.', 'EMPLOYEE', 1, '2020-04-01', true, 0, NOW(), NOW()),
(2, 'MGR001', '鈴木一郎', 'suzuki@example.com', '$2b$12$PYivQam4XySOXuy4YKDAEeCkmCaUBGUB0BkZDZxRaMmLJs.FUqer.', 'MANAGER', 1, '2018-04-01', true, 0, NOW(), NOW()),
(3, 'HR001', '佐藤花子', 'sato@example.com', '$2b$12$PYivQam4XySOXuy4YKDAEeCkmCaUBGUB0BkZDZxRaMmLJs.FUqer.', 'HR', 2, '2019-04-01', true, 0, NOW(), NOW());

UPDATE teams SET manager_id = 2 WHERE id = 1;
