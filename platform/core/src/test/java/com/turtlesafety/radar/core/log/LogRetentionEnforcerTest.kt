package com.turtlesafety.radar.core.log

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class LogRetentionEnforcerTest {

    private class RecordingDao : DetectionLogDao {
        data class PruneCall(val maxScoreExclusive: Int, val beforeMillis: Long)

        val pruneCalls = mutableListOf<PruneCall>()

        override suspend fun insert(log: DetectionLog): Long = 1L
        override suspend fun get(id: Long): DetectionLog? = null
        override fun observeRecent(limit: Int): Flow<List<DetectionLog>> = emptyFlow()
        override fun observeByMinScore(minScore: Int, limit: Int): Flow<List<DetectionLog>> = emptyFlow()
        override suspend fun acknowledge(id: Long) {}
        override suspend fun pruneOlderThan(maxScoreExclusive: Int, beforeMillis: Long) {
            pruneCalls += PruneCall(maxScoreExclusive, beforeMillis)
        }
        override suspend fun clearAll() {}
        override suspend fun count(): Int = 0
    }

    @Test
    fun `enforce calls prune for low medium and high tiers using spec retentions`() = runTest {
        val now = 1_700_000_000_000L
        val dao = RecordingDao()
        val enforcer = LogRetentionEnforcer(dao, clock = { now })

        enforcer.enforce()

        val expected = listOf(
            RecordingDao.PruneCall(
                maxScoreExclusive = 2,
                beforeMillis = now - TimeUnit.DAYS.toMillis(7),
            ),
            RecordingDao.PruneCall(
                maxScoreExclusive = 4,
                beforeMillis = now - TimeUnit.DAYS.toMillis(30),
            ),
            RecordingDao.PruneCall(
                maxScoreExclusive = 6,
                beforeMillis = now - TimeUnit.DAYS.toMillis(90),
            ),
        )
        assertEquals(expected, dao.pruneCalls)
    }
}
