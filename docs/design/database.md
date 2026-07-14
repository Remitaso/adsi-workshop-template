# DB 設計

## ER 図

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ departments  │1───*│  sections    │1───*│    teams     │
│──────────────│     │──────────────│     │──────────────│
│ id (PK)      │     │ id (PK)      │     │ id (PK)      │
│ name         │     │ name         │     │ name         │
│              │     │ department_id│     │ section_id   │
│              │     │              │     │ manager_id   │
└──────────────┘     └──────────────┘     └──────┬───────┘
                                                  │1
                                                  │
                                                  │*
                                          ┌───────┴──────┐
                                          │  employees   │
                                          │──────────────│
                                          │ id (PK)      │
                                          │ employee_code│
                                          │ name         │
                                          │ email        │
                                          │ password     │
                                          │ role         │
                                          │ team_id (FK) │
                                          │ hire_date    │
                                          │ active       │
                                          │ version      │
                                          └──┬───────┬───┘
                                             │1      │1
                              ┌──────────────┘       └─────────────┐
                              │*                                    │*
                     ┌────────┴──────┐                    ┌────────┴───────┐
                     │ time_records  │                    │ leave_requests │
                     │───────────────│                    │────────────────│
                     │ id (PK)       │                    │ id (PK)        │
                     │ employee_id   │                    │ employee_id    │
                     │ work_date     │                    │ leave_type     │
                     │ status        │                    │ start_date     │
                     │ approved_by   │                    │ end_date       │
                     │ approved_at   │                    │ reason         │
                     │ version       │                    │ status         │
                     └────────┬──────┘                    │ approved_by    │
                              │1                          │ approved_at    │
                              │                           │ version        │
                              │*                          └────────────────┘
                     ┌────────┴──────┐
                     │ time_entries  │          ┌─────────────────────┐
                     │───────────────│          │   leave_balances    │
                     │ id (PK)       │          │─────────────────────│
                     │ time_record_id│          │ id (PK)             │
                     │ clock_in      │          │ employee_id (FK)    │
                     │ clock_out     │          │ fiscal_year         │
                     │ version       │          │ granted             │
                     └───────────────┘          │ used                │
                                                │ version             │
                     ┌───────────────────────┐  └─────────────────────┘
                     │ monthly_attendances   │
                     │───────────────────────│
                     │ id (PK)               │
                     │ employee_id (FK)      │
                     │ year_month            │
                     │ total_work_minutes    │
                     │ overtime_minutes      │
                     │ night_overtime_minutes│
                     │ paid_leave_days       │
                     │ absent_days           │
                     └───────────────────────┘
```

---

## テーブル定義

### departments

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| name | VARCHAR(100) | NOT NULL | 部名 |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

### sections

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| name | VARCHAR(100) | NOT NULL | 課名 |
| department_id | BIGINT | NOT NULL, FK(departments) | |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

### teams

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| name | VARCHAR(100) | NOT NULL | チーム名 |
| section_id | BIGINT | NOT NULL, FK(sections) | |
| manager_id | BIGINT | FK(employees), nullable | 管理者 |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

### employees

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| employee_code | VARCHAR(20) | NOT NULL, UNIQUE | 社員コード |
| name | VARCHAR(100) | NOT NULL | 氏名 |
| email | VARCHAR(255) | NOT NULL, UNIQUE | ログインID |
| password | VARCHAR(255) | NOT NULL | BCrypt ハッシュ |
| role | VARCHAR(20) | NOT NULL | EMPLOYEE/MANAGER/HR |
| team_id | BIGINT | NOT NULL, FK(teams) | |
| hire_date | DATE | NOT NULL | 入社日 |
| active | BOOLEAN | NOT NULL DEFAULT TRUE | |
| version | BIGINT | NOT NULL DEFAULT 0 | 楽観ロック |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

### time_records

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| employee_id | BIGINT | NOT NULL, FK(employees) | |
| work_date | DATE | NOT NULL | 勤務日 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'DRAFT' | DRAFT/SUBMITTED/APPROVED |
| approved_by | BIGINT | FK(employees), nullable | |
| approved_at | TIMESTAMP | nullable | |
| version | BIGINT | NOT NULL DEFAULT 0 | |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

**UNIQUE制約**: (employee_id, work_date)

### time_entries

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| time_record_id | BIGINT | NOT NULL, FK(time_records) | |
| clock_in | TIMESTAMP | NOT NULL | 出勤時刻 |
| clock_out | TIMESTAMP | nullable | 退勤時刻 |
| version | BIGINT | NOT NULL DEFAULT 0 | |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

### leave_requests

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| employee_id | BIGINT | NOT NULL, FK(employees) | |
| leave_type | VARCHAR(20) | NOT NULL | PAID/HALF_DAY/COMPENSATORY/SPECIAL/ABSENCE |
| start_date | DATE | NOT NULL | |
| end_date | DATE | NOT NULL | |
| reason | VARCHAR(500) | | 申請理由 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'PENDING' | PENDING/APPROVED/REJECTED |
| approved_by | BIGINT | FK(employees), nullable | |
| approved_at | TIMESTAMP | nullable | |
| version | BIGINT | NOT NULL DEFAULT 0 | |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

### leave_balances

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| employee_id | BIGINT | NOT NULL, FK(employees) | |
| fiscal_year | INT | NOT NULL | 対象年度 |
| granted | DECIMAL(4,1) | NOT NULL | 付与日数 |
| used | DECIMAL(4,1) | NOT NULL DEFAULT 0 | 取得日数 |
| version | BIGINT | NOT NULL DEFAULT 0 | |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

**UNIQUE制約**: (employee_id, fiscal_year)

### monthly_attendances

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| employee_id | BIGINT | NOT NULL, FK(employees) | |
| year_month | VARCHAR(7) | NOT NULL | 'YYYY-MM' 形式 |
| total_work_minutes | INT | NOT NULL DEFAULT 0 | 総労働分 |
| overtime_minutes | INT | NOT NULL DEFAULT 0 | 残業分 |
| night_overtime_minutes | INT | NOT NULL DEFAULT 0 | 深夜残業分 |
| paid_leave_days | DECIMAL(4,1) | NOT NULL DEFAULT 0 | |
| absent_days | INT | NOT NULL DEFAULT 0 | |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

**UNIQUE制約**: (employee_id, year_month)

---

## インデックス

| テーブル | インデックス | 用途 |
|---------|------------|------|
| employees | idx_employees_email | ログイン検索 |
| employees | idx_employees_team_id | チーム一覧 |
| time_records | idx_time_records_employee_date | 日次検索 |
| time_records | idx_time_records_employee_month | 月次一覧（work_date の年月） |
| time_entries | idx_time_entries_record_id | 打刻一覧 |
| leave_requests | idx_leave_requests_employee | 社員の申請一覧 |
| leave_requests | idx_leave_requests_status | 承認待ち検索 |
| monthly_attendances | idx_monthly_att_yearmonth | 月次集計検索 |

---

## データ保持ポリシー

- 全勤怠データ: **3 年間**保持（労基法準拠）
- 3 年超のデータ: アーカイブ or 削除（運用フェーズで決定）
