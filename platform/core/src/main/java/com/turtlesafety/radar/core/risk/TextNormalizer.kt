package com.turtlesafety.radar.core.risk

import java.text.Normalizer

/**
 * 日本語混在テキストのリスク判定向け正規化。
 *
 * - NFKC 正規化: 全角英数→半角、半角カナ→全角カナ など。
 * - 小文字化: 英字の表記ゆれ吸収。
 *
 * カタカナ/ひらがな統一は意図的に行わない: 「インスタ」と「いんすた」は別語として
 * 辞書側で揃える方が誤検知制御しやすい。
 */
internal object TextNormalizer {
    fun normalize(input: String): String =
        Normalizer.normalize(input, Normalizer.Form.NFKC).lowercase()
}
