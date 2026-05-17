package com.turtlesafety.radar.watcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

/**
 * RadarAccessibilityService の有効化状態を確認するヘルパー。
 *
 * 入力監視は子ども側の能動操作を不要にする中核機構なので、
 * 親が初期設定でこれを有効化する必要がある。
 */
object AccessibilityWatcherPermission {

    private const val ENABLED_SERVICES_KEY = "enabled_accessibility_services"

    fun isEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, ENABLED_SERVICES_KEY)
            ?: return false
        val expected = component(context).flattenToString()
        val expectedShort = component(context).flattenToShortString()
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(flat) }
        for (token in splitter) {
            if (token == expected || token == expectedShort) return true
        }
        return false
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun component(context: Context): ComponentName =
        ComponentName(context, RadarAccessibilityService::class.java)
}
