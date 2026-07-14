# Unit 2: 休暇管理

> **Unit 0 完了後、Unit 1 と並行して実装可能**

## 目的

社員が休暇を申請し、管理者が承認/却下する機能を実装する。
有給残高の自動管理も含む。

## 対応ユーザーストーリー

- US-10: 休暇申請（種類選択）
- US-11: 休暇承認/却下
- US-12: 有給休暇の勤続年数に応じた自動付与
- US-13: 有給残日数確認

## スコープ

### Backend

- Flyway: leave_requests, leave_balances テーブル
- LeaveRequest Entity + Repository
- LeaveBalance Entity + Repository
- LeaveService（申請・承認・却下・残高管理・年次付与）
- LeaveController（休暇 API）
- ApprovalController（休暇承認 API — Unit 1 と共有）

### Frontend

- 休暇申請画面（S-05）: 種類・期間・理由入力
- 休暇一覧画面（S-06）: 申請履歴 + 有給残高表示
- 承認一覧画面の休暇タブ（S-07 一部）

## テーブル（Flyway マイグレーション）

- V7__create_leave_requests.sql
- V8__create_leave_balances.sql

## API エンドポイント

- POST /api/v1/leaves
- GET /api/v1/leaves?status=PENDING
- GET /api/v1/leaves/balance
- POST /api/v1/approval/leave-requests/{requestId}/approve
- POST /api/v1/approval/leave-requests/{requestId}/reject

## ドメインルール

- 有給申請時に残日数チェック（不足時はエラー）
- 半休は 0.5 日消費
- 承認で有給残高が減算される
- 却下時は残高変動なし
- 有給付与ルール（労基法準拠）:
  - 入社6ヶ月後: 10日
  - 1年6ヶ月: 11日
  - 2年6ヶ月: 12日
  - 3年6ヶ月: 14日
  - 4年6ヶ月: 16日
  - 5年6ヶ月: 18日
  - 6年6ヶ月以上: 20日

## テスト

- LeaveService: 申請/承認/却下/残高チェック/付与ルール
- 有給付与計算: 勤続年数ごとの付与日数テスト
- Controller: 各エンドポイントの正常系・異常系
- 統合テスト: 申請→承認→残高減算の一連フロー
- Frontend: 休暇申請フォームのバリデーションテスト

## 受け入れ基準

- [ ] 休暇種類を選択して申請できる
- [ ] 有給残日数が不足していると申請エラーになる
- [ ] 管理者が承認すると残高が減算される
- [ ] 却下時は残高が変わらない
- [ ] 勤続年数に応じて正しい日数が付与される

## 依存

- Unit 0（共通基盤）
- Unit 1 とは**独立**（並行実装可能）

## 担当

担当者 B
