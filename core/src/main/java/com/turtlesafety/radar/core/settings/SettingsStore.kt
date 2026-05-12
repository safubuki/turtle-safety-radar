package com.turtlesafety.radar.core.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * 保護者が変更する非機微な設定の永続化。
 *
 * - 機密情報 (PIN等) は [com.turtlesafety.radar.core.security.PinManager] が保持。
 * - 監視対象アプリは package name の集合として保存する。表示名は実行時に
 *   [android.content.pm.PackageManager] から解決する想定 (保存しない)。
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var sensitivity: Sensitivity
        get() = Sensitivity.fromName(prefs.getString(KEY_SENSITIVITY, null))
        set(value) {
            prefs.edit().putString(KEY_SENSITIVITY, value.name).apply()
        }

    var monitoredApps: Set<String>
        get() = prefs.getStringSet(KEY_MONITORED_APPS, DEFAULT_MONITORED_APPS) ?: DEFAULT_MONITORED_APPS
        set(value) {
            prefs.edit().putStringSet(KEY_MONITORED_APPS, value).apply()
        }

    fun addMonitoredApp(packageName: String) {
        monitoredApps = monitoredApps + packageName
    }

    fun removeMonitoredApp(packageName: String) {
        monitoredApps = monitoredApps - packageName
    }

    fun isMonitored(packageName: String): Boolean =
        packageName in monitoredApps

    companion object {
        private const val PREFS_NAME = "radar_settings"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_MONITORED_APPS = "monitored_apps"

        /** 仕様書 §5.2 の主要対象。インストール未確認でも安全に保持できる。 */
        val DEFAULT_MONITORED_APPS: Set<String> = setOf(
            "jp.naver.line.android",
            "com.google.android.gm",
            "com.google.android.apps.messaging",
            "com.discord",
            "com.instagram.android",
            "com.twitter.android",
            "com.zhiliaoapp.musically", // TikTok
        )
    }
}
