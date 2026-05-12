package com.turtlesafety.radar.core.risk

/** リスクエンジンが検出した個別のヒット。 */
data class RiskMatch(
    val category: RiskCategory,
    val keyword: String,
    val startIndex: Int,
    val endIndex: Int,
)

/**
 * リスク判定結果。スコアは仕様書 §8 の 0..5 にクランプされる。
 *
 * - score==0: 問題なし
 * - score==1: 注意
 * - score==2: 要確認
 * - score==3: 高め
 * - score==4: 高リスク (保護者通知の閾値)
 * - score==5: 即時対応推奨
 */
data class RiskAssessment(
    val score: Int,
    val categories: Set<RiskCategory>,
    val matches: List<RiskMatch>,
    val reason: String,
    val excerpt: String?,
) {
    companion object {
        val NONE = RiskAssessment(
            score = 0,
            categories = emptySet(),
            matches = emptyList(),
            reason = "no risk detected",
            excerpt = null,
        )
    }
}
