package com.turtlesafety.radar.core.log

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 仕様書 v0.4 §11.1 で定義される検知ログ。
 *
 * - 子どもの全文は保存しない: [excerpt] はリスクエンジンが切り出した短い抜粋のみ。
 * - [categories] は [com.turtlesafety.radar.core.risk.RiskCategory] の name をカンマ連結したもの。
 *   分類 enum 自体は Room の TypeConverter で扱わず、純粋な文字列として保持して
 *   モジュール境界を越えても扱いやすくする。
 */
@Entity(
    tableName = "detection_log",
    indices = [
        Index("timestamp"),
        Index("score"),
    ],
)
data class DetectionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    /** [com.turtlesafety.radar.core.risk.DetectionSource] の name。 */
    val source: String,
    /** 検知元アプリのパッケージ名 (通知由来時のみ)。 */
    val appName: String?,
    /** "MEETUP,SECRECY" のようなカテゴリ name のカンマ連結。 */
    val categories: String,
    val score: Int,
    /** 抜粋 (前後 ~20 文字)。null の場合あり (例: 通知本文取得不可)。 */
    val excerpt: String?,
    /** 判定理由 (デバッグ・親への説明用)。 */
    val reason: String,
    /** 保護者が確認済みかどうか。 */
    val acknowledged: Boolean = false,
)
