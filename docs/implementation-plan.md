# 実装計画 (v0.4 仕様に基づく)

仕様: [spec-v0.4.md](spec-v0.4.md)

## 0. ゴール

家庭内利用の単一APKとして、子どもの安全リスク兆候を検知する Android アプリ
「Turtle Safety Radar」の MVP を段階的に実装する。

## 1. 技術選定

| 項目 | 選定 | 理由 |
|---|---|---|
| 言語 | Kotlin | Android 標準・最新言語機能 |
| ビルド | Gradle (Kotlin DSL) + Version Catalog | 仕様 4 章のマルチモジュール構成に適合 |
| 最低 SDK | API 26 (Android 8.0) | NotificationListenerService の安定動作下限 |
| ターゲット SDK | API 34 (Android 14) | 通知/前景サービス制約への現実的対応 |
| UI | Jetpack Compose | 親コンソール / IME 候補ビュー双方で利用可 |
| DB | Room | ローカルログDB |
| DI | 手書きの軽量 ServiceLocator (MVPは小規模) | Hilt は将来検討 |
| ローカルAI | Level 0 (ルールベース) のみ MVP | LLM/分類器は仕様 5.5 で後回し |
| 暗号化 | EncryptedSharedPreferences (PIN等) | Tinkベース、Android標準 |
| 配布 | サイドロード APK | 仕様 3.1 |

JDK: 17 を使用 (Android Gradle Plugin 8.x で標準)。
Java 21 がインストール済みだが、AGP の安定動作のため `JAVA_HOME` 切替を後で確認する。

## 2. プロジェクト構造

```text
turtle-safety-radar/
├── build.gradle.kts            (root, plugin alias宣言)
├── settings.gradle.kts         (モジュール登録 + repositories)
├── gradle.properties           (jvmargs, AndroidX, Kotlin)
├── gradle/libs.versions.toml   (バージョン一元管理)
├── gradle/wrapper/             (gradle-wrapper.properties)
├── gradlew / gradlew.bat
├── docs/
│   ├── spec-v0.4.md
│   └── implementation-plan.md  ← 本書
├── apps/
│   └── app/                    (com.turtlesafety.radar — エントリAPK)
├── platform/
│   └── core/                   (リスク判定エンジン, ログDB, PIN, 設定)
└── features/
	├── notification-monitor/   (NotificationListenerService)
	├── safety-ime/             (InputMethodService)
	├── media-checker/          (QR/SNS ID 検査)
	├── local-ai/               (将来用ファサード、初期はno-op)
	├── parent-console/         (保護者向け Compose 画面群)
	└── external-guard/         (外部制限チェックリスト)
```

### モジュール依存方針

```text
app  ─→ parent-console, notification-monitor, safety-ime, media-checker, external-guard
全モジュール ─→ core
local-ai ←── (safety-ime, notification-monitor, media-checker) ※ファサード越し
```

`core` は他モジュールに依存しない。`app` は接着剤。

## 3. パッケージ命名

ベース: `com.turtlesafety.radar`

| モジュール | パッケージ |
|---|---|
| core | `com.turtlesafety.radar.core` |
| notification-monitor | `com.turtlesafety.radar.notif` |
| safety-ime | `com.turtlesafety.radar.ime` |
| media-checker | `com.turtlesafety.radar.media` |
| local-ai | `com.turtlesafety.radar.ai` |
| parent-console | `com.turtlesafety.radar.parent` |
| external-guard | `com.turtlesafety.radar.guard` |
| app | `com.turtlesafety.radar` |

## 4. フェーズ分割

### Phase 0: 基盤 ✅ (commit e517732)

- [x] git 初期化 + .gitignore
- [x] 仕様書 / 計画書ドキュメント
- [x] Gradle multi-module 雛形 (settings/build/wrapper/libs.versions.toml)
- [x] 各モジュールの空シェル (build.gradle.kts + AndroidManifest.xml + パッケージディレクトリ)
- [x] app モジュール (MainActivity, RadarApplication, テーマ)

完了条件: Android Studio で開いて Gradle Sync が通る状態。

### Phase 1: Core MVP ✅ (commit f9c0349)

- [x] 管理者 PIN: EncryptedSharedPreferences + PBKDF2-HMAC-SHA256, 定数時間比較
- [x] 設定管理: Sensitivity 列挙 + 監視対象アプリ Set
- [x] ログDB (Room): `DetectionLog` + DAO + Repository + 保持期間別の削除
- [x] リスク判定エンジン: ルールベース判定、カテゴリ上限 + 組み合わせボーナス
- [x] 危険カテゴリ辞書: §7.1〜§7.6 のキーワード weight 付き
- [x] CoreServices ServiceLocator + Radar 単一エントリ
- [x] 12 ユニットテスト (リスクエンジン 11 + リテンション 1)

### Phase 2: Notification Monitor ✅ (commit d36e351)

- [x] `NotificationListenerService` (RadarNotificationListenerService)
- [x] NotificationProcessor (Android 非依存・テスト可能)
- [x] 監視対象アプリのフィルタ (lambda 注入で疎結合)
- [x] NotificationListenerPermission (有効化状態 + 設定 Intent)
- [x] 4 ユニットテスト

### Phase 3: Safety IME ✅ (commit f9be709)

MVP は「チェック専用 IME」として実装 (仕様 §12 の「完全強制ではない」方針)。

- [x] InputMethodService (SafetyImeService) + Compose 不使用の軽量パネル
- [x] PreSendChecker (Android 非依存) + WarningLevel
- [x] SafetyImePermission
- [x] manifest 登録 + xml/method.xml
- [x] 4 ユニットテスト

将来: フル日本語 IME 対応は Phase 7+ で検討。

### Phase 4: External Guard

- [x] ChecklistDefinitions: §6 の 21 項目を安定 ID で 4 カテゴリで保持
- [x] ChecklistRepository (SharedPreferences 実装) + 進捗集計
- [x] ExternalGuardScreen (Compose): カテゴリ別カード + 進捗バー
- [x] ボトムナビへ「チェック」タブとして組み込み
- [ ] ユニットテスト (後続フェーズで追加予定)

実装場所: `features/parent-console/.../guard/` および `screens/ExternalGuardScreen.kt`
(初期実装ではモジュール分割せず parent-console 内に同居)。

### Phase 5: Parent Console ✅ (commit b4ce03a)

- [x] PIN セットアップ / ゲート (Compose Material3)
- [x] ホーム (権限状態カード × 通知監視 / Safety IME / ログ件数 / チェック進捗)
- [x] ログ一覧 (スコア色分け + 確認済マーキング + 全削除)
- [x] 設定 (Sensitivity FilterChip + 監視対象アプリ Switch)
- [x] External Guard チェックリスト画面 (カテゴリ別 + 進捗バー)
- [x] Bottom NavigationBar + Lock ボタン

未着手 (後回し): ログ CSV/JSON エクスポート、オンボーディングウィザード。

### Phase 6: 統合と権限再診断

- [x] PermissionStateMonitor: 通知監視 / IME の取り消し検知 → SYSTEM ログ
- [x] Application 起動時の診断 (リテンション適用も)
- [x] Parent Console の権限再診断ボタンと連動
- [x] インストール直後のオンボーディングフロー (仕様 14.1, PIN 設定前にウェルカム + プライバシー方針案内を前置)

### Phase 7: 製品品質向上 (今フェーズ)

- [x] 保護者コンソール全画面の UI 刷新 (Material 3 アイコン / 視認性向上 / 日本語ラベル統一)
- [x] ログ画面のリスクレベル日本語表示、ソース別アイコン、フィルタチップ、空状態
- [x] 設定画面に監視対象アプリの追加 / 削除 UI、データ保持期間表示、プライバシー方針セクション
- [x] ホーム画面のセルフテスト / Quick Check を「開発者向け診断」セクションに格納
- [x] クラッシュ画面の家庭ユーザー向け文言調整 (技術詳細は折り畳み)

### Phase 8 以降 (今後の拡張候補)

- Media Checker のさらなる強化 (リアルタイム監視、信頼度調整)
- Local AI (Level 1 軽量分類器、Level 2 LLM)
- 親アプリへの Push 通知 (FCM 不使用なら LAN 経由など別途検討)
- 子ども側警告 UI の強化 (仕様 §10、IME 以外のタイミングでの警告表示)
- Google Play 公開分割案 (仕様 15 章)

## 5. リスクと未確定事項

| 項目 | 内容 | 対応 |
|---|---|---|
| Notification本文 | プレビュー非表示時は取得不可 | 仕様5.2記載済、ログには「通知発生のみ」を残す |
| 独自IME強制 | OS仕様上単一APKでは完全強制不可 | 仕様12章の「検知+警告+外部制限」方針を踏襲 |
| プライバシー | 入力全文を保存しない原則 | リスクエンジンの中で抜粋窓 (例: 検知トリガ語の前後20文字) を切り出す |
| 開発機 | Android SDK 未導入 | 雛形は SDK 無しで作成、Phase1終盤までに Android Studio 導入 |
| キーストア | 同一署名キーでの更新が前提 (仕様14.2) | Phase 6 で debug.keystore とは別の運用キーを作成 |

## 6. テスト方針

- Core (リスクエンジン・PIN・ログDB) はユニットテストを最重視
- 仕様 17 章の MVP 受け入れ条件を、各 Phase 完了時にチェックリストで確認
- Notification / IME は実機テスト中心 (Robolectric は補助)

## 7. 進行ルール

- 各 Phase の終わりにコミット
- 仕様変更が出たら `spec-v0.4.md` ではなく `spec-v0.5.md` を起こす (差分追跡しやすく)
- プライバシーに関わる方針変更は仕様書とコードの両方に反映
