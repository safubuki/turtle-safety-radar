package com.turtlesafety.radar.parent

/** Parent Console 内の画面識別子。Navigation ライブラリは導入せず手書きで切り替える。 */
sealed interface Screen {
    data object Home : Screen
    data object Logs : Screen
    data object Settings : Screen
    data object Checklist : Screen
}

/** PIN 認証ゲートの状態。 */
enum class GateStatus {
    /** PIN 未設定: 初回セットアップ画面。 */
    UNINITIALIZED,

    /** PIN 設定済みだが未認証: ロック画面。 */
    LOCKED,

    /** 認証済み: 通常画面表示可。 */
    UNLOCKED,
}
