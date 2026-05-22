package com.turtlesafety.radar.parent

import com.turtlesafety.radar.core.log.DetectionLog
import com.turtlesafety.radar.core.risk.RiskAssessment
import com.turtlesafety.radar.core.settings.LocalAiMode
import com.turtlesafety.radar.core.settings.Sensitivity
import com.turtlesafety.radar.media.MediaScanResult
import com.turtlesafety.radar.parent.guard.ChecklistRepository

/** Parent Console の表示状態。ViewModel が `StateFlow` で提供する。 */
data class ParentConsoleState(
    val gateStatus: GateStatus = GateStatus.UNINITIALIZED,
    val gateError: String? = null,

    val currentScreen: Screen = Screen.Home,

    val recentLogs: List<DetectionLog> = emptyList(),
    val totalLogCount: Int = 0,
    val unacknowledgedLogCount: Int = 0,

    val sensitivity: Sensitivity = Sensitivity.NORMAL,
    val monitoredApps: Set<String> = emptySet(),
    val mediaCheckerEnabled: Boolean = true,
    val localAiMode: LocalAiMode = LocalAiMode.BASIC,

    val notificationListenerEnabled: Boolean = false,
    val safetyImeEnabled: Boolean = false,
    val safetyImeIsDefault: Boolean = false,
    val parentNotificationsEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,

    val mediaScanInProgress: Boolean = false,
    val lastMediaScanResult: MediaScanResult? = null,
    val mediaScanError: String? = null,
    val quickCheckAssessment: RiskAssessment? = null,
    val quickCheckError: String? = null,

    val selfTestReport: String? = null,

    /** External Guard チェック項目: id -> 確認済み。 */
    val checklistState: Map<String, Boolean> = emptyMap(),
    val checklistProgress: ChecklistRepository.Progress =
        ChecklistRepository.Progress(done = 0, total = 0),
)
