package com.turtlesafety.radar.core.log

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionLogDao {

    @Insert
    suspend fun insert(log: DetectionLog): Long

    @Query("SELECT * FROM detection_log WHERE id = :id")
    suspend fun get(id: Long): DetectionLog?

    @Query("SELECT * FROM detection_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DetectionLog>>

    @Query(
        "SELECT * FROM detection_log " +
            "WHERE score >= :minScore " +
            "ORDER BY timestamp DESC " +
            "LIMIT :limit"
    )
    fun observeByMinScore(minScore: Int, limit: Int): Flow<List<DetectionLog>>

    @Query("UPDATE detection_log SET acknowledged = 1 WHERE id = :id")
    suspend fun acknowledge(id: Long)

    /** [maxScoreExclusive] 未満かつ [beforeMillis] より古いログを削除。 */
    @Query("DELETE FROM detection_log WHERE score < :maxScoreExclusive AND timestamp < :beforeMillis")
    suspend fun pruneOlderThan(maxScoreExclusive: Int, beforeMillis: Long)

    @Query("DELETE FROM detection_log")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM detection_log")
    suspend fun count(): Int
}
