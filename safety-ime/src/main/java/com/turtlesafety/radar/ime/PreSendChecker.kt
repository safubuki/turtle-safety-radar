package com.turtlesafety.radar.ime

import com.turtlesafety.radar.core.log.DetectionLogRepository
import com.turtlesafety.radar.core.risk.DetectionSource
import com.turtlesafety.radar.core.risk.RiskAssessment
import com.turtlesafety.radar.core.risk.RiskEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * IME での送信前検査ロジック (Android 非依存)。
 *
 * - 入力テキストをリスクエンジンで評価
 * - 結果に応じて [WarningLevel] を返す
 * - 中以上のリスクはログ保存をスケジュール
 *
 * 仕様書 §5.3 / §17.3 を反映:
 * - 全文は保存しない (RiskAssessment.excerpt 経由で短い抜粋のみ)
 * - 高リスクのみ最小限ログを保存 (MIN_LOG_SCORE)
 */
class PreSendChecker(
    private val riskEngine: RiskEngine,
    private val repository: DetectionLogRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    fun check(text: String): CheckResult {
        if (text.isBlank()) {
            return CheckResult(level = WarningLevel.NONE, assessment = RiskAssessment.NONE)
        }
        val assessment = riskEngine.assess(text, DetectionSource.IME)
        if (assessment.score >= MIN_LOG_SCORE) {
            scope.launch {
                runCatching {
                    repository.record(assessment, DetectionSource.IME, appName = null)
                }
            }
        }
        return CheckResult(level = WarningLevel.fromScore(assessment.score), assessment = assessment)
    }

    data class CheckResult(
        val level: WarningLevel,
        val assessment: RiskAssessment,
    )

    /** 子ども側に出す警告強度 (仕様書 §10)。 */
    enum class WarningLevel(val displayMessage: String) {
        NONE(""),
        NOTICE("入力に少し注意が必要な表現があります"),
        WARN("入力に注意してください。送る前に家族に相談しましょう"),
        STOP("これは送らないでください。すぐに保護者に相談してください"),
        ;

        companion object {
            fun fromScore(score: Int): WarningLevel = when {
                score >= 4 -> STOP
                score >= 2 -> WARN
                score >= 1 -> NOTICE
                else -> NONE
            }
        }
    }

    companion object {
        /** ログ保存閾値 (中リスク以上のみ保存)。 */
        const val MIN_LOG_SCORE = 2
    }
}
