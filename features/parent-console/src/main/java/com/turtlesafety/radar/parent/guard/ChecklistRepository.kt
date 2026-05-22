package com.turtlesafety.radar.parent.guard

import android.content.Context
import android.content.SharedPreferences

/**
 * External Guard チェックリストの確認状態を SharedPreferences に永続化する。
 *
 * 仕様書 §17.5「保護者が運用状態を確認できる」を満たすための最小ストア。
 * 機密値は持たないため EncryptedSharedPreferences は使わない。
 */
class ChecklistRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isChecked(itemId: String): Boolean = prefs.getBoolean(itemId, false)

    fun setChecked(itemId: String, value: Boolean) {
        prefs.edit().putBoolean(itemId, value).apply()
    }

    /** スナップショット: 既知 id -> bool。未保存項目は false。 */
    fun snapshot(): Map<String, Boolean> =
        ChecklistDefinitions.knownItemIds.associateWith { isChecked(it) }

    /** 完了 / 全体件数のペアを返す。 */
    fun progress(): Progress {
        val snap = snapshot()
        val done = snap.count { it.value }
        return Progress(done = done, total = ChecklistDefinitions.totalItems)
    }

    data class Progress(val done: Int, val total: Int) {
        val ratio: Float get() = if (total == 0) 0f else done.toFloat() / total.toFloat()
    }

    companion object {
        private const val PREFS = "radar_external_guard"
    }
}
