package com.turtlesafety.radar.media

import com.turtlesafety.radar.core.risk.DetectionSource
import com.turtlesafety.radar.core.risk.RiskAssessment

data class MediaSignal(
    val label: String,
    val sample: String? = null,
)

data class MediaScanResult(
    val source: DetectionSource,
    val assessment: RiskAssessment,
    val extractedTextPreview: String?,
    val qrPayloads: List<String>,
    val signals: List<MediaSignal>,
    val warningMessage: String,
) {
    val score: Int get() = assessment.score
}