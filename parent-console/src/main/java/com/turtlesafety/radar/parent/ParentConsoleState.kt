package com.turtlesafety.radar.parent

import com.turtlesafety.radar.core.log.DetectionLog
import com.turtlesafety.radar.core.settings.Sensitivity

/**
 * Parent Console の表示状態。ViewModel が `StateFlow` で提供する。
 *
 * チェックリスト状態は ChecklistRepository から都度取得するので、
 * 再読み込み用に [checklistRevision] を持つ (インクリメントで再 read を促す)。
 */
data class ParentConsoleState(
    val gateStatus: GateStatus = GateStatus.UNINITIALIZED,
    val gateError: String? = null,

    val currentScreen: Screen = Screen.Home,

    val recentLogs: List<DetectionLog> = emptyList(),
    val totalLogCount: Int = 0,

    val sensitivity: Sensitivity = Sensitivity.NORMAL,
    val monitoredApps: Set<String> = emptySet(),

    val notificationListenerEnabled: Boolean = false,
    val safetyImeEnabled: Boolean = false,
    val safetyImeIsDefault: Boolean = false,

    val checklistRevision: Int = 0,
    val checklistCheckedCount: Int = 0,
    val checklistTotal: Int = 0,
)
