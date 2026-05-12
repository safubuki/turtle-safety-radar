package com.turtlesafety.radar.core.log

import com.turtlesafety.radar.core.risk.DetectionSource
import com.turtlesafety.radar.core.risk.RiskAssessment
import kotlinx.coroutines.flow.Flow

/**
 * リスクエンジンの判定結果をログとして保存し、保護者向け一覧に提供する。
 *
 * 仕様書 v0.4 §11 の保存方針:
 * - 高リスク (score >= 4) は必ず保存
 * - 中以下は要件次第で保存 (MVP では呼び出し側が判断)
 * - 全文は保存しない
 */
class DetectionLogRepository(
    private val dao: DetectionLogDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    /**
     * リスク判定結果をログ化する。
     *
     * @return 挿入されたログの id。
     */
    suspend fun record(
        assessment: RiskAssessment,
        source: DetectionSource,
        appName: String? = null,
    ): Long {
        val entity = DetectionLog(
            timestamp = clock(),
            source = source.name,
            appName = appName,
            categories = assessment.categories.joinToString(",") { it.name },
            score = assessment.score,
            excerpt = assessment.excerpt,
            reason = assessment.reason,
        )
        return dao.insert(entity)
    }

    fun observeRecent(limit: Int = 100): Flow<List<DetectionLog>> =
        dao.observeRecent(limit)

    fun observeHighRisk(limit: Int = 100): Flow<List<DetectionLog>> =
        dao.observeByMinScore(minScore = 4, limit = limit)

    suspend fun acknowledge(id: Long) = dao.acknowledge(id)

    suspend fun clearAll() = dao.clearAll()

    suspend fun count(): Int = dao.count()
}
