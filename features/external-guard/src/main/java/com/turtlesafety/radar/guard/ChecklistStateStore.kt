package com.turtlesafety.radar.guard

import android.content.Context
import android.content.SharedPreferences

/**
 * チェックリスト 1 項目あたりの状態を読み書きする最小インタフェース。
 * テスト容易性のためインタフェースとして切り出している。
 */
interface ChecklistStateStore {
    fun get(id: String): ChecklistState?
    fun set(id: String, state: ChecklistState)
    fun clear(id: String)
    fun all(): Map<String, ChecklistState>
}

/**
 * SharedPreferences 1 ファイルにチェック状態をフラットに保存する実装。
 * key prefix:
 *   `c:<id>` = "true"/"false" (Boolean)
 *   `t:<id>` = Long timestamp
 *   `n:<id>` = String note (optional)
 */
internal class SharedPrefsChecklistStateStore(context: Context) : ChecklistStateStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun get(id: String): ChecklistState? {
        if (!prefs.contains(checkedKey(id))) return null
        return ChecklistState(
            checked = prefs.getBoolean(checkedKey(id), false),
            updatedAtMillis = prefs.getLong(timestampKey(id), 0L),
            note = prefs.getString(noteKey(id), null),
        )
    }

    override fun set(id: String, state: ChecklistState) {
        prefs.edit()
            .putBoolean(checkedKey(id), state.checked)
            .putLong(timestampKey(id), state.updatedAtMillis)
            .also { editor -> state.note?.let { editor.putString(noteKey(id), it) } ?: editor.remove(noteKey(id)) }
            .apply()
    }

    override fun clear(id: String) {
        prefs.edit()
            .remove(checkedKey(id))
            .remove(timestampKey(id))
            .remove(noteKey(id))
            .apply()
    }

    override fun all(): Map<String, ChecklistState> {
        val all = prefs.all
        val result = mutableMapOf<String, ChecklistState>()
        for ((key, _) in all) {
            if (!key.startsWith("c:")) continue
            val id = key.substringAfter("c:")
            get(id)?.let { result[id] = it }
        }
        return result
    }

    private fun checkedKey(id: String) = "c:$id"
    private fun timestampKey(id: String) = "t:$id"
    private fun noteKey(id: String) = "n:$id"

    companion object {
        private const val PREFS_NAME = "radar_checklist"
    }
}
