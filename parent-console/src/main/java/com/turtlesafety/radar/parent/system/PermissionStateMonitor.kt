package com.turtlesafety.radar.parent.system

import android.content.Context
import android.content.SharedPreferences
import com.turtlesafety.radar.core.log.DetectionLogRepository
import com.turtlesafety.radar.core.risk.DetectionSource
import com.turtlesafety.radar.core.risk.RiskAssessment
import com.turtlesafety.radar.ime.SafetyImePermission
import com.turtlesafety.radar.notif.NotificationListenerPermission

/**
 * 通知監視 / Safety IME の権限が解除されたタイミングを検知し、
 * `DetectionSource.SYSTEM` のログとして残す。
 *
 * 仕様書 §9: 「Safety IMEが無効化された」「通知監視が無効化された」を保護者通知条件として
 * 扱うため、検出を最低限のログに落とし込む。
 *
 * 起動時 (`Application.onCreate`) と Parent Console での権限再診断時に呼ぶ。
 */
class PermissionStateMonitor(
    private val context: Context,
    private val repository: DetectionLogRepository,
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
) {

    /** 現在状態と保存済み状態を比較し、無効化があれば SYSTEM ログを残す。 */
    suspend fun snapshotAndReport() {
        val current = currentSnapshot()
        val previous = storedSnapshot()

        if (previous.notificationListenerEnabled && !current.notificationListenerEnabled) {
            recordRevocation(label = "通知監視が無効化されました")
        }
        if (previous.safetyImeEnabled && !current.safetyImeEnabled) {
            recordRevocation(label = "Safety IME が無効化されました")
        }

        storeSnapshot(current)
    }

    private fun currentSnapshot(): Snapshot = Snapshot(
        notificationListenerEnabled = NotificationListenerPermission.isRadarListenerGranted(context),
        safetyImeEnabled = SafetyImePermission.isEnabled(context),
    )

    private fun storedSnapshot(): Snapshot = Snapshot(
        notificationListenerEnabled = prefs.getBoolean(KEY_NOTIF, false),
        safetyImeEnabled = prefs.getBoolean(KEY_IME, false),
    )

    private fun storeSnapshot(snapshot: Snapshot) {
        prefs.edit()
            .putBoolean(KEY_NOTIF, snapshot.notificationListenerEnabled)
            .putBoolean(KEY_IME, snapshot.safetyImeEnabled)
            .apply()
    }

    private suspend fun recordRevocation(label: String) {
        val assessment = RiskAssessment(
            score = REVOCATION_SCORE,
            categories = emptySet(),
            matches = emptyList(),
            reason = label,
            excerpt = null,
        )
        runCatching {
            repository.record(assessment, DetectionSource.SYSTEM, appName = null)
        }
    }

    private data class Snapshot(
        val notificationListenerEnabled: Boolean,
        val safetyImeEnabled: Boolean,
    )

    companion object {
        private const val PREFS_NAME = "radar_permission_state"
        private const val KEY_NOTIF = "notif_listener_enabled"
        private const val KEY_IME = "safety_ime_enabled"

        /** 取り消し検知は仕様書 §9 で「保護者へ通知」の条件なので高リスク扱い。 */
        const val REVOCATION_SCORE = 4
    }
}
