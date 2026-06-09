package com.turtlesafety.radar.parent

/** Parent Console 内の画面識別子。Navigation ライブラリは導入せず手書きで切り替える。 */
sealed interface Screen {
    data object Home : Screen
    data object Logs : Screen

    /** 仕様書 §5.7 の External Guard チェックリスト。 */
    data object Guard : Screen
    data object Settings : Screen
}

/** PIN 認証ゲートの状態。 */
enum class GateStatus {
    /** 初回起動: 利用説明と PIN 設定をまとめて行うオンボーディング画面。 */
    UNINITIALIZED,

    /** PIN 設定済みだが未認証: ロック画面。 */
    LOCKED,

    /** 認証済み: 通常画面表示可。 */
    UNLOCKED,
}
