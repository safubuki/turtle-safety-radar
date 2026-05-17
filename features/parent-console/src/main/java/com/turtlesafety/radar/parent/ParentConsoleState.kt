package com.turtlesafety.radar.parent

import com.turtlesafety.radar.core.log.DetectionLog
import com.turtlesafety.radar.core.risk.RiskAssessment
import com.turtlesafety.radar.core.settings.LocalAiMode
import com.turtlesafety.radar.core.settings.Sensitivity
import com.turtlesafety.radar.media.MediaScanResult

/** Parent Console の表示状態。ViewModel が `StateFlow` で提供する。 */
data class ParentConsoleState(
    val gateStatus: GateStatus = GateStatus.UNINITIALIZED,
    val gateError: String? = null,

    val currentScreen: Screen = Screen.Home,

    val recentLogs: List<DetectionLog> = emptyList(),
    val totalLogCount: Int = 0,

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
)
