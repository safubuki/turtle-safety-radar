package com.turtlesafety.radar.ime

import com.turtlesafety.radar.core.log.DetectionLog
import com.turtlesafety.radar.core.log.DetectionLogDao
import com.turtlesafety.radar.core.log.DetectionLogRepository
import com.turtlesafety.radar.core.risk.RuleBasedRiskEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreSendCheckerTest {

    private class RecordingDao : DetectionLogDao {
        val inserted = mutableListOf<DetectionLog>()
        override suspend fun insert(log: DetectionLog): Long {
            inserted += log
            return inserted.size.toLong()
        }
        override suspend fun get(id: Long): DetectionLog? = null
        override fun observeRecent(limit: Int): Flow<List<DetectionLog>> = emptyFlow()
        override fun observeByMinScore(minScore: Int, limit: Int): Flow<List<DetectionLog>> = emptyFlow()
        override suspend fun acknowledge(id: Long) {}
        override suspend fun pruneOlderThan(maxScoreExclusive: Int, beforeMillis: Long) {}
        override suspend fun clearAll() { inserted.clear() }
        override suspend fun count(): Int = inserted.size
    }

    private fun newChecker(dao: RecordingDao, scope: TestScope) = PreSendChecker(
        riskEngine = RuleBasedRiskEngine(),
        repository = DetectionLogRepository(dao),
        scope = scope,
    )

    @Test
    fun `blank input yields NONE and no log`() = runTest(UnconfinedTestDispatcher()) {
        val dao = RecordingDao()
        val checker = newChecker(dao, TestScope(coroutineContext))

        val result = checker.check("   ")
        assertEquals(PreSendChecker.WarningLevel.NONE, result.level)
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun `benign input yields NONE and no log`() = runTest(UnconfinedTestDispatcher()) {
        val dao = RecordingDao()
        val checker = newChecker(dao, TestScope(coroutineContext))

        val result = checker.check("今日は学校で給食を食べた")
        assertEquals(PreSendChecker.WarningLevel.NONE, result.level)
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun `single weak keyword yields NOTICE without log`() = runTest(UnconfinedTestDispatcher()) {
        val dao = RecordingDao()
        val checker = newChecker(dao, TestScope(coroutineContext))

        val result = checker.check("今度どこかで会おうよ")
        assertEquals(PreSendChecker.WarningLevel.NOTICE, result.level)
        // NOTICE は score==1 で MIN_LOG_SCORE(2) 未満なのでログしない。
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun `combination input yields STOP and logs the detection`() = runTest(UnconfinedTestDispatcher()) {
        val dao = RecordingDao()
        val checker = newChecker(dao, TestScope(coroutineContext))

        val result = checker.check("駅で会おう 親には言わないで")
        assertEquals(PreSendChecker.WarningLevel.STOP, result.level)
        assertEquals(1, dao.inserted.size)
        val log = dao.inserted.single()
        assertEquals("IME", log.source)
        assertTrue("score should be >=4, got ${log.score}", log.score >= 4)
    }
}
