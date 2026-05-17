package com.turtlesafety.radar.core.risk

import com.turtlesafety.radar.core.settings.Sensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitivityAwareRiskEngineTest {

    @Test
    fun `high sensitivity raises non zero score`() {
        val engine = SensitivityAwareRiskEngine(
            delegate = RuleBasedRiskEngine(),
            currentSensitivity = { Sensitivity.HIGH },
        )

        val result = engine.assess("今度どこかで会おうよ", DetectionSource.IME)

        assertEquals(2, result.score)
        assertTrue(result.reason.contains("sensitivity=HIGH"))
    }

    @Test
    fun `low sensitivity does not make score negative`() {
        val engine = SensitivityAwareRiskEngine(
            delegate = RuleBasedRiskEngine(),
            currentSensitivity = { Sensitivity.LOW },
        )

        val result = engine.assess("今度どこかで会おうよ", DetectionSource.IME)

        assertEquals(0, result.score)
    }

    @Test
    fun `benign text stays zero even on high sensitivity`() {
        val engine = SensitivityAwareRiskEngine(
            delegate = RuleBasedRiskEngine(),
            currentSensitivity = { Sensitivity.HIGH },
        )

        val result = engine.assess("今日は学校で給食を食べた", DetectionSource.IME)

        assertEquals(RiskAssessment.NONE, result)
    }
}