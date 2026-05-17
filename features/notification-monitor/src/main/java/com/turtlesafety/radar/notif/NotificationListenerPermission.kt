package com.turtlesafety.radar.notif

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * 通知監視権限の状態確認と設定画面遷移。
 *
 * Android は通知監視権限を [Settings.Secure] の `enabled_notification_listeners` に
 * コンポーネント名コロン区切りで保持しているため、これを直接参照する。
 */
object NotificationListenerPermission {

    private const val SECURE_KEY = "enabled_notification_listeners"

    /** 任意のリスナーコンポーネントが有効か。 */
    fun isGranted(context: Context, component: ComponentName): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, SECURE_KEY) ?: return false
        val expectedFlat = component.flattenToString()
        val expectedShort = component.flattenToShortString()
        return flat.split(":").any { it == expectedFlat || it == expectedShort }
    }

    /** 本アプリの [RadarNotificationListenerService] が有効か。 */
    fun isRadarListenerGranted(context: Context): Boolean =
        isGranted(context, ComponentName(context, RadarNotificationListenerService::class.java))

    /** 通知アクセス設定画面を開く。Activity 外から呼ぶ場合は NEW_TASK を付与する。 */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
