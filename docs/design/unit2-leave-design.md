# Unit 2: 休暇管理 — 詳細設計

## 1. ドメインモデル

### Entity

#### LeaveRequest（休暇申請）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | PK, 自動採番 |
| employeeId | Long | 社員 FK |
| leaveType | LeaveType (enum) | PAID / HALF_DAY / COMPENSATORY / SPECIAL / ABSENCE |
| startDate | LocalDate | 開始日 |
| endDate | LocalDate | 終了日 |
| reason | String | 申請理由 |
| status | ApprovalStatus (enum) | PENDING / APPROVED / REJECTED |
| approvedBy | Long | 承認者 FK, nullable |
| approvedAt | LocalDateTime | 承認日時, nullable |
| rejectReason | String | 却下理由, nullable |
| version | Long | 楽観ロック |

#### LeaveBalance（有給残高）

**1社員 × 1付与回（年度）= 1レコード。繰越の時効管理と古い年度からの消化に対応。**

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | PK, 自動採番 |
| employeeId | Long | 社員 FK |
| grantDate | LocalDate | 付与日（入社応当日） |
| expiryDate | LocalDate | 有効期限（付与日 + 2年） |
| grantedDays | BigDecimal | 付与日数 |
| usedDays | BigDecimal | 消化日数 |
| version | Long | 楽観ロック |

> `fiscal_year` INT → `grantDate` / `expiryDate` に変更。
> 入社日基準のため暦年度では管理できず、付与日と有効期限で管理する。

### Enum

```java
enum LeaveType { PAID, HALF_DAY, COMPENSATORY, SPECIAL, ABSENCE }

enum ApprovalStatus { PENDING, APPROVED, REJECTED }
```

### Value Object

#### LeaveDays（休暇日数）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| days | BigDecimal | 日数（0.5刻み: 半休対応） |

---

## 2. DB 設計（Flyway マイグレーション）

### V7__create_leave_requests.sql

```sql
CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    reject_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_leave_requests_approver FOREIGN KEY (approved_by) REFERENCES employees(id)
);

CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_leave_requests_employee_status ON leave_requests(employee_id, status);
```

### V8__create_leave_balances.sql

```sql
CREATE TABLE leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    grant_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    granted_days DECIMAL(4,1) NOT NULL,
    used_days DECIMAL(4,1) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_balances_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT uq_leave_balances_employee_grant UNIQUE (employee_id, grant_date)
);

CREATE INDEX idx_leave_balances_employee ON leave_balances(employee_id);
CREATE INDEX idx_leave_balances_expiry ON leave_balances(expiry_date);
```

---

## 3. API 設計

### 休暇申請 API

#### POST /api/v1/leaves — 休暇申請

**Request**
```json
{
  "leaveType": "PAID",
  "startDate": "2026-07-20",
  "endDate": "2026-07-20",
  "reason": "私用のため"
}
```

**Response 201**
```json
{
  "id": 5,
  "leaveType": "PAID",
  "startDate": "2026-07-20",
  "endDate": "2026-07-20",
  "reason": "私用のため",
  "status": "PENDING",
  "createdAt": "2026-07-14T10:00:00"
}
```

**エラー**
- 400: バリデーションエラー（開始日 > 終了日、過去日指定等）
- 409: 有給残日数不足（leaveType=PAID/HALF_DAY のとき）

#### GET /api/v1/leaves — 自分の休暇申請一覧

**Query Parameters**
- `status` (optional): PENDING / APPROVED / REJECTED
- `year` (optional): 対象年（default: 当年）

**Response 200**
```json
{
  "items": [
    {
      "id": 5,
      "leaveType": "PAID",
      "startDate": "2026-07-20",
      "endDate": "2026-07-20",
      "reason": "私用のため",
      "status": "PENDING",
      "createdAt": "2026-07-14T10:00:00"
    }
  ]
}
```

#### DELETE /api/v1/leaves/{id} — 休暇申請取消

PENDING 状態のみ取消可能。

**Response 204**

**エラー**
- 404: 存在しない
- 409: PENDING 以外（承認済み/却下済みは取消不可）

#### GET /api/v1/leaves/balance — 有給残高照会

**Response 200**
```json
{
  "totalRemaining": 12.0,
  "details": [
    {
      "grantDate": "2025-10-01",
      "expiryDate": "2027-10-01",
      "granted": 15.0,
      "used": 5.0,
      "remaining": 10.0
    },
    {
      "grantDate": "2026-10-01",
      "expiryDate": "2028-10-01",
      "granted": 17.0,
      "used": 15.0,
      "remaining": 2.0
    }
  ]
}
```

### 承認 API（管理者用）

#### GET /api/v1/approval/leave-requests — 承認待ち休暇一覧

自チームの PENDING 申請のみ表示。

**Response 200**
```json
{
  "items": [
    {
      "id": 5,
      "employeeId": 1,
      "employeeName": "田中太郎",
      "leaveType": "PAID",
      "startDate": "2026-07-20",
      "endDate": "2026-07-20",
      "reason": "私用のため",
      "createdAt": "2026-07-14T10:00:00"
    }
  ]
}
```

#### POST /api/v1/approval/leave-requests/{requestId}/approve — 承認

承認時に有給残高を減算（古い付与分から消化）。

**Response 200**
```json
{
  "id": 5,
  "status": "APPROVED",
  "approvedBy": 2,
  "approvedAt": "2026-07-15T09:00:00"
}
```

**エラー**
- 403: 承認権限なし（自チームの manager でない）
- 404: 申請が存在しない
- 409: 既に承認/却下済み

#### POST /api/v1/approval/leave-requests/{requestId}/reject — 却下

**Request**
```json
{
  "reason": "業務都合のため"
}
```

**Response 200**
```json
{
  "id": 5,
  "status": "REJECTED",
  "rejectReason": "業務都合のため"
}
```

### 有給付与 API（管理者/人事/バッチ用）

#### POST /api/v1/leaves/grant/{employeeId} — 手動付与（人事用）

> 通常はバッチ/スケジューラで自動付与。人事が手動補正する場合に使用。

**Request**
```json
{
  "grantDate": "2026-10-01",
  "days": 15.0
}
```

**Response 201**

---

## 4. Service 設計

### LeaveService (interface)

```java
public interface LeaveService {
    // 休暇申請
    LeaveRequest apply(Long employeeId, LeaveType leaveType,
                       LocalDate startDate, LocalDate endDate, String reason);

    // 申請取消（PENDING のみ）
    void cancel(Long requestId, Long employeeId);

    // 自分の申請一覧
    List<LeaveRequest> findByEmployee(Long employeeId, ApprovalStatus status, Integer year);

    // 承認（残高減算含む）
    LeaveRequest approve(Long requestId, Long approverId);

    // 却下
    LeaveRequest reject(Long requestId, Long approverId, String reason);

    // 有給残高照会（有効期限内のみ）
    LeaveBalanceSummary getBalance(Long employeeId);

    // 年次有給付与（入社応当日に呼ばれる想定）
    LeaveBalance grantAnnualLeave(Long employeeId);

    // 消化日数計算（PAID=日数、HALF_DAY=0.5）
    BigDecimal calculateLeaveDays(LeaveType leaveType, LocalDate startDate, LocalDate endDate);
}
```

### ドメインルール詳細

#### 有給付与日数計算

```
勤続年数（入社日からの経過）→ 付与日数:
  0年（入社日、前倒し付与）: 10日
  1年6ヶ月: 11日
  2年6ヶ月: 12日
  3年6ヶ月: 14日
  4年6ヶ月: 16日
  5年6ヶ月: 18日
  6年6ヶ月以上: 20日
```

#### 残高消化ロジック（古い年度から消化）

```
1. 有効期限内の LeaveBalance を grant_date 昇順で取得
2. 消化日数分を古い順に used_days に加算
3. 各レコードの remaining（= granted - used）が 0 になったら次へ
4. 全レコードの remaining 合計が消化日数に足りなければエラー
```

#### 半休の扱い

- `HALF_DAY` は 0.5 日消費
- start_date = end_date でなければエラー（半休は1日のみ指定可能）

#### 申請時の残高チェック

- `PAID`: startDate〜endDate の営業日数分の残高が必要
- `HALF_DAY`: 0.5 日分の残高が必要
- `COMPENSATORY` / `SPECIAL` / `ABSENCE`: 残高チェック不要

#### 承認権限チェック

```
1. 申請者の team_id を取得
2. teams テーブルから manager_id を取得
3. approverId == manager_id であることを確認
4. 不一致 → 403 Forbidden
```

---

## 5. パッケージ構成

```
com.example.attendance.leave/
├── controller/
│   └── LeaveController.java
├── dto/
│   ├── CreateLeaveRequest.java (record)
│   ├── LeaveResponse.java (record)
│   ├── LeaveBalanceResponse.java (record)
│   ├── LeaveBalanceDetailResponse.java (record)
│   ├── RejectLeaveRequest.java (record)
│   └── GrantLeaveRequest.java (record)
├── entity/
│   ├── LeaveRequest.java
│   ├── LeaveBalance.java
│   ├── LeaveType.java (enum)
│   └── ApprovalStatus.java (enum)
├── repository/
│   ├── LeaveRequestRepository.java
│   └── LeaveBalanceRepository.java
├── service/
│   ├── LeaveService.java (interface)
│   └── impl/
│       └── LeaveServiceImpl.java
└── exception/
    ├── InsufficientLeaveBalanceException.java
    └── LeaveRequestNotFoundException.java
```

> Unit 1（打刻）は `com.example.attendance.attendance/` パッケージに配置想定。
> 共通の承認 API は Unit 1 側に `ApprovalController` があるなら、
> 休暇承認エンドポイントだけ `LeaveController` に含める（衝突回避）。

---

## 6. Unit 1 との境界

| 項目 | Unit 1（打刻） | Unit 2（休暇） | 共有 |
|------|--------------|--------------|------|
| Flyway | V6〜 | V7〜V8 | V1〜V5（Unit 0） |
| パッケージ | `attendance/` | `leave/` | `config/`, `entity/Employee` |
| 承認 API | `/approval/time-records/` | `/approval/leave-requests/` | パス分離で衝突なし |
| Controller | AttendanceController | LeaveController | — |
| Entity 参照 | Employee (read) | Employee (read) | Employee は Unit 0 |

**マージ時の衝突リスク最小化:**
- Flyway バージョンを V7〜V8 に固定（Unit 1 が V6 を使う想定）
- パッケージを `leave` サブパッケージに完全分離
- 共通 Entity（Employee, Team）は読み取りのみ。変更しない

---

## 7. 画面設計（Frontend）

### S-05 休暇申請画面 `/leaves/new`

```
┌─────────────────────────────────────────────────────┐
│  休暇申請                                            │
├─────────────────────────────────────────────────────┤
│                                                     │
│  休暇種類:  [有給休暇       ▼]                       │
│            有給休暇 / 半休 / 代休 / 特別休暇 / 欠勤   │
│                                                     │
│  開始日:    [2026-07-20]                            │
│  終了日:    [2026-07-20]                            │
│  ※半休の場合は終了日=開始日で固定                      │
│                                                     │
│  理由:     [私用のため                    ]          │
│                                                     │
│  ┌───────────────────────────────┐                 │
│  │ 有給残日数: 12.0日             │                 │
│  │ (内訳: 前年度繰越 2.0日 +      │                 │
│  │        今年度 10.0日)          │                 │
│  └───────────────────────────────┘                 │
│                                                     │
│            [キャンセル]  [申請する]                    │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### S-06 休暇一覧画面 `/leaves`

```
┌─────────────────────────────────────────────────────┐
│  休暇管理                                            │
├─────────────────────────────────────────────────────┤
│                                                     │
│  有給残高: 12.0日 / 20.0日        [休暇を申請する]    │
│  ┌───────────────────────────────────────┐         │
│  │ ■■■■■■■■■■■■░░░░░░░░ 60%            │         │
│  └───────────────────────────────────────┘         │
│                                                     │
│  フィルタ: [全て ▼]  [2026年 ▼]                     │
│                                                     │
│  日付        種別    期間          状態    操作       │
│  ──────────────────────────────────────────────────  │
│  7/14申請  有給   7/20〜7/20    ⏳承認待ち [取消]    │
│  6/20申請  半休   6/25          ✓承認済み           │
│  5/10申請  有給   5/15〜5/16    ✓承認済み           │
│  4/01申請  有給   4/10〜4/11    ✗却下              │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 8. 実装順序（TDD）

1. **Flyway マイグレーション** — V7, V8
2. **Entity** — LeaveRequest, LeaveBalance, LeaveType, ApprovalStatus
3. **Repository** — LeaveRequestRepository, LeaveBalanceRepository
4. **Service テスト → Service 実装**
   - 有給付与日数計算
   - 残高チェック
   - 申請 → 承認 → 残高減算
   - 申請 → 却下
   - 取消
5. **Controller テスト → Controller 実装**
6. **Frontend** — 休暇申請画面、休暇一覧画面
