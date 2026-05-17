# Turtle Safety Radar

子どものスマートフォン利用で起こりやすい危険なやり取りの兆候を、端末内で検知する Android アプリです。

通知、入力文、画像内テキストや QR コードを対象に、連絡先交換、秘密化、別 SNS 誘導、会う約束、性的要求などのリスクを見つけ、必要に応じて保護者が確認できるようにします。

Google Play 配布を前提にした一般向けアプリではなく、まずは家庭内利用を想定した単一 APK として実装しています。

## このアプリでできること

- 通知内容を見て、危険な会話の兆候を検知する
- Safety IME で、送信前の文章を手動チェックする
- 画像やスクリーンショットから QR、SNS 誘導、連絡先らしき情報を検査する
- 保護者向け画面で PIN 管理、権限状態確認、ログ閲覧、感度変更を行う
- 外部ペアレンタル設定の確認チェックリストを管理する
- 高リスクのイベントを端末内ログとして最小限保存する

## 現在の実装状況

現時点で、次の機能は実装済みです。

- Parent Console
  保護者用 PIN、ホーム、ログ一覧、設定、External Guard チェック画面
- Notification Monitor
  監視対象アプリの通知を解析し、リスク判定を実行
- Safety IME
  送信前チェック用の軽量 IME パネル
- Media Checker
  画像選択による OCR と QR 解析
- Local AI
  ルールベース判定に加える軽量な文脈補正モード
- Core
  リスク判定、設定管理、PIN、Room ログ DB、保持期間制御

## 特徴

### 1. 端末内で一次判定する

会話や画像の解析は、まず端末内で実行します。
常時クラウドに送る前提ではありません。

### 2. 全文保存を前提にしない

このアプリはキーロガーではありません。
すべての入力履歴や全チャット履歴を保存する設計にはしていません。

### 3. 高リスク時だけ最小限の証跡を残す

ログにはスコア、カテゴリ、短い抜粋、発生元などの最小限情報を残します。
画像そのものや全文保存は避ける方針です。

### 4. 家庭内運用をしやすくする

保護者が初期設定しやすいように、ホーム画面で権限状態、Safety IME、通知監視、Media Checker、Local AI、ログ件数をまとめて確認できます。

## 想定ユースケース

- LINE や SNS の通知から、危険な誘導や秘密化の兆候を早めに知りたい
- 子どもがメッセージ送信前に、自分で危険度を確認できるようにしたい
- 画像で送られてきた QR や SNS ID を手元で確認したい
- 完全な利用制限だけでなく、兆候検知と会話のきっかけを作りたい

## 画面構成

アプリを起動すると、まず保護者用 PIN のセットアップまたは解除画面が表示されます。
解除後は次の 4 画面を使えます。

- ホーム
  権限状態、初期セットアップ進捗、Safety IME、通知監視、保護者通知、Media Checker、Local AI、ログ件数を確認
- ログ
  検知ログの一覧表示、確認済み化、共有、全削除
- チェック
  External Guard の確認項目を管理
- 設定
  感度、監視対象アプリ、Media Checker、Local AI を変更

## 既定の監視対象アプリ

初期状態では、次のアプリが通知監視の対象です。

- LINE
- Gmail
- Google Messages
- Discord
- Instagram
- X
- TikTok

必要に応じて、設定画面からパッケージ名を直接追加できます。

## Safety IME について

Safety IME は、一般的な日本語フルキーボードではありません。
現在は「送信前チェック用パネル」として動作します。

- 通常の IME で入力したあとに Safety IME へ切り替える
- リスクチェックを押す
- 現在の入力内容を安全判定する
- 警告レベルとカテゴリ、短い抜粋を表示する

また、次のような入力欄は検査対象外です。

- パスワード欄
- 認証コード欄

## Media Checker について

Media Checker は、画像やスクリーンショットを選んで検査する機能です。

- QR コード
- OCR で読めたテキスト
- SNS 誘導文
- 連絡先らしき文字列

を組み合わせてスコアリングします。

初期版では、画像自体を保存せず、検査結果のみを扱います。

## Local AI について

Local AI は、ルールベース判定を補助するモードです。

- Basic
  ルールベースのみ
- Standard
  軽量な文脈補正を追加
- Local LLM
  将来拡張用。現状は Standard 相当へフォールバック
- Cloud Assist
  将来拡張用。現状は Standard 相当へフォールバック

現時点では Standard が実用上の推奨モードです。

## プロジェクト構成

ルート直下は、役割ごとに整理しています。

```text
turtle-safety-radar/
├── apps/
│   └── app/                    Android アプリ本体
├── platform/
│   └── core/                   共通基盤、設定、DB、リスク判定
├── features/
│   ├── notification-monitor/   通知監視
│   ├── safety-ime/             送信前チェック IME
│   ├── media-checker/          画像検査
│   ├── local-ai/               Local AI 表示と補助機能
│   ├── parent-console/         保護者向け UI
│   └── external-guard/         外部制限チェックリスト
├── docs/                       仕様書、実装計画
├── gradle/                     Gradle wrapper / versions catalog
├── build.gradle.kts            ルート Gradle 設定
├── settings.gradle.kts         モジュール定義
└── README.md
```

## 開発環境

推奨環境は次の通りです。

- Android Studio 最新安定版
- JDK 17 以上
- Android SDK Platform 34
- Android Build Tools 34.0.0

このリポジトリは Gradle wrapper を含んでいるため、グローバル Gradle は不要です。

## セットアップ

### 1. リポジトリを取得する

```powershell
git clone https://github.com/safubuki/turtle-safety-radar.git
cd turtle-safety-radar
```

### 2. Android SDK の場所を設定する

ルートに local.properties を用意し、Android SDK へのパスを設定します。

例:

```properties
sdk.dir=C:\\Users\\yourname\\AppData\\Local\\Android\\Sdk
```

macOS / Linux の例:

```properties
sdk.dir=/Users/yourname/Library/Android/sdk
```

### 3. Android Studio で開く

ルートフォルダをそのまま Android Studio で開けば、Gradle Sync でモジュールが認識されます。

## ビルド方法

### Windows

```powershell
.\gradlew.bat :app:assembleDebug
```

### macOS / Linux

```bash
./gradlew :app:assembleDebug
```

ビルド成功後の APK は次に出力されます。

- apps/app/build/outputs/apk/debug/app-debug.apk

## テスト実行

### 全ユニットテスト

Windows:

```powershell
.\gradlew.bat testDebugUnitTest
```

macOS / Linux:

```bash
./gradlew testDebugUnitTest
```

### モジュール単位のテスト例

```powershell
.\gradlew.bat :safety-ime:testDebugUnitTest
.\gradlew.bat :media-checker:testDebugUnitTest
```

## アプリの始め方

1. debug APK を端末へインストールする
2. アプリを起動し、保護者用 PIN を設定する
3. ホーム画面で通知アクセスを許可する
4. Safety IME を有効化する
5. 必要なら Safety IME を標準入力方式に設定する
6. Android 13 以降では通知権限を許可する
7. 設定画面で感度、監視対象アプリ、Media Checker、Local AI を調整する

## よく使う運用フロー

### 通知監視を使う

- ホーム画面で通知アクセス設定を開く
- 対象アプリから届く通知を自動で判定する
- 高リスクのものはログ画面で確認する

### Safety IME を使う

- 入力中に Safety IME へ切り替える
- リスクチェックを押す
- 警告レベルと詳細を確認する

### Media Checker を使う

- 設定画面で Media Checker を有効にする
- 画像を選んで検査を押す
- OCR 抜粋、QR 件数、検出シグナル、スコアを確認する

## 制約と注意点

- 通知本文は、端末や対象アプリの通知表示設定によって取得できないことがあります
- Safety IME は現状フルキーボードではなく、チェック専用 IME です
- Local LLM と Cloud Assist は UI とモード選択まで実装済みですが、実際の高度モデル統合は今後の拡張です
- Media Checker は現在、手動で画像を選択して検査する形です
- 本アプリ単体で SNS やブラウザの完全制限を行うものではありません。必要に応じて Family Link や端末制限と併用してください

## プライバシー方針の要点

- 入力全文の保存を前提にしない
- パスワードや認証コードを保存しない
- 画像そのものを保存しない方針
- 高リスク時に限って最小限のログを残す

## 関連ドキュメント

- docs/spec-v0.4.md
- docs/implementation-plan.md

## 今後の拡張候補

- Local LLM / Cloud Assist の本実装
- Safety IME の入力体験強化
- インストール直後のオンボーディング
- 実機ベースの権限導線と運用改善
