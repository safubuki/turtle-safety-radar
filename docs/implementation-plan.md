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
├── app/                        (com.turtlesafety.radar — エントリAPK)
├── core/                       (リスク判定エンジン, ログDB, PIN, 設定)
├── notification-monitor/       (NotificationListenerService)
├── safety-ime/                 (InputMethodService)
├── media-checker/              (QR/SNS ID 検査)
├── local-ai/                   (将来用ファサード、初期はno-op)
├── parent-console/             (保護者向け Compose 画面群)
└── external-guard/             (外部制限チェックリスト)
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

### Phase 0: 基盤 (本セッションで完了予定)

- [x] git 初期化 + .gitignore
- [x] 仕様書 / 計画書ドキュメント
- [ ] Gradle multi-module 雛形 (settings/build/wrapper/libs.versions.toml)
- [ ] 各モジュールの空シェル (build.gradle.kts + AndroidManifest.xml + パッケージディレクトリ)
- [ ] app モジュール (MainActivity, RadarApplication, テーマ)

完了条件: Android Studio で開いて Gradle Sync が通る状態。

### Phase 1: Core MVP

- 管理者 PIN: EncryptedSharedPreferences で保存、設定・検証
- 設定管理: 監視対象アプリ、検知感度
- ログDB (Room): エンティティ `DetectionLog`, DAO, 保持期間別の自動削除
- リスク判定エンジン: ルールベース判定 → スコアリング (仕様 7-8 章)
- 危険カテゴリ辞書: 仕様 7.1〜7.6 のキーワードリスト

完了条件: ユニットテストで「会う約束 + 秘密化」が高スコア化されること。

### Phase 2: Notification Monitor

- `NotificationListenerService` の実装
- `core` のリスクエンジンへ通知本文を渡す
- 高リスクイベントを `DetectionLog` へ書き込み
- 監視対象アプリのフィルタ
- 通知監視権限の解除検知 (権限再起動時に検出)

完了条件: 開発機の LINE/SMS 通知で `DetectionLog` にエントリが入ること。

### Phase 3: Safety IME

- `InputMethodService` 派生のスケルトン (シンプルな日本語入力は OS の IME を使わせない方針上、最低限のキーパッドを Compose で実装する)
- 入力前検査: 確定タイミングで現在の文字列をリスクエンジンへ
- 子ども側警告ダイアログ (仕様 10 章)
- 高リスク時のみ最小限抜粋を `DetectionLog` に保存

注: 日本語IMEとしての完成度は MVP では追わない。プロダクション化は別フェーズ。

完了条件: 設定アプリで Safety IME を有効化でき、危険語句入力で警告ダイアログが出ること。

### Phase 4: External Guard

- 仕様 6 章のチェックリストを定義データとして保持
- 親コンソールから表示・確認状態を保存
- TONE / Family Link / MDM 等の文言は等位扱い (依存しない)

完了条件: 親コンソールでチェックリスト全項目を確認・更新できること。

### Phase 5: Parent Console

- PIN 入力画面
- ホーム (権限状態 / IME 状態 / 通知監視状態のサマリ)
- ログ一覧 (フィルタ: 日付・カテゴリ・リスクスコア)
- 設定 (監視対象アプリ、検知感度)
- External Guard チェックリスト画面
- ログ削除・エクスポート (CSV か JSON)

完了条件: PIN を知らない子どもは Parent Console に入れないこと。

### Phase 6: 統合と権限再診断

- 起動時に必要権限の有無を診断
- 通知監視/IME が無効化された場合の検知ロジック
- インストール直後のオンボーディングフロー (仕様 14.1)

### Phase 7 以降 (MVP 後回し)

- Media Checker (QR/SNS ID画像検査)
- Local AI (Level 1 軽量分類器、Level 2 LLM)
- 親アプリへの Push 通知 (FCM 不使用なら LAN 経由など別途検討)
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
