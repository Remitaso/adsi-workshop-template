# Unit 3: 集計・レポート・アラート

> **Unit 1 + Unit 2 完了後に実装**（打刻データと休暇データの両方を集計するため）

## 目的

月次勤怠集計・CSV エクスポート・残業アラート・打刻忘れ通知を実装する。
管理者ダッシュボードも含む。

## 対応ユーザーストーリー

- US-09: 週40時間の所定労働時間管理
- US-14: 月次勤怠集計
- US-15: CSV エクスポート
- US-16: チームダッシュボード
- US-17: 月45時間残業アラート
- US-18: 打刻忘れ通知

## スコープ

### Backend

- Flyway: monthly_attendances テーブル、notifications テーブル（アラート用）
- MonthlyAttendance Entity + Repository
- AttendanceSummaryService（月次集計・CSV生成）
- NotificationService（残業アラート・打刻忘れ通知）
- ReportController（集計・エクスポート API）
- AlertController（アラート API）
- スケジューラ（打刻忘れチェック — @Scheduled）

### Frontend

- チームダッシュボード画面（S-08）: チーム勤怠一覧
- 月次レポート画面（S-09）: 全社集計 + CSV ダウンロード
- アラート表示（Header 内通知バッジ + 一覧）

## テーブル（Flyway マイグレーション）

- V9__create_monthly_attendances.sql
- V10__create_notifications.sql

## API エンドポイント

- GET /api/v1/reports/monthly?yearMonth=YYYY-MM
- GET /api/v1/reports/team?yearMonth=YYYY-MM
- GET /api/v1/reports/company?yearMonth=YYYY-MM
- GET /api/v1/reports/export?yearMonth=YYYY-MM&departmentId=1
- GET /api/v1/alerts
- PUT /api/v1/alerts/{alertId}/read

## ドメインルール

- 月次集計: 対象月の全 TimeRecord + LeaveRequest から算出
- 残業判定: 月合計から所定労働時間（週40h × 週数）を超過した分
- 残業アラート: 月45時間超で本人 + 管理者に通知
- 打刻忘れ: 営業日に TimeRecord がない場合、翌営業日に通知
- CSV フォーマット: 社員コード, 氏名, 部署, 総労働時間, 残業時間, 深夜時間, 有給日数, 欠勤日数

## テスト

- AttendanceSummaryService: 月次集計計算（通常/残業/深夜混在ケース）
- NotificationService: アラート生成条件のテスト
- CSV 出力: フォーマット・文字コード確認
- Controller: 権限チェック（人事のみ全社閲覧可）
- Frontend: ダッシュボードのデータ表示テスト

## 受け入れ基準

- [ ] 月次集計が正しく計算される（労働/残業/深夜/休暇）
- [ ] CSV エクスポートが正しいフォーマットでダウンロードできる
- [ ] 月45時間超で残業アラートが生成される
- [ ] 打刻忘れ通知が翌営業日に生成される
- [ ] チームダッシュボードでメンバーの状況が一覧できる

## 依存

- Unit 0（共通基盤）
- Unit 1（打刻データ）
- Unit 2（休暇データ）

## 担当

2人で分担（Backend: A、Frontend: B など）
