package com.turtlesafety.radar.core.risk

import com.turtlesafety.radar.core.settings.LocalAiMode

/**
 * Local AI モードに応じて、ルールベース判定へ軽い文脈補正を加える。
 *
 * - BASIC: そのまま返す
 * - STANDARD: 軽量ヒューリスティックで境界ケースを 1 段引き上げる
 * - LOCAL_LLM / CLOUD_ASSIST: 現時点では STANDARD の補正へフォールバックする
 */
class LocalAiAwareRiskEngine(
    private val delegate: RiskEngine,
    private val currentMode: () -> LocalAiMode,
) : RiskEngine {

    override fun assess(
        text: String,
        source: DetectionSource,
        appName: String?,
    ): RiskAssessment {
        val base = delegate.assess(text, source, appName)
        val mode = currentMode()
        return when (mode) {
            LocalAiMode.BASIC -> base
            LocalAiMode.STANDARD -> refine(base, text, mode, fallback = null)
            LocalAiMode.LOCAL_LLM -> refine(base, text, mode, fallback = LocalAiMode.STANDARD)
            LocalAiMode.CLOUD_ASSIST -> refine(base, text, mode, fallback = LocalAiMode.STANDARD)
        }
    }

    private fun refine(
        base: RiskAssessment,
        text: String,
        mode: LocalAiMode,
        fallback: LocalAiMode?,
    ): RiskAssessment {
        if (base.score == 0) return base

        val normalized = TextNormalizer.normalize(text)
        val cueCount = CONTEXT_CUES.count { normalized.contains(it) }
        val hasHandleOrPhone = HANDLE_OR_PHONE.containsMatchIn(normalized)
        val meetupContext = normalized.contains("今度") && normalized.contains("会")

        val bonus = when {
            base.score in 1..3 && cueCount >= 2 -> 1
            base.score in 1..3 && hasHandleOrPhone -> 1
            base.score in 2..3 && meetupContext -> 1
            else -> 0
        }

        if (bonus == 0 && fallback == null) return base

        val adjustedScore = (base.score + bonus).coerceIn(0, RuleBasedRiskEngine.MAX_SCORE)
        val suffix = buildString {
            append("local_ai=")
            append(mode.name)
            fallback?.let {
                append(" fallback=")
                append(it.name)
            }
            append(" bonus=")
            append(bonus)
        }
        return base.copy(
            score = adjustedScore,
            reason = "${base.reason} | $suffix",
        )
    }

    companion object {
        private val CONTEXT_CUES = listOf(
            TextNormalizer.normalize("あとで"),
            TextNormalizer.normalize("すぐ"),
            TextNormalizer.normalize("こっそり"),
            TextNormalizer.normalize("追加"),
            TextNormalizer.normalize("連絡"),
            TextNormalizer.normalize("送って"),
            TextNormalizer.normalize("教えて"),
            TextNormalizer.normalize("dm"),
            TextNormalizer.normalize("line"),
            TextNormalizer.normalize("discord"),
            TextNormalizer.normalize("インスタ"),
        )

        private val HANDLE_OR_PHONE = Regex(
            pattern = "(@[a-z0-9_\\.]{3,}|discord\\.gg/[a-z0-9]+|[0-9]{2,4}-[0-9]{2,4}-[0-9]{3,4})",
        )
    }
}