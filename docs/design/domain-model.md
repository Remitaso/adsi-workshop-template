# ドメインモデル設計

## ドメイン概要図

```
┌─────────────────────────────────────────────────────────────────┐
│                        勤怠管理ドメイン                           │
├─────────────┬──────────────┬──────────────┬────────────────────┤
│   組織管理   │    打刻      │   休暇管理    │   集計・レポート    │
│             │              │              │                    │
│ Department  │ TimeRecord   │ LeaveRequest │ MonthlyAttendance  │
│ Section     │ TimeEntry    │ LeaveBalance │                    │
│ Team        │              │              │                    │
│ Employee    │              │              │                    │
└─────────────┴──────────────┴──────────────┴────────────────────┘
```

---

## Entity

### Employee（社員）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | PK, 自動採番 |
| employeeCode | String | 社員コード（ユニーク） |
| name | String | 氏名 |
| email | String | メールアドレス（ログインID） |
| password | String | ハッシュ化パスワード |
| role | Role (enum) | EMPLOYEE / MANAGER / HR |
| teamId | Long | 所属チーム FK |
| hireDate | LocalDate | 入社日 |
| active | boolean | 有効/無効 |
| version | Long | 楽観ロック |

### Department（部）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | PK |
| name | String | 部名 |

### Section（課）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | PK |
| name | String | 課名 |
| departmentId | Long | 所属部 FK |

### Team（チーム）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | PK |
| name | String | チーム名 |
| sectionId | Long | 所属課 FK |
| managerId | Long | 管理者（Employee）FK |

### TimeRecord（日次勤怠記録）

1 日 1 社員 1 レコード。承認状態を管理する。

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | PK |
| employeeId | Long | 社員 FK |
| workDate | LocalDate | 勤務日 |
| status | RecordStatus (enum) | DRAFT / SUBMITTED / APPROVED |
| approvedBy | Long | 承認者（Employee）FK, nullable |
| approvedAt | LocalDateTime | 承認日時, nullable |
| version | Long | 楽観ロック |

### TimeEntry（打刻エントリ）

1 回の出勤〜退勤ペア。1 日に複数レコード可能（中抜け対応）。

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | PK |
| timeRecordId | Long | 日次記録 FK |
| clockIn | LocalDateTime | 出勤時刻 |
| clockOut | LocalDateTime | 退勤時刻, nullable（未退勤） |
| version | Long | 楽観ロック |

### LeaveRequest（休暇申請）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | PK |
| employeeId | Long | 社員 FK |
| leaveType | LeaveType (enum) | PAID / HALF_DAY / COMPENSATORY / SPECIAL / ABSENCE |
| startDate | LocalDate | 開始日 |
| endDate | LocalDate | 終了日 |
| reason | String | 申請理由 |
| status | ApprovalStatus (enum) | PENDING / APPROVED / REJECTED |
| approvedBy | Long | 承認者 FK, nullable |
| approvedAt | LocalDateTime | 承認日時, nullable |
| version | Long | 楽観ロック |

### LeaveBalance（有給残高）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | PK |
| employeeId | Long | 社員 FK |
| fiscalYear | int | 対象年度 |
| granted | double | 付与日数 |
| used | double | 取得日数 |
| version | Long | 楽観ロック |

### MonthlyAttendance（月次勤怠集計）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | PK |
| employeeId | Long | 社員 FK |
| yearMonth | YearMonth | 対象年月 |
| totalWorkMinutes | int | 総労働時間（分） |
| overtimeMinutes | int | 残業時間（分） |
| nightOvertimeMinutes | int | 深夜残業時間（分） |
| paidLeaveDays | double | 有給取得日数 |
| absentDays | int | 欠勤日数 |

---

## Value Object

### WorkDuration（勤務時間）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| totalMinutes | int | 総勤務時間（分） |
| regularMinutes | int | 所定内時間（分） |
| overtimeMinutes | int | 残業時間（分） |
| nightMinutes | int | 深夜時間（分） |

---

## Enum

```java
enum Role { EMPLOYEE, MANAGER, HR }

enum RecordStatus { DRAFT, SUBMITTED, APPROVED }

enum ApprovalStatus { PENDING, APPROVED, REJECTED }

enum LeaveType { PAID, HALF_DAY, COMPENSATORY, SPECIAL, ABSENCE }
```

---

## ドメイン関連図

```
Department 1──* Section 1──* Team *──1 Employee(manager)
                                   1
                                   │
                                   *
                              Employee
                              1      1
                              │      │
                              *      *
                    TimeRecord      LeaveRequest
                    1                     
                    │                     
                    *                     
                  TimeEntry         LeaveBalance

Employee 1──* MonthlyAttendance
```

---

## Service

### AttendanceService

- `clockIn(employeeId)` — 出勤打刻
- `clockOut(employeeId)` — 退勤打刻
- `modifyEntry(entryId, clockIn, clockOut)` — 打刻修正（DRAFT 状態のみ）
- `submitForApproval(timeRecordId)` — 承認申請
- `approve(timeRecordId, approverId)` — 承認
- `calculateDailyWork(timeRecordId)` → WorkDuration — 日次勤務時間計算

### LeaveService

- `apply(employeeId, leaveType, startDate, endDate, reason)` — 休暇申請
- `approve(requestId, approverId)` — 承認
- `reject(requestId, approverId)` — 却下
- `getBalance(employeeId, fiscalYear)` → LeaveBalance — 残高照会
- `grantAnnualLeave(employeeId)` — 年次有給付与

### AttendanceSummaryService

- `calculateMonthly(employeeId, yearMonth)` → MonthlyAttendance — 月次集計
- `exportCsv(yearMonth, departmentId)` — CSV エクスポート
- `checkOvertimeAlert(employeeId, yearMonth)` — 残業アラート判定

### NotificationService

- `notifyMissingClock(employeeId)` — 打刻忘れ通知
- `notifyOvertimeAlert(employeeId, overtimeHours)` — 残業アラート
- `notifyLeaveResult(requestId)` — 休暇申請結果通知

### EmployeeService

- `create(...)` — 社員登録
- `update(...)` — 社員情報更新
- `deactivate(employeeId)` — 無効化
- `findByTeam(teamId)` — チーム社員一覧

---

## Repository（interface）

| Repository | 主なメソッド |
|-----------|------------|
| EmployeeRepository | findByEmail, findByTeamId, findActiveEmployees |
| TimeRecordRepository | findByEmployeeAndDate, findByEmployeeAndMonth |
| TimeEntryRepository | findByTimeRecordId |
| LeaveRequestRepository | findByEmployeeAndStatus, findPendingByApprover |
| LeaveBalanceRepository | findByEmployeeAndYear |
| MonthlyAttendanceRepository | findByEmployeeAndMonth, findByMonth |
| DepartmentRepository | findAll |
| SectionRepository | findByDepartmentId |
| TeamRepository | findBySectionId |
