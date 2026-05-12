package com.turtlesafety.radar.core.risk

import kotlin.math.max
import kotlin.math.min

/**
 * 仕様書 v0.4 §7-§8 を実装する Level 0 のリスクエンジン。
 *
 * - キーワード辞書は [RiskKeywords] が保持する。
 * - 単独カテゴリのスコアは [PER_CATEGORY_CAP] で頭打ちにし、
 *   §8.2 の組み合わせ条件でボーナスを加算する。
 * - 最終スコアは 0..5 にクランプ。
 *
 * 副作用なし・スレッドセーフ。ユニットテストで挙動を検証する。
 */
class RuleBasedRiskEngine : RiskEngine {

    override fun assess(
        text: String,
        source: DetectionSource,
        appName: String?,
    ): RiskAssessment {
        if (text.isBlank()) return RiskAssessment.NONE

        val normalized = TextNormalizer.normalize(text)
        val matches = findMatches(normalized, text)
        if (matches.isEmpty()) return RiskAssessment.NONE

        val byCategory: Map<RiskCategory, List<RiskMatch>> = matches.groupBy { it.category }
        val perCategoryScores: Map<RiskCategory, Int> = byCategory.mapValues { (_, list) ->
            min(PER_CATEGORY_CAP, list.sumOf { keywordWeightFor(it) })
        }

        val baseScore = perCategoryScores.values.max()
        val bonus = combinationBonus(perCategoryScores.keys)
        val finalScore = clamp(baseScore + bonus, 0, MAX_SCORE)

        val excerpt = buildExcerpt(text, matches)
        val reason = buildReason(perCategoryScores, bonus, finalScore)

        return RiskAssessment(
            score = finalScore,
            categories = perCategoryScores.keys,
            matches = matches,
            reason = reason,
            excerpt = excerpt,
        )
    }

    // -----------------------------------------------------------------------
    // Matching
    // -----------------------------------------------------------------------

    private fun findMatches(normalizedText: String, originalText: String): List<RiskMatch> {
        val result = mutableListOf<RiskMatch>()
        for (kw in RiskKeywords.normalized) {
            var fromIndex = 0
            while (true) {
                val idx = normalizedText.indexOf(kw.pattern, fromIndex)
                if (idx < 0) break
                val end = idx + kw.pattern.length
                result += RiskMatch(
                    category = kw.category,
                    keyword = kw.pattern,
                    startIndex = idx.coerceAtMost(originalText.length),
                    endIndex = end.coerceAtMost(originalText.length),
                )
                fromIndex = end
            }
        }
        return result
    }

    private fun keywordWeightFor(match: RiskMatch): Int =
        RiskKeywords.normalized.firstOrNull { it.pattern == match.keyword }?.weight ?: 1

    // -----------------------------------------------------------------------
    // Combination bonus (仕様書 §8.2)
    // -----------------------------------------------------------------------

    private fun combinationBonus(categories: Set<RiskCategory>): Int {
        val sex = RiskCategory.SEXUAL_REQUEST in categories
        val secrecy = RiskCategory.SECRECY in categories
        val meet = RiskCategory.MEETUP in categories
        val contact = RiskCategory.CONTACT_EXCHANGE in categories
        val coerce = RiskCategory.COERCION in categories
        val unknown = RiskCategory.IDENTITY_UNKNOWN in categories

        // 最重: 性的+秘密化 / 性的+脅し / 会う約束+身元不明
        if (sex && secrecy) return 3
        if (sex && coerce) return 3
        if (meet && unknown) return 3

        // 高: 会う約束+秘密化 / 連絡先交換+秘密化 / 脅し+性的(冗長だが)
        if (meet && secrecy) return 2
        if (contact && secrecy) return 2
        if (coerce && sex) return 2

        // 中: 2 カテゴリ以上での共起一般
        if (categories.size >= 2) return 1
        return 0
    }

    // -----------------------------------------------------------------------
    // Excerpt / reason
    // -----------------------------------------------------------------------

    /** 最も寄与度の高いマッチを中心に前後 [EXCERPT_RADIUS] 文字を切り出す。 */
    private fun buildExcerpt(original: String, matches: List<RiskMatch>): String? {
        if (matches.isEmpty()) return null
        val pivot = matches.maxBy { keywordWeightFor(it) }
        val start = max(0, pivot.startIndex - EXCERPT_RADIUS)
        val end = min(original.length, pivot.endIndex + EXCERPT_RADIUS)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < original.length) "…" else ""
        return prefix + original.substring(start, end) + suffix
    }

    private fun buildReason(
        perCategoryScores: Map<RiskCategory, Int>,
        bonus: Int,
        finalScore: Int,
    ): String {
        val parts = perCategoryScores.entries
            .sortedByDescending { it.value }
            .joinToString(separator = ", ") { (cat, score) -> "${cat.name}=$score" }
        return "categories[$parts] +bonus=$bonus => score=$finalScore"
    }

    private fun clamp(value: Int, lo: Int, hi: Int): Int = max(lo, min(hi, value))

    companion object {
        /** 1カテゴリだけで最終スコアを 4 (高リスク) にしないための上限。 */
        const val PER_CATEGORY_CAP = 3

        /** 抜粋の前後文字数。子どもの全文を保存しない原則 (仕様書 §11.2) を守るため短く。 */
        const val EXCERPT_RADIUS = 20

        const val MAX_SCORE = 5
    }
}
