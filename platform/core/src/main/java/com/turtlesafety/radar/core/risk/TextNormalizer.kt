package com.turtlesafety.radar.core.risk

import java.text.Normalizer

/**
 * 日本語混在テキストのリスク判定向け正規化。
 *
 * 適用順:
 * 1. NFKC: 全角英数→半角、半角カナ→全角カナなど。
 * 2. lowercase: 英字の表記ゆれ吸収。
 * 3. カタカナ→ひらがな統一: "インスタ" と "いんすた" を同じパターンで拾えるようにする。
 * 4. 連続空白/全角空白の縮約。
 *
 * 結果:辞書側の登録は ひらがな + 小文字英数 で済み、キーワード重複を減らせる。
 */
internal object TextNormalizer {
    fun normalize(input: String): String {
        val nfkc = Normalizer.normalize(input, Normalizer.Form.NFKC).lowercase()
        val toHira = StringBuilder(nfkc.length)
        for (ch in nfkc) {
            // 全角カタカナ→ひらがな (U+30A1..U+30F6 を -0x60)
            val converted = if (ch in 'ァ'..'ヶ') {
                (ch.code - 0x60).toChar()
            } else {
                ch
            }
            toHira.append(converted)
        }
        return toHira.toString()
            .replace('　', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
