package com.turtlesafety.radar.parent.format

import com.turtlesafety.radar.core.risk.DetectionSource
import com.turtlesafety.radar.core.risk.RiskCategory

/**
 * UI 表示用の日本語ラベル群。
 *
 * リスクエンジンや DB は enum 名 (英大文字) で値を扱うが、保護者向け画面では
 * 仕様書 §7 / §8 の日本語表記をそのまま見せる。
 */
object RiskLabels {

    /** 仕様書 §8 のスコア → 短い日本語ラベル。 */
    fun levelLabel(score: Int): String = when {
        score >= 5 -> "即時対応"
        score >= 4 -> "高リスク"
        score >= 3 -> "高め"
        score >= 2 -> "要確認"
        score >= 1 -> "注意"
        else -> "問題なし"
    }

    /** スコア → 一言の説明。 */
    fun levelSummary(score: Int): String = when {
        score >= 5 -> "今すぐ確認することを推奨します"
        score >= 4 -> "高リスクです。早めに状況を確認してください"
        score >= 3 -> "リスクが高めです。様子を見てください"
        score >= 2 -> "確認しておくと安心です"
        score >= 1 -> "ごく軽い注意です"
        else -> "問題は見つかりませんでした"
    }

    /** カテゴリの日本語名 (カンマ連結文字列を受け取る)。 */
    fun categoryLabels(categories: String): String =
        categories.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { runCatching { RiskCategory.valueOf(it) }.getOrNull() }
            .joinToString(separator = " / ") { categoryLabel(it) }

    fun categoryLabel(category: RiskCategory): String = when (category) {
        RiskCategory.CONTACT_EXCHANGE -> "連絡先交換"
        RiskCategory.SECRECY -> "秘密化"
        RiskCategory.MEETUP -> "会う約束"
        RiskCategory.SEXUAL_REQUEST -> "性的・自撮り要求"
        RiskCategory.COERCION -> "脅し・支配"
        RiskCategory.IDENTITY_UNKNOWN -> "身元不明"
    }

    /** ソース (enum 名文字列) の日本語名。SYSTEM など未知の値は素のまま返す。 */
    fun sourceLabel(rawSource: String): String =
        runCatching { sourceLabel(DetectionSource.valueOf(rawSource)) }
            .getOrDefault(rawSource)

    fun sourceLabel(source: DetectionSource): String = when (source) {
        DetectionSource.NOTIFICATION -> "通知"
        DetectionSource.IME -> "入力検査"
        DetectionSource.IMAGE -> "画像"
        DetectionSource.QR -> "QR コード"
        DetectionSource.SYSTEM -> "システム"
    }
}
