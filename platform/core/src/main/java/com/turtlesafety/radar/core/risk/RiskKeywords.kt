package com.turtlesafety.radar.core.risk

/**
 * リスク判定用キーワード辞書 (仕様書 v0.4 §7)。
 *
 * weight は単一マッチでの寄与度:
 * - 1: 単独では誤検知が多い弱シグナル (§8.1 の例: 会おう/駅/インスタ/写真/ID/DM)
 * - 2: 中シグナル
 * - 3: 単独でも強い (典型的に脅し・性的要求・明示的な誘導)
 *
 * 全パターンは [TextNormalizer] による正規化(NFKC + 小文字化 + カタカナ→ひらがな)
 * を経由するため、辞書側はひらがなと小文字英数で書く。
 *
 * 1カテゴリの合算スコアは [RuleBasedRiskEngine.PER_CATEGORY_CAP] でクランプされ、
 * 仕様書 §8.2 の組み合わせはエンジン側でボーナス加算する。
 */
internal data class RiskKeyword(
    val pattern: String,
    val category: RiskCategory,
    val weight: Int,
)

internal object RiskKeywords {

    private const val W_WEAK = 1
    private const val W_MEDIUM = 2
    private const val W_STRONG = 3

    val all: List<RiskKeyword> = buildList {
        // §7.1 連絡先交換 ----------------------------------------------------
        add(RiskKeyword("qr送って", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("qrコード", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("id教えて", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("id交換", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("ライン教えて", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("line教えて", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("いんすた教えて", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("いんすた", RiskCategory.CONTACT_EXCHANGE, W_WEAK))
        add(RiskKeyword("instagram", RiskCategory.CONTACT_EXCHANGE, W_WEAK))
        add(RiskKeyword("ディスコ", RiskCategory.CONTACT_EXCHANGE, W_WEAK))
        add(RiskKeyword("でぃすこ", RiskCategory.CONTACT_EXCHANGE, W_WEAK))
        add(RiskKeyword("discord来て", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("discord", RiskCategory.CONTACT_EXCHANGE, W_WEAK))
        add(RiskKeyword("dmしよう", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("dmして", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("dm", RiskCategory.CONTACT_EXCHANGE, W_WEAK))
        add(RiskKeyword("個ちゃ", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("個チャ", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("電話番号教えて", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("連絡先教えて", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("line追加", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("ライン追加", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("ライン交換", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("らいん交換", RiskCategory.CONTACT_EXCHANGE, W_MEDIUM))
        add(RiskKeyword("ここじゃなくて別で話", RiskCategory.CONTACT_EXCHANGE, W_STRONG))
        add(RiskKeyword("別のアプリで話", RiskCategory.CONTACT_EXCHANGE, W_STRONG))
        add(RiskKeyword("別アプリで話", RiskCategory.CONTACT_EXCHANGE, W_STRONG))

        // §7.2 秘密化 --------------------------------------------------------
        add(RiskKeyword("親に言わないで", RiskCategory.SECRECY, W_STRONG))
        add(RiskKeyword("親には言わ", RiskCategory.SECRECY, W_STRONG))
        add(RiskKeyword("親には内緒", RiskCategory.SECRECY, W_STRONG))
        add(RiskKeyword("内緒", RiskCategory.SECRECY, W_MEDIUM))
        add(RiskKeyword("ないしょ", RiskCategory.SECRECY, W_MEDIUM))
        add(RiskKeyword("履歴消して", RiskCategory.SECRECY, W_STRONG))
        add(RiskKeyword("履歴を消", RiskCategory.SECRECY, W_STRONG))
        add(RiskKeyword("通知切って", RiskCategory.SECRECY, W_STRONG))
        add(RiskKeyword("ばれないように", RiskCategory.SECRECY, W_MEDIUM))
        add(RiskKeyword("ばれない", RiskCategory.SECRECY, W_WEAK))
        add(RiskKeyword("家族には言わ", RiskCategory.SECRECY, W_STRONG))
        add(RiskKeyword("家族には内緒", RiskCategory.SECRECY, W_STRONG))
        add(RiskKeyword("ここでは話せない", RiskCategory.SECRECY, W_MEDIUM))
        add(RiskKeyword("こっそり", RiskCategory.SECRECY, W_MEDIUM))

        // §7.3 会う約束 ------------------------------------------------------
        add(RiskKeyword("会おう", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("あおう", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("会いに", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("会いたい", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("駅で", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("駅の", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("駅前", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("公園で", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("公園の", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("ファミレス", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("ふぁみれす", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("カラオケ", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("からおけ", RiskCategory.MEETUP, W_WEAK))
        add(RiskKeyword("トイレ", RiskCategory.MEETUP, W_MEDIUM))
        add(RiskKeyword("といれ", RiskCategory.MEETUP, W_MEDIUM))
        add(RiskKeyword("ホテル", RiskCategory.MEETUP, W_STRONG))
        add(RiskKeyword("ほてる", RiskCategory.MEETUP, W_STRONG))
        add(RiskKeyword("ラブホ", RiskCategory.MEETUP, W_STRONG))
        add(RiskKeyword("らぶほ", RiskCategory.MEETUP, W_STRONG))
        add(RiskKeyword("何時に来", RiskCategory.MEETUP, W_MEDIUM))
        add(RiskKeyword("抜け出して", RiskCategory.MEETUP, W_STRONG))
        add(RiskKeyword("抜け出そう", RiskCategory.MEETUP, W_STRONG))
        add(RiskKeyword("一人で来", RiskCategory.MEETUP, W_STRONG))
        add(RiskKeyword("家族と別れて", RiskCategory.MEETUP, W_STRONG))
        add(RiskKeyword("車で迎え", RiskCategory.MEETUP, W_STRONG))
        add(RiskKeyword("車に乗", RiskCategory.MEETUP, W_MEDIUM))

        // §7.4 性的・自撮り要求 ---------------------------------------------
        add(RiskKeyword("自撮り送って", RiskCategory.SEXUAL_REQUEST, W_STRONG))
        add(RiskKeyword("自撮り", RiskCategory.SEXUAL_REQUEST, W_MEDIUM))
        add(RiskKeyword("じどり", RiskCategory.SEXUAL_REQUEST, W_MEDIUM))
        add(RiskKeyword("写真送って", RiskCategory.SEXUAL_REQUEST, W_MEDIUM))
        add(RiskKeyword("動画送って", RiskCategory.SEXUAL_REQUEST, W_MEDIUM))
        add(RiskKeyword("見せて", RiskCategory.SEXUAL_REQUEST, W_WEAK))
        add(RiskKeyword("もっと見たい", RiskCategory.SEXUAL_REQUEST, W_MEDIUM))
        add(RiskKeyword("服を脱", RiskCategory.SEXUAL_REQUEST, W_STRONG))
        add(RiskKeyword("脱いで", RiskCategory.SEXUAL_REQUEST, W_STRONG))
        add(RiskKeyword("下着", RiskCategory.SEXUAL_REQUEST, W_STRONG))
        add(RiskKeyword("したぎ", RiskCategory.SEXUAL_REQUEST, W_STRONG))
        add(RiskKeyword("裸", RiskCategory.SEXUAL_REQUEST, W_STRONG))
        add(RiskKeyword("はだか", RiskCategory.SEXUAL_REQUEST, W_STRONG))
        add(RiskKeyword("胸", RiskCategory.SEXUAL_REQUEST, W_MEDIUM))
        add(RiskKeyword("おっぱい", RiskCategory.SEXUAL_REQUEST, W_STRONG))
        add(RiskKeyword("えっち", RiskCategory.SEXUAL_REQUEST, W_STRONG))
        add(RiskKeyword("ぱんつ", RiskCategory.SEXUAL_REQUEST, W_STRONG))

        // §7.5 脅し・支配 ---------------------------------------------------
        add(RiskKeyword("断ったら", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("ことわったら", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("ばらす", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("バラす", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("言うこと聞", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("いうこと聞", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("誰にも言うな", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("だれにも言うな", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("逃げないで", RiskCategory.COERCION, W_MEDIUM))
        add(RiskKeyword("消したら怒", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("ブロックしたら", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("ぶろっくしたら", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("返信しないと", RiskCategory.COERCION, W_MEDIUM))
        add(RiskKeyword("返事しないと", RiskCategory.COERCION, W_MEDIUM))
        add(RiskKeyword("殺す", RiskCategory.COERCION, W_STRONG))
        add(RiskKeyword("ころす", RiskCategory.COERCION, W_STRONG))

        // §7.6 身元不明リスク -----------------------------------------------
        add(RiskKeyword("年齢ひみつ", RiskCategory.IDENTITY_UNKNOWN, W_MEDIUM))
        add(RiskKeyword("年齢秘密", RiskCategory.IDENTITY_UNKNOWN, W_MEDIUM))
        add(RiskKeyword("年いくつ", RiskCategory.IDENTITY_UNKNOWN, W_WEAK))
        add(RiskKeyword("いくつなの", RiskCategory.IDENTITY_UNKNOWN, W_WEAK))
        add(RiskKeyword("何歳", RiskCategory.IDENTITY_UNKNOWN, W_WEAK))
        add(RiskKeyword("なんさい", RiskCategory.IDENTITY_UNKNOWN, W_WEAK))
        add(RiskKeyword("学校どこ", RiskCategory.IDENTITY_UNKNOWN, W_WEAK))
        add(RiskKeyword("学校言わない", RiskCategory.IDENTITY_UNKNOWN, W_MEDIUM))
        add(RiskKeyword("本名言わない", RiskCategory.IDENTITY_UNKNOWN, W_MEDIUM))
        add(RiskKeyword("住所", RiskCategory.IDENTITY_UNKNOWN, W_MEDIUM))
        add(RiskKeyword("じゅうしょ", RiskCategory.IDENTITY_UNKNOWN, W_MEDIUM))
    }

    /** all を正規化済みパターンに展開した検索用テーブル。 */
    internal val normalized: List<RiskKeyword> by lazy {
        all.map { it.copy(pattern = TextNormalizer.normalize(it.pattern)) }
    }
}
