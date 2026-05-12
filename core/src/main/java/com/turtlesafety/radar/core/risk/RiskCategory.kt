package com.turtlesafety.radar.core.risk

/**
 * 仕様書 v0.4 §7 の危険カテゴリ。
 *
 * 単独の検知ではスコアは低めに抑え、カテゴリの組み合わせで高リスク化させる (§8.2)。
 */
enum class RiskCategory {
    /** §7.1 連絡先交換: QR/SNS ID/別アプリ誘導など。 */
    CONTACT_EXCHANGE,

    /** §7.2 秘密化: 親に言わない / 履歴消す / 通知切る / バレないように など。 */
    SECRECY,

    /** §7.3 会う約束: 場所 + 時間 + 単独 + 抜け出し など。 */
    MEETUP,

    /** §7.4 性的・自撮り要求。 */
    SEXUAL_REQUEST,

    /** §7.5 脅し・支配: バラす / 言うこと聞いて / 返信しないと困る など。 */
    COERCION,

    /** §7.6 身元不明リスク: 年齢/学校/本名不明、顔写真だけ要求 など。 */
    IDENTITY_UNKNOWN,
}
