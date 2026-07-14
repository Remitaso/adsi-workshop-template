# API 設計

## 概要

- ベース URL: `/api/v1`
- 認証: Session Cookie (Spring Security)
- レスポンス形式: JSON
- エラーレスポンス: 共通フォーマット

### エラーレスポンス共通形式

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "入力値に問題があります",
    "details": [
      { "field": "email", "message": "メールアドレスの形式が正しくありません" }
    ]
  }
}
```

---

## 認証 API

### POST /api/v1/auth/login

ログイン

**Request**
```json
{
  "email": "tanaka@example.com",
  "password": "password123"
}
```

**Response 200**
```json
{
  "id": 1,
  "employeeCode": "EMP001",
  "name": "田中太郎",
  "email": "tanaka@example.com",
  "role": "EMPLOYEE",
  "teamName": "開発1チーム"
}
```

**Response 401** — 認証失敗

### POST /api/v1/auth/logout

ログアウト。セッション無効化。

**Response 200** — `{}`

### GET /api/v1/auth/me

ログイン中ユーザー情報取得

**Response 200** — ログイン時と同じ形式

---

## 打刻 API

### POST /api/v1/attendance/clock-in

出勤打刻

**Response 201**
```json
{
  "id": 1,
  "timeRecordId": 10,
  "clockIn": "2026-07-14T09:00:00",
  "clockOut": null
}
```

### POST /api/v1/attendance/clock-out

退勤打刻

**Response 200**
```json
{
  "id": 1,
  "timeRecordId": 10,
  "clockIn": "2026-07-14T09:00:00",
  "clockOut": "2026-07-14T18:00:00"
}
```

### GET /api/v1/attendance/today

本日の勤怠状態取得

**Response 200**
```json
{
  "workDate": "2026-07-14",
  "status": "DRAFT",
  "entries": [
    {
      "id": 1,
      "clockIn": "2026-07-14T09:00:00",
      "clockOut": "2026-07-14T12:00:00"
    },
    {
      "id": 2,
      "clockIn": "2026-07-14T13:00:00",
      "clockOut": null
    }
  ],
  "totalWorkMinutes": 180
}
```

### GET /api/v1/attendance/records?yearMonth=2026-07

月次の日別勤怠一覧

**Response 200**
```json
{
  "yearMonth": "2026-07",
  "records": [
    {
      "workDate": "2026-07-01",
      "status": "APPROVED",
      "totalWorkMinutes": 480,
      "overtimeMinutes": 0,
      "entries": [...]
    }
  ]
}
```

### PUT /api/v1/attendance/entries/{entryId}

打刻修正（DRAFT 状態のみ）

**Request**
```json
{
  "clockIn": "2026-07-14T09:15:00",
  "clockOut": "2026-07-14T18:30:00"
}
```

**Response 200** — 更新後の TimeEntry

**Response 409** — 承認済みで修正不可

### POST /api/v1/attendance/records/{recordId}/submit

承認申請（DRAFT → SUBMITTED）

**Response 200** — 更新後の TimeRecord

---

## 承認 API（管理者用）

### GET /api/v1/approval/pending

承認待ち一覧

**Response 200**
```json
{
  "items": [
    {
      "type": "TIME_RECORD",
      "id": 10,
      "employeeName": "田中太郎",
      "workDate": "2026-07-14",
      "submittedAt": "2026-07-14T18:30:00"
    },
    {
      "type": "LEAVE_REQUEST",
      "id": 5,
      "employeeName": "佐藤花子",
      "leaveType": "PAID",
      "startDate": "2026-07-20",
      "endDate": "2026-07-20"
    }
  ]
}
```

### POST /api/v1/approval/time-records/{recordId}/approve

打刻承認（SUBMITTED → APPROVED）

**Response 200**

### POST /api/v1/approval/leave-requests/{requestId}/approve

休暇承認

**Response 200**

### POST /api/v1/approval/leave-requests/{requestId}/reject

休暇却下

**Request**
```json
{
  "reason": "業務都合のため"
}
```

**Response 200**

---

## 休暇 API

### POST /api/v1/leaves

休暇申請

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
  "status": "PENDING"
}
```

### GET /api/v1/leaves?status=PENDING

自分の休暇申請一覧

**Response 200**
```json
{
  "items": [...]
}
```

### GET /api/v1/leaves/balance

有給残高照会

**Response 200**
```json
{
  "fiscalYear": 2026,
  "granted": 15.0,
  "used": 3.0,
  "remaining": 12.0
}
```

---

## 集計・レポート API

### GET /api/v1/reports/monthly?yearMonth=2026-07

自分の月次集計

**Response 200**
```json
{
  "yearMonth": "2026-07",
  "totalWorkMinutes": 9600,
  "overtimeMinutes": 1200,
  "nightOvertimeMinutes": 0,
  "paidLeaveDays": 1.0,
  "absentDays": 0,
  "workDays": 20
}
```

### GET /api/v1/reports/team?yearMonth=2026-07

チーム月次集計（管理者用）

**Response 200**
```json
{
  "yearMonth": "2026-07",
  "members": [
    {
      "employeeId": 1,
      "name": "田中太郎",
      "totalWorkMinutes": 9600,
      "overtimeMinutes": 1200,
      "overtimeAlert": false
    }
  ]
}
```

### GET /api/v1/reports/export?yearMonth=2026-07&departmentId=1

CSV エクスポート（人事用）

**Response 200** — Content-Type: text/csv

### GET /api/v1/reports/company?yearMonth=2026-07

全社月次集計（人事用）

**Response 200** — チーム集計と同形式（全社員分）

---

## アラート API

### GET /api/v1/alerts

自分宛のアラート一覧

**Response 200**
```json
{
  "items": [
    {
      "id": 1,
      "type": "OVERTIME_WARNING",
      "message": "今月の残業時間が45時間を超えています（現在: 48時間）",
      "createdAt": "2026-07-14T09:00:00",
      "read": false
    },
    {
      "id": 2,
      "type": "MISSING_CLOCK",
      "message": "2026-07-13の打刻がありません",
      "createdAt": "2026-07-14T09:00:00",
      "read": false
    }
  ]
}
```

### PUT /api/v1/alerts/{alertId}/read

既読にする

**Response 200**

---

## 社員管理 API（人事用）

### GET /api/v1/employees?teamId=1&active=true

社員一覧（フィルタ可能）

### POST /api/v1/employees

社員登録

**Request**
```json
{
  "employeeCode": "EMP100",
  "name": "新入社員",
  "email": "new@example.com",
  "password": "initial123",
  "role": "EMPLOYEE",
  "teamId": 1,
  "hireDate": "2026-07-01"
}
```

**Response 201**

### PUT /api/v1/employees/{id}

社員情報更新

### DELETE /api/v1/employees/{id}

社員無効化（論理削除: active=false）

---

## 組織管理 API（人事用）

### GET /api/v1/organizations/departments

部署一覧（部 > 課 > チーム の階層）

**Response 200**
```json
{
  "departments": [
    {
      "id": 1,
      "name": "開発部",
      "sections": [
        {
          "id": 1,
          "name": "第1開発課",
          "teams": [
            { "id": 1, "name": "開発1チーム", "managerName": "鈴木一郎" }
          ]
        }
      ]
    }
  ]
}
```

---

## 認可ルール

| エンドポイント | EMPLOYEE | MANAGER | HR |
|--------------|----------|---------|-----|
| 打刻 API | o | o | o |
| 自分の勤怠参照 | o | o | o |
| 承認 API | x | o | o |
| チーム集計 | x | o | o |
| 全社集計・CSV | x | x | o |
| 社員管理 | x | x | o |
| 組織管理 | x | x | o |
