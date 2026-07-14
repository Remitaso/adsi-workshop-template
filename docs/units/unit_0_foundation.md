# Unit 0: 共通基盤

> **最初に実装する Unit。後続の全 Unit がこの基盤に依存する。**

## 目的

プロジェクトのスケルトン・DB接続・認証・共通設定を整備し、
後続 Unit がすぐに TDD で機能実装に入れる状態を作る。

## スコープ

### Backend (Spring Boot)

- プロジェクト初期構成（Gradle, パッケージ構成）
- PostgreSQL 接続設定（application.yml, test用H2）
- Flyway 設定 + 組織マスタテーブル（departments, sections, teams, employees）
- Spring Security 設定（SecurityFilterChain, BCrypt, セッション認証）
- 認証 API（POST /login, POST /logout, GET /me）
- 共通エラーハンドリング（@RestControllerAdvice）
- Employee Entity + Repository + Service（CRUD）
- 組織 Entity（Department, Section, Team）+ Repository
- ArchUnit テスト（レイヤー依存チェック）
- テスト基盤（TestProfile, テスト用 application-test.yml）

### Frontend (Next.js)

- プロジェクト初期構成（Next.js + TypeScript + Tailwind）
- API クライアント基盤（fetch ラッパー, withBasePath）
- 認証コンテキスト（ログイン状態管理）
- ログイン画面（S-01）
- 共通レイアウト（Header, ナビゲーション）
- ロールベースルーティング（権限に応じたメニュー表示）

## テーブル（Flyway マイグレーション）

- V1__create_departments.sql
- V2__create_sections.sql
- V3__create_teams.sql
- V4__create_employees.sql

## API エンドポイント

- POST /api/v1/auth/login
- POST /api/v1/auth/logout
- GET /api/v1/auth/me
- GET /api/v1/employees（人事用）
- POST /api/v1/employees（人事用）
- PUT /api/v1/employees/{id}（人事用）
- DELETE /api/v1/employees/{id}（人事用）
- GET /api/v1/organizations/departments

## テスト

- Employee CRUD のユニットテスト（Service）
- Employee API の WebMvcTest（Controller）
- 認証フローの統合テスト（ログイン→認証→ログアウト）
- ArchUnit（レイヤー依存）
- Frontend: ログイン画面のコンポーネントテスト

## 受け入れ基準

- [ ] `./gradlew test` が全件グリーン
- [ ] ログインしてダッシュボード画面（空）が表示される
- [ ] 権限なしで管理APIにアクセスすると 403 が返る
- [ ] Flyway マイグレーションが正常に実行される

## 依存

なし（全 Unit の基盤）

## 担当

2人で協力して完了させる（後続 Unit のブロッカーになるため優先）
