package com.turtlesafety.radar.core.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleBasedRiskEngineTest {

    private lateinit var engine: RuleBasedRiskEngine

    @Before
    fun setUp() {
        engine = RuleBasedRiskEngine()
    }

    @Test
    fun `blank text yields NONE`() {
        val result = engine.assess("", DetectionSource.IME)
        assertEquals(RiskAssessment.NONE, result)
    }

    @Test
    fun `benign text yields zero score`() {
        val result = engine.assess("今日は学校で給食を食べた", DetectionSource.IME)
        assertEquals(0, result.score)
        assertTrue(result.categories.isEmpty())
        assertNull(result.excerpt)
    }

    @Test
    fun `single weak meetup keyword stays low or medium`() {
        // §8.1 「会おう」単独は誤検知が多いため低〜中に留める。
        val result = engine.assess("今度どこかで会おうよ", DetectionSource.IME)
        assertTrue("score=${result.score} should be <=2", result.score <= 2)
        assertTrue(RiskCategory.MEETUP in result.categories)
    }

    @Test
    fun `meetup plus secrecy escalates to high risk`() {
        // §8.2 会う約束 + 秘密化
        val result = engine.assess("駅で会おう 親には言わないで", DetectionSource.IME)
        assertTrue("score=${result.score} should be >=4", result.score >= 4)
        assertTrue(RiskCategory.MEETUP in result.categories)
        assertTrue(RiskCategory.SECRECY in result.categories)
    }

    @Test
    fun `sexual request plus secrecy reaches maximum`() {
        val result = engine.assess("自撮り送って 内緒だよ", DetectionSource.IME)
        assertEquals(5, result.score)
    }

    @Test
    fun `contact exchange plus secrecy is high risk`() {
        val result = engine.assess("インスタ教えて ばれないようにね", DetectionSource.IME)
        assertTrue("score=${result.score} should be >=4", result.score >= 4)
        assertTrue(RiskCategory.CONTACT_EXCHANGE in result.categories)
        assertTrue(RiskCategory.SECRECY in result.categories)
    }

    @Test
    fun `single category alone is capped below high risk`() {
        // PER_CATEGORY_CAP = 3 によりカテゴリ単独で >=4 にならない。
        val result = engine.assess("バラす バラす 言うこと聞いて 誰にも言うな", DetectionSource.IME)
        assertTrue(
            "single COERCION should stay below 4, got ${result.score}",
            result.score <= 3
        )
        assertTrue(RiskCategory.COERCION in result.categories)
    }

    @Test
    fun `normalization handles fullwidth and uppercase`() {
        // 全角QR + 全角英字を含む
        val result = engine.assess("ＱＲ送ってよ", DetectionSource.IME)
        assertTrue(RiskCategory.CONTACT_EXCHANGE in result.categories)
    }

    @Test
    fun `excerpt is short and centered on highest weight match`() {
        val text = "今日学校で楽しかった でも自撮り送ってと言われた どうしよう"
        val result = engine.assess(text, DetectionSource.IME)
        assertNotNull(result.excerpt)
        // EXCERPT_RADIUS=20 → 前後20文字 + キーワード長 + 省略記号 程度
        assertTrue(
            "excerpt length=${result.excerpt!!.length} should be <= 60",
            result.excerpt!!.length <= 60,
        )
        assertTrue(result.excerpt!!.contains("自撮り"))
    }

    @Test
    fun `meetup plus identity unknown reaches maximum`() {
        val result = engine.assess("一人で来てね 年齢ひみつだよ", DetectionSource.IME)
        assertEquals(5, result.score)
    }

    @Test
    fun `reason string lists categories with scores`() {
        val result = engine.assess("駅で会おう 親には言わないで", DetectionSource.IME)
        assertTrue(result.reason.contains("MEETUP"))
        assertTrue(result.reason.contains("SECRECY"))
        assertTrue(result.reason.contains("score="))
    }
}
