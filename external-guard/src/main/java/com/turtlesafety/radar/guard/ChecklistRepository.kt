package com.turtlesafety.radar.guard

import android.content.Context

/**
 * External Guard のチェックリスト用 Repository。
 *
 * - 項目定義は [ChecklistDefinitions] が保持する不変データ。
 * - 状態 (checked / note / updatedAt) は [ChecklistStateStore] が永続化する。
 *
 * UI 層 (Parent Console) はこの Repository 1 つに依存すれば、
 * チェックリストの表示・更新・進捗集計を完結できる。
 */
class ChecklistRepository(
    private val store: ChecklistStateStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    /** カテゴリごとの項目と現在の状態を組み合わせて返す。 */
    fun snapshot(): Map<ChecklistCategory, List<ChecklistEntry>> {
        val states = store.all()
        return ChecklistDefinitions.byCategory.mapValues { (_, items) ->
            items.map { item ->
                ChecklistEntry(
                    item = item,
                    state = states[item.id],
                )
            }
        }
    }

    fun setChecked(id: String, checked: Boolean) {
        require(ChecklistDefinitions.byId(id) != null) { "Unknown checklist id: $id" }
        val previousNote = store.get(id)?.note
        store.set(
            id = id,
            state = ChecklistState(
                checked = checked,
                updatedAtMillis = clock(),
                note = previousNote,
            ),
        )
    }

    fun setNote(id: String, note: String?) {
        require(ChecklistDefinitions.byId(id) != null) { "Unknown checklist id: $id" }
        val current = store.get(id) ?: ChecklistState(
            checked = false,
            updatedAtMillis = clock(),
            note = null,
        )
        store.set(id = id, state = current.copy(note = note, updatedAtMillis = clock()))
    }

    fun progress(): Progress {
        val states = store.all()
        val total = ChecklistDefinitions.items.size
        val checked = ChecklistDefinitions.items.count { states[it.id]?.checked == true }
        return Progress(checked = checked, total = total)
    }

    data class ChecklistEntry(
        val item: ChecklistItem,
        val state: ChecklistState?,
    ) {
        val isChecked: Boolean get() = state?.checked == true
    }

    data class Progress(val checked: Int, val total: Int) {
        val ratio: Float get() = if (total == 0) 0f else checked.toFloat() / total.toFloat()
    }

    companion object {
        fun create(context: Context): ChecklistRepository =
            ChecklistRepository(SharedPrefsChecklistStateStore(context))
    }
}
