package com.turtlesafety.radar

import android.content.Context
import android.content.SharedPreferences
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 起動時のサイレントクラッシュを可視化するための簡易レコーダー。
 *
 * - Default UncaughtExceptionHandler でフックしてスタックトレースを SharedPreferences に保存
 * - 次回起動時に [MainActivity] が読み出して画面表示できる
 *
 * 子ども側端末で adb logcat を取れないユーザー向けのフォールバック。
 */
object CrashRecorder {

    private const val PREFS = "radar_crash"
    private const val KEY_LAST = "last_crash"
    private const val KEY_LAST_AT = "last_crash_at"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { record(app, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun record(context: Context, throwable: Throwable) {
        val sw = StringWriter()
        PrintWriter(sw).use { throwable.printStackTrace(it) }
        prefs(context).edit()
            .putString(KEY_LAST, sw.toString())
            .putLong(KEY_LAST_AT, System.currentTimeMillis())
            .apply()
    }

    fun lastCrash(context: Context): Pair<String, Long>? {
        val p = prefs(context)
        val text = p.getString(KEY_LAST, null) ?: return null
        val at = p.getLong(KEY_LAST_AT, 0L)
        return text to at
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_LAST).remove(KEY_LAST_AT).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
