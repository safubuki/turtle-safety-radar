package com.turtlesafety.radar.notif

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.turtlesafety.radar.core.Radar

/**
 * 仕様書 §5.2 の通知監視。OS から通知が posted されるたびに [NotificationProcessor] を呼ぶ。
 *
 * 制約:
 * - 通知本文が非表示 (lock screen で隠れている等) の場合は EXTRA_TEXT が取れない可能性がある。
 * - 自分自身のアプリの通知 (今後 Phase 6 で出す警告通知) は無視する。
 */
class RadarNotificationListenerService : NotificationListenerService() {

    private var processor: NotificationProcessor? = null

    override fun onCreate() {
        super.onCreate()
        val services = Radar.services()
        processor = NotificationProcessor(
            riskEngine = services.riskEngine,
            isMonitored = services.settingsStore::isMonitored,
            repository = services.detectionLogRepository,
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val event = sbn ?: return
        if (event.packageName == applicationContext.packageName) return

        val extras = event.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        processor?.process(event.packageName, title, text)
    }
}
