package com.turtlesafety.radar.core.log

import java.util.concurrent.TimeUnit

/**
 * 仕様書 v0.4 §11.3 のログ保持ポリシーを適用する。
 *
 * - score 0..1 (低リスク): 7日
 * - score 2..3 (中リスク): 30日
 * - score >= 4 (高リスク): 90日
 *
 * 呼び出し側は起動時や日次タスクから [enforce] を実行する想定。
 */
class LogRetentionEnforcer(
    private val dao: DetectionLogDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val lowRiskRetentionMillis: Long = DEFAULT_LOW_RETENTION,
    private val mediumRiskRetentionMillis: Long = DEFAULT_MEDIUM_RETENTION,
    private val highRiskRetentionMillis: Long = DEFAULT_HIGH_RETENTION,
) {

    suspend fun enforce() {
        val now = clock()
        // 低 (score < 2) を 7日超過分削除
        dao.pruneOlderThan(
            maxScoreExclusive = 2,
            beforeMillis = now - lowRiskRetentionMillis,
        )
        // 中 (score < 4) を 30日超過分削除
        dao.pruneOlderThan(
            maxScoreExclusive = 4,
            beforeMillis = now - mediumRiskRetentionMillis,
        )
        // 全て (score < 6) を 90日超過分削除 — 実質高リスクも対象。
        dao.pruneOlderThan(
            maxScoreExclusive = 6,
            beforeMillis = now - highRiskRetentionMillis,
        )
    }

    companion object {
        val DEFAULT_LOW_RETENTION: Long = TimeUnit.DAYS.toMillis(7)
        val DEFAULT_MEDIUM_RETENTION: Long = TimeUnit.DAYS.toMillis(30)
        val DEFAULT_HIGH_RETENTION: Long = TimeUnit.DAYS.toMillis(90)
    }
}
