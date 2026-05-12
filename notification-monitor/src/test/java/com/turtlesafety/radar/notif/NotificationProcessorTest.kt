package com.turtlesafety.radar.notif

import com.turtlesafety.radar.core.log.DetectionLog
import com.turtlesafety.radar.core.log.DetectionLogDao
import com.turtlesafety.radar.core.log.DetectionLogRepository
import com.turtlesafety.radar.core.risk.RuleBasedRiskEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationProcessorTest {

    private class RecordingDao : DetectionLogDao {
        val inserted = mutableListOf<DetectionLog>()

        override suspend fun insert(log: DetectionLog): Long {
            inserted += log
            return inserted.size.toLong()
        }
        override suspend fun get(id: Long): DetectionLog? = inserted.getOrNull(id.toInt() - 1)
        override fun observeRecent(limit: Int): Flow<List<DetectionLog>> = emptyFlow()
        override fun observeByMinScore(minScore: Int, limit: Int): Flow<List<DetectionLog>> = emptyFlow()
        override suspend fun acknowledge(id: Long) {}
        override suspend fun pruneOlderThan(maxScoreExclusive: Int, beforeMillis: Long) {}
        override suspend fun clearAll() { inserted.clear() }
        override suspend fun count(): Int = inserted.size
    }

    private val monitoredPkg = "jp.naver.line.android"

    @Test
    fun `non monitored package is ignored`() = runTest(UnconfinedTestDispatcher()) {
        val dao = RecordingDao()
        val processor = NotificationProcessor(
            riskEngine = RuleBasedRiskEngine(),
            isMonitored = { it == monitoredPkg },
            repository = DetectionLogRepository(dao),
            scope = TestScope(coroutineContext),
        )

        val outcome = processor.process(
            packageName = "com.unmonitored.app",
            title = "親に言わないでね",
            text = "QR送って",
        )
        assertEquals(NotificationProcessor.Outcome.IGNORED_NOT_MONITORED, outcome)
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun `empty title and text are ignored`() = runTest(UnconfinedTestDispatcher()) {
        val dao = RecordingDao()
        val processor = NotificationProcessor(
            riskEngine = RuleBasedRiskEngine(),
            isMonitored = { true },
            repository = DetectionLogRepository(dao),
            scope = TestScope(coroutineContext),
        )

        assertEquals(
            NotificationProcessor.Outcome.IGNORED_EMPTY,
            processor.process(monitoredPkg, null, null),
        )
        assertEquals(
            NotificationProcessor.Outcome.IGNORED_EMPTY,
            processor.process(monitoredPkg, "", ""),
        )
    }

    @Test
    fun `benign notification is not recorded`() = runTest(UnconfinedTestDispatcher()) {
        val dao = RecordingDao()
        val processor = NotificationProcessor(
            riskEngine = RuleBasedRiskEngine(),
            isMonitored = { true },
            repository = DetectionLogRepository(dao),
            scope = TestScope(coroutineContext),
        )

        val outcome = processor.process(
            packageName = monitoredPkg,
            title = "ママ",
            text = "今日は早く帰るね",
        )
        assertEquals(NotificationProcessor.Outcome.SCORED_BELOW_THRESHOLD, outcome)
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun `high risk combination is recorded`() = runTest(UnconfinedTestDispatcher()) {
        val dao = RecordingDao()
        val processor = NotificationProcessor(
            riskEngine = RuleBasedRiskEngine(),
            isMonitored = { true },
            repository = DetectionLogRepository(dao),
            scope = TestScope(coroutineContext),
        )

        val outcome = processor.process(
            packageName = monitoredPkg,
            title = "知らない人",
            text = "駅で会おう 親には言わないで",
        )
        assertEquals(NotificationProcessor.Outcome.RECORDED, outcome)
        assertEquals(1, dao.inserted.size)
        val log = dao.inserted.single()
        assertEquals(monitoredPkg, log.appName)
        assertEquals("NOTIFICATION", log.source)
        assertTrue("score should be >=4, got ${log.score}", log.score >= 4)
    }
}
