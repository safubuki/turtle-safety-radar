package com.turtlesafety.radar.ai

import com.turtlesafety.radar.core.settings.LocalAiMode

data class LocalAiModeDescription(
    val title: String,
    val summary: String,
    val note: String,
)

object LocalAiModeDescriptions {
    fun describe(mode: LocalAiMode): LocalAiModeDescription = when (mode) {
        LocalAiMode.BASIC -> LocalAiModeDescription(
            title = "Basic",
            summary = "ルールベース判定のみを使います。",
            note = "全端末で安定して動作します。",
        )
        LocalAiMode.STANDARD -> LocalAiModeDescription(
            title = "Standard",
            summary = "軽量な文脈補正で誤検知を減らしつつ見逃しも補います。",
            note = "現時点での推奨モードです。",
        )
        LocalAiMode.LOCAL_LLM -> LocalAiModeDescription(
            title = "Local LLM",
            summary = "将来のローカル LLM 拡張に備えたモードです。",
            note = "現在は Standard の補正へフォールバックします。",
        )
        LocalAiMode.CLOUD_ASSIST -> LocalAiModeDescription(
            title = "Cloud Assist",
            summary = "将来のクラウド補助判定に備えたモードです。",
            note = "現在は Standard の補正へフォールバックします。",
        )
    }
}