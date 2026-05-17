package com.turtlesafety.radar.media

import com.turtlesafety.radar.core.risk.DetectionSource
import com.turtlesafety.radar.core.risk.RiskAssessment
import com.turtlesafety.radar.core.risk.RiskCategory
import com.turtlesafety.radar.core.risk.RiskEngine
import java.text.Normalizer

object MediaSignalAnalyzer {

    fun analyze(
        extractedText: String,
        qrPayloads: List<String>,
        riskEngine: RiskEngine,
    ): MediaScanResult {
        val normalizedQrPayloads = qrPayloads.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val joinedText = buildString {
            if (extractedText.isNotBlank()) append(extractedText.trim())
            if (normalizedQrPayloads.isNotEmpty()) {
                if (isNotBlank()) append('\n')
                append(normalizedQrPayloads.joinToString(separator = "\n"))
            }
        }

        val baseSource = if (normalizedQrPayloads.isNotEmpty()) DetectionSource.QR else DetectionSource.IMAGE
        val base = if (joinedText.isBlank()) RiskAssessment.NONE else riskEngine.assess(joinedText, baseSource)
        val detectedSignals = detectSignals(extractedText, normalizedQrPayloads)
        val signalCategories = detectedSignals.map { it.category }.toSet()

        var score = base.score
        if (normalizedQrPayloads.isNotEmpty()) score = maxOf(score, 2)
        if (detectedSignals.any { it.category == RiskCategory.CONTACT_EXCHANGE }) score = maxOf(score, 2)
        if (detectedSignals.any { it.category == RiskCategory.IDENTITY_UNKNOWN }) score = maxOf(score, 3)
        if (normalizedQrPayloads.isNotEmpty() && detectedSignals.any { it.category == RiskCategory.CONTACT_EXCHANGE }) {
            score = maxOf(score, 4)
        }
        if (
            detectedSignals.any { it.label == "電話番号" || it.label == "住所らしき表現" || it.label == "学校名らしき表現" } &&
            detectedSignals.any { it.category == RiskCategory.CONTACT_EXCHANGE }
        ) {
            score = maxOf(score, 4)
        }
        if (base.score in 1..3 && detectedSignals.isNotEmpty()) {
            score = maxOf(score, base.score + 1)
        }
        score = score.coerceIn(0, 5)

        val categories = (base.categories + signalCategories)
        val signals = detectedSignals.map { MediaSignal(label = it.label, sample = it.sample) }
        val reason = buildReason(base.reason, signals, normalizedQrPayloads)
        val excerpt = base.excerpt ?: previewOf(extractedText.ifBlank { normalizedQrPayloads.joinToString(separator = " ") })
        val assessment = RiskAssessment(
            score = score,
            categories = categories,
            matches = base.matches,
            reason = reason,
            excerpt = excerpt,
        )

        return MediaScanResult(
            source = baseSource,
            assessment = assessment,
            extractedTextPreview = previewOf(extractedText),
            qrPayloads = normalizedQrPayloads,
            signals = signals,
            warningMessage = warningFor(score, normalizedQrPayloads.isNotEmpty()),
        )
    }

    private fun buildReason(
        baseReason: String,
        signals: List<MediaSignal>,
        qrPayloads: List<String>,
    ): String {
        val parts = mutableListOf<String>()
        if (baseReason != RiskAssessment.NONE.reason) parts += baseReason
        if (qrPayloads.isNotEmpty()) parts += "qr=${qrPayloads.size}"
        if (signals.isNotEmpty()) parts += "signals=${signals.joinToString(separator = ",") { it.label }}"
        return if (parts.isEmpty()) "no image risk detected" else parts.joinToString(separator = " | ")
    }

    private fun warningFor(score: Int, hasQr: Boolean): String = when {
        score >= 4 && hasQr -> "QR や連絡先情報の共有につながる可能性があります。保護者に相談してください"
        score >= 4 -> "画像内に高リスクの情報が含まれる可能性があります。保護者に相談してください"
        score >= 2 -> "画像内の連絡先や個人情報に注意してください"
        hasQr -> "QR コードを検出しました。共有前に内容を確認してください"
        else -> "問題は見つかりませんでした"
    }

    private fun previewOf(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.length <= PREVIEW_LENGTH) trimmed else trimmed.take(PREVIEW_LENGTH) + "…"
    }

    private fun detectSignals(extractedText: String, qrPayloads: List<String>): List<DetectedSignal> {
        val sourceText = listOf(extractedText, qrPayloads.joinToString(separator = " "))
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")
        val normalized = normalize(sourceText)
        val signals = mutableListOf<DetectedSignal>()

        if (qrPayloads.isNotEmpty()) {
            signals += DetectedSignal(
                label = "QRコード",
                category = RiskCategory.CONTACT_EXCHANGE,
                sample = qrPayloads.firstOrNull(),
            )
        }

        collectSignals(signals, normalized, sourceText, DISCORD_INVITE, "Discord招待リンク", RiskCategory.CONTACT_EXCHANGE)
        collectSignals(signals, normalized, sourceText, LINE_ID, "LINE IDらしき表現", RiskCategory.CONTACT_EXCHANGE)
        collectSignals(signals, normalized, sourceText, PHONE_NUMBER, "電話番号", RiskCategory.CONTACT_EXCHANGE)
        collectSignals(signals, normalized, sourceText, ADDRESS_HINT, "住所らしき表現", RiskCategory.IDENTITY_UNKNOWN)
        collectSignals(signals, normalized, sourceText, SCHOOL_HINT, "学校名らしき表現", RiskCategory.IDENTITY_UNKNOWN)
        collectSignals(signals, normalized, sourceText, CONTACT_PROMPT, "追加や移動の誘導", RiskCategory.CONTACT_EXCHANGE)
        collectSignals(signals, normalized, sourceText, SOCIAL_HANDLE, "SNSアカウントらしき表現", RiskCategory.CONTACT_EXCHANGE)

        return signals.distinctBy { it.label to it.sample }
    }

    private fun collectSignals(
        target: MutableList<DetectedSignal>,
        normalized: String,
        original: String,
        regex: Regex,
        label: String,
        category: RiskCategory,
    ) {
        regex.find(normalized)?.let { match ->
            val sample = original.getOrNull(match.range.first)?.let {
                original.substring(match.range.first.coerceAtMost(original.lastIndex), (match.range.last + 1).coerceAtMost(original.length))
            } ?: match.value
            target += DetectedSignal(label = label, category = category, sample = sample)
        }
    }

    private data class DetectedSignal(
        val label: String,
        val category: RiskCategory,
        val sample: String?,
    )

    private const val PREVIEW_LENGTH = 80

    private fun normalize(input: String): String =
        Normalizer.normalize(input, Normalizer.Form.NFKC).lowercase()

    private val DISCORD_INVITE = Regex("discord(?:app)?\\.com/invite/[a-z0-9-]+|discord\\.gg/[a-z0-9-]+")
    private val LINE_ID = Regex("line\\s*id|lineid|id[:：]\\s*[a-z0-9_.-]{3,}")
    private val PHONE_NUMBER = Regex("[0-9]{2,4}-[0-9]{2,4}-[0-9]{3,4}")
    private val ADDRESS_HINT = Regex("[0-9一二三四五六七八九十]+丁目|[0-9]+番地|[都道府県].+[市区町村]")
    private val SCHOOL_HINT = Regex("[a-z0-9ぁ-んァ-ヶ一-龯]{2,}(小学校|中学校|高校|高等学校|大学)")
    private val CONTACT_PROMPT = Regex("追加して|dmして|個チャ|連絡して|教えて|送って")
    private val SOCIAL_HANDLE = Regex("@[a-z0-9_.]{3,}")
}