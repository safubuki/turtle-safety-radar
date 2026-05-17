package com.turtlesafety.radar.core.risk

import com.turtlesafety.radar.core.settings.LocalAiMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAiAwareRiskEngineTest {

    @Test
    fun `basic mode keeps original assessment`() {
        val engine = LocalAiAwareRiskEngine(
            delegate = RuleBasedRiskEngine(),
            currentMode = { LocalAiMode.BASIC },
        )

        val result = engine.assess("今度どこかで会おうよ", DetectionSource.IME)

        assertEquals(1, result.score)
        assertTrue(!result.reason.contains("local_ai="))
    }

    @Test
    fun `standard mode raises borderline contextual score`() {
        val engine = LocalAiAwareRiskEngine(
            delegate = RuleBasedRiskEngine(),
            currentMode = { LocalAiMode.STANDARD },
        )

        val result = engine.assess("今度会おう DMで連絡して", DetectionSource.IME)

        assertEquals(3, result.score)
        assertTrue(result.reason.contains("local_ai=STANDARD"))
    }

    @Test
    fun `cloud assist currently falls back to standard heuristic`() {
        val engine = LocalAiAwareRiskEngine(
            delegate = RuleBasedRiskEngine(),
            currentMode = { LocalAiMode.CLOUD_ASSIST },
        )

        val result = engine.assess("今度会おう DMで連絡して", DetectionSource.IME)

        assertEquals(3, result.score)
        assertTrue(result.reason.contains("fallback=STANDARD"))
    }
}