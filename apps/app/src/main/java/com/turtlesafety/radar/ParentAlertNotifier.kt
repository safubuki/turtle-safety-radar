package com.turtlesafety.radar

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.turtlesafety.radar.core.log.DetectionLog
import com.turtlesafety.radar.core.log.DetectionLogRepository
import kotlinx.coroutines.flow.collect

/**
 * 高リスクログをアプリ内の保護者通知へ変換する。
 *
 * 初回導入時は既存ログへ遡って通知せず、以後に追加された高リスクログだけを扱う。
 */
class ParentAlertNotifier(
    context: Context,
    private val repository: DetectionLogRepository,
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    ),
) {

    private val appContext = context.applicationContext

    suspend fun run() {
        createChannel()
        repository.observeHighRisk(limit = 100).collect { logs ->
            handle(logs)
        }
    }

    private fun handle(logs: List<DetectionLog>) {
        if (!prefs.contains(KEY_LAST_NOTIFIED_ID)) {
            val baseline = logs.maxOfOrNull { it.id } ?: 0L
            prefs.edit().putLong(KEY_LAST_NOTIFIED_ID, baseline).apply()
            return
        }

        val lastNotifiedId = prefs.getLong(KEY_LAST_NOTIFIED_ID, 0L)
        var newestHandledId = lastNotifiedId

        logs.asSequence()
            .filter { it.id > lastNotifiedId }
            .sortedBy { it.id }
            .forEach { log ->
                notify(log)
                newestHandledId = log.id
            }

        if (newestHandledId != lastNotifiedId) {
            prefs.edit().putLong(KEY_LAST_NOTIFIED_ID, newestHandledId).apply()
        }
    }

    private fun notify(log: DetectionLog) {
        if (!canPostNotifications()) return

        val text = log.excerpt ?: log.reason
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titleFor(log))
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    buildString {
                        append(sourceLabel(log))
                        if (!text.isNullOrBlank()) {
                            append('\n')
                            append(text)
                        }
                    },
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        runCatching {
            NotificationManagerCompat.from(appContext).notify(log.id.toInt(), builder.build())
        }
    }

    private fun titleFor(log: DetectionLog): String = when (log.source) {
        "SYSTEM" -> "安全設定の変更を検知しました"
        else -> "高リスクを検知しました"
    }

    private fun sourceLabel(log: DetectionLog): String = buildString {
        append(log.source)
        if (!log.appName.isNullOrBlank()) {
            append(" - ")
            append(log.appName)
        }
        if (log.categories.isNotBlank()) {
            append(" [")
            append(log.categories)
            append(']')
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = appContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "保護者通知",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "子ども安全レーダーの高リスク検知通知"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val PREFS_NAME = "radar_parent_alerts"
        private const val KEY_LAST_NOTIFIED_ID = "last_notified_id"
        private const val CHANNEL_ID = "parent_alerts"
    }
}