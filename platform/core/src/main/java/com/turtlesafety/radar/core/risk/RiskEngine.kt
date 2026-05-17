package com.turtlesafety.radar.core.risk

/**
 * 入力テキストのリスク評価を行う最上位インタフェース。
 *
 * 実装は端末内 (オフライン) で完結することを前提とする。
 * クラウドAI連携 (Level 3) は別レイヤで `RiskEngine` をラップする想定。
 */
interface RiskEngine {
    fun assess(
        text: String,
        source: DetectionSource,
        appName: String? = null,
    ): RiskAssessment
}
