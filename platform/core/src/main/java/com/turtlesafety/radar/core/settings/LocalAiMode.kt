package com.turtlesafety.radar.core.settings

/**
 * 端末内 AI の動作モード。
 *
 * MVP では BASIC/STANDARD までは実装対象、LOCAL_LLM/CLOUD_ASSIST は設定入口を先に提供する。
 */
enum class LocalAiMode {
    /** ルールベースのみ。 */
    BASIC,

    /** 軽量な文脈補正を加える。 */
    STANDARD,

    /** 将来のローカル LLM 用。 */
    LOCAL_LLM,

    /** 将来のクラウド補助用。 */
    CLOUD_ASSIST,
    ;

    companion object {
        fun fromName(name: String?): LocalAiMode =
            entries.firstOrNull { it.name == name } ?: BASIC
    }
}