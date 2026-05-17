package com.turtlesafety.radar.core.risk

import com.turtlesafety.radar.core.settings.Sensitivity

/**
 * 保存済みの感度設定を毎回参照して、ベース判定スコアへ補正を加えるラッパー。
 *
 * 誤検知を避けるため、ベーススコアが 0 の場合は補正しない。
 */
class SensitivityAwareRiskEngine(
    private val delegate: RiskEngine,
    private val currentSensitivity: () -> Sensitivity,
) : RiskEngine {

    override fun assess(
        text: String,
        source: DetectionSource,
        appName: String?,
    ): RiskAssessment {
        val base = delegate.assess(text, source, appName)
        if (base.score == 0) return base

        val sensitivity = currentSensitivity()
        val delta = sensitivity.scoreDelta
        if (delta == 0) return base

        val adjustedScore = (base.score + delta).coerceIn(0, RuleBasedRiskEngine.MAX_SCORE)
        if (adjustedScore == base.score) return base

        return base.copy(
            score = adjustedScore,
            reason = "${base.reason} | sensitivity=${sensitivity.name} delta=$delta",
        )
    }
}