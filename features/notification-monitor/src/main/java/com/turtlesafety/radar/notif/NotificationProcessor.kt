package com.turtlesafety.radar.notif

import com.turtlesafety.radar.core.log.DetectionLogRepository
import com.turtlesafety.radar.core.risk.DetectionSource
import com.turtlesafety.radar.core.risk.RiskAssessment
import com.turtlesafety.radar.core.risk.RiskEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 通知 1 件の文字列をリスクエンジンに通し、ログ DB へ保存するかを決める純粋ロジック。
 *
 * Android 依存を持たないので JVM ユニットテスト可能。
 * [RadarNotificationListenerService] が実機側のアダプタとして本クラスを呼び出す。
 *
 * 仕様書 §5.2 / §11 の方針:
 * - 監視対象アプリでない場合は処理しない (誤検知/プライバシー両面で安全側)
 * - 通知本文が取れない場合 (タイトル/本文すべて空) はスキップ
 * - スコアが [MIN_SAVE_SCORE] 未満ならログ保存しない (低リスクのスパムを避ける)
 */
class NotificationProcessor(
    private val riskEngine: RiskEngine,
    private val isMonitored: (String) -> Boolean,
    private val repository: DetectionLogRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    fun process(packageName: String, title: String?, text: String?): Outcome {
        if (!isMonitored(packageName)) return Outcome.IGNORED_NOT_MONITORED

        val hasAccessibleBody = !text.isNullOrBlank()
        val combined = listOfNotNull(title, text)
            .joinToString(separator = " ")
            .trim()
        if (combined.isEmpty()) {
            recordNoTextEvent(packageName)
            return Outcome.RECORDED_NO_TEXT_EVENT
        }

        val assessment = riskEngine.assess(
            text = combined,
            source = DetectionSource.NOTIFICATION,
            appName = packageName,
        )

        if (assessment.score < MIN_SAVE_SCORE) {
            if (!hasAccessibleBody) {
                recordNoTextEvent(packageName)
                return Outcome.RECORDED_NO_TEXT_EVENT
            }
            return Outcome.SCORED_BELOW_THRESHOLD
        }

        scope.launch {
            runCatching {
                repository.record(assessment, DetectionSource.NOTIFICATION, packageName)
            }
        }
        return Outcome.RECORDED
    }

    private fun recordNoTextEvent(packageName: String) {
        scope.launch {
            runCatching {
                repository.record(
                    assessment = RiskAssessment(
                        score = 0,
                        categories = emptySet(),
                        matches = emptyList(),
                        reason = NO_TEXT_REASON,
                        excerpt = null,
                    ),
                    source = DetectionSource.NOTIFICATION,
                    appName = packageName,
                )
            }
        }
    }

    enum class Outcome {
        /** ログ保存をスケジュールした。 */
        RECORDED,
        /** 監視対象外のパッケージ。 */
        IGNORED_NOT_MONITORED,
        /** タイトル・本文ともに空。 */
        RECORDED_NO_TEXT_EVENT,
        /** スコアが [MIN_SAVE_SCORE] 未満。 */
        SCORED_BELOW_THRESHOLD,
    }

    companion object {
        /** 保存閾値。スコア 1 以上 (= 何らかのリスク語を検知) で保存。 */
        const val MIN_SAVE_SCORE = 1

        const val NO_TEXT_REASON = "notification posted without accessible text"
    }
}
