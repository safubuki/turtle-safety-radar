package com.turtlesafety.radar.media

import com.turtlesafety.radar.core.risk.DetectionSource
import com.turtlesafety.radar.core.risk.RuleBasedRiskEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSignalAnalyzerTest {

    @Test
    fun `qr plus contact prompt becomes high risk`() {
        val result = MediaSignalAnalyzer.analyze(
            extractedText = "Discord来て DMして",
            qrPayloads = listOf("https://discord.gg/example"),
            riskEngine = RuleBasedRiskEngine(),
        )

        assertEquals(DetectionSource.QR, result.source)
        assertTrue(result.score >= 4)
        assertTrue(result.signals.any { it.label == "QRコード" })
    }

    @Test
    fun `phone number and school hint become medium or higher`() {
        val result = MediaSignalAnalyzer.analyze(
            extractedText = "090-1234-5678 桜丘中学校で待ってる",
            qrPayloads = emptyList(),
            riskEngine = RuleBasedRiskEngine(),
        )

        assertEquals(DetectionSource.IMAGE, result.source)
        assertTrue(result.score >= 3)
        assertTrue(result.signals.any { it.label == "電話番号" })
        assertTrue(result.signals.any { it.label == "学校名らしき表現" })
    }

    @Test
    fun `benign image text stays low risk`() {
        val result = MediaSignalAnalyzer.analyze(
            extractedText = "今日の給食はカレーでした",
            qrPayloads = emptyList(),
            riskEngine = RuleBasedRiskEngine(),
        )

        assertEquals(0, result.score)
        assertTrue(result.signals.isEmpty())
    }
}