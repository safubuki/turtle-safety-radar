package com.turtlesafety.radar.guard

/**
 * External Guard チェックリストのカテゴリ (仕様書 v0.4 §6)。
 *
 * 本アプリは特定の見守りサービス (TONE / Family Link / MDM 等) と直接連携しない。
 * このリストは保護者が「外部の制限を運用しているか」を自己確認するためのもの。
 */
enum class ChecklistCategory(val title: String) {
    APP_RESTRICTIONS("§6.1 アプリ制限"),
    SETTINGS_LOCKDOWN("§6.2 設定変更対策"),
    COMMUNICATION("§6.3 連絡手段対策"),
    LOCATION_AND_OUTING("§6.4 位置・外出対策"),
}

/**
 * チェックリスト 1 項目の定義。
 *
 * @property id ストレージで使う安定 ID (将来項目順を変えても壊れないように)。
 * @property category §6.x の区分。
 * @property title 保護者向け表示文。
 * @property hint 必要に応じた補足。
 */
data class ChecklistItem(
    val id: String,
    val category: ChecklistCategory,
    val title: String,
    val hint: String? = null,
)

/**
 * チェック状態。
 */
data class ChecklistState(
    val checked: Boolean,
    val updatedAtMillis: Long,
    val note: String? = null,
)
