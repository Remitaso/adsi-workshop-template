# Unit 1: 打刻・勤怠記録

> **Unit 0 完了後、Unit 2 と並行して実装可能**

## 目的

社員が出退勤を打刻し、日次の勤務時間が自動計算される機能を実装する。
打刻修正と承認フローも含む。

## 対応ユーザーストーリー

- US-01: 出勤打刻
- US-02: 退勤打刻
- US-03: 複数回出退勤（中抜け）
- US-04: 打刻修正（承認前は自由）
- US-05: 打刻承認
- US-06: 日次勤務時間の自動計算
- US-07: 8時間超の残業計算
- US-08: 深夜割増計算

## スコープ

### Backend

- Flyway: time_records, time_entries テーブル
- TimeRecord Entity + Repository
- TimeEntry Entity + Repository
- AttendanceService（打刻・修正・承認・時間計算）
- WorkDuration Value Object（通常/残業/深夜の分離計算）
- AttendanceController（打刻 API）
- ApprovalController（打刻承認 API）

### Frontend

- ダッシュボード画面（S-02）: 打刻ボタン + 本日の状態
- 月次勤怠一覧画面（S-03）: 日別の勤怠リスト
- 打刻修正画面（S-04）: エントリ修正フォーム
- 承認一覧画面の打刻タブ（S-07 一部）

## テーブル（Flyway マイグレーション）

- V5__create_time_records.sql
- V6__create_time_entries.sql

## API エンドポイント

- POST /api/v1/attendance/clock-in
- POST /api/v1/attendance/clock-out
- GET /api/v1/attendance/today
- GET /api/v1/attendance/records?yearMonth=YYYY-MM
- PUT /api/v1/attendance/entries/{entryId}
- POST /api/v1/attendance/records/{recordId}/submit
- POST /api/v1/approval/time-records/{recordId}/approve

## ドメインルール

- 出勤中（clockOut が null）の状態で再出勤はできない
- 退勤は出勤中の状態でのみ可能
- 打刻修正は status=DRAFT のときのみ
- 承認後（APPROVED）は修正不可
- 勤務時間計算:
  - 各 TimeEntry の clockIn〜clockOut を合算
  - 日 8 時間超 → 残業
  - 22:00〜5:00 → 深夜割増

## テスト

- AttendanceService: 出勤/退勤/中抜け/修正/承認の全パターン
- WorkDuration 計算: 通常勤務/残業/深夜/日跨ぎのケース
- Controller: 各エンドポイントの正常系・異常系
- 統合テスト: 出勤→退勤→修正→承認の一連フロー
- Frontend: ダッシュボード打刻ボタンのコンポーネントテスト

## 受け入れ基準

- [ ] 出勤→退勤で勤務時間が正しく計算される
- [ ] 中抜け（複数ペア）が正しく記録される
- [ ] DRAFT 状態で打刻修正ができる
- [ ] APPROVED 後は修正できない（409 エラー）
- [ ] 深夜帯の勤務が正しく区別される

## 依存

- Unit 0（共通基盤）

## 担当

担当者 A
