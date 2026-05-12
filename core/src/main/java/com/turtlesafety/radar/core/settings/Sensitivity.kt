package com.turtlesafety.radar.core.settings

/** 検知感度 (仕様書 v0.4 §17.1)。 */
enum class Sensitivity(val scoreDelta: Int) {
    /** スコアを 1 下げて誤検知重視。 */
    LOW(-1),
    /** デフォルト。エンジンのスコアをそのまま採用。 */
    NORMAL(0),
    /** スコアを 1 上げて検知漏れを減らす。 */
    HIGH(+1),
    ;

    companion object {
        fun fromName(name: String?): Sensitivity =
            entries.firstOrNull { it.name == name } ?: NORMAL
    }
}
