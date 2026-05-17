package com.turtlesafety.radar.guard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistRepositoryTest {

    private class FakeStore : ChecklistStateStore {
        private val map = mutableMapOf<String, ChecklistState>()
        override fun get(id: String): ChecklistState? = map[id]
        override fun set(id: String, state: ChecklistState) { map[id] = state }
        override fun clear(id: String) { map.remove(id) }
        override fun all(): Map<String, ChecklistState> = map.toMap()
    }

    private fun newRepo(now: Long = 1_700_000_000_000L): Pair<ChecklistRepository, FakeStore> {
        val store = FakeStore()
        val repo = ChecklistRepository(store, clock = { now })
        return repo to store
    }

    @Test
    fun `snapshot exposes every item with null state by default`() {
        val (repo, _) = newRepo()
        val snapshot = repo.snapshot()
        val total = snapshot.values.sumOf { it.size }
        assertEquals(ChecklistDefinitions.items.size, total)
        assertTrue(snapshot.values.flatten().all { it.state == null && !it.isChecked })
    }

    @Test
    fun `setChecked stores boolean and timestamp`() {
        val (repo, store) = newRepo(now = 42L)
        repo.setChecked("app.line", checked = true)

        val state = store.get("app.line")
        assertEquals(true, state?.checked)
        assertEquals(42L, state?.updatedAtMillis)
        assertNull(state?.note)
    }

    @Test
    fun `setChecked preserves existing note`() {
        val (repo, store) = newRepo(now = 100L)
        repo.setNote("app.browser", "Brave のみ許可")
        repo.setChecked("app.browser", checked = true)

        val state = store.get("app.browser")
        assertEquals("Brave のみ許可", state?.note)
        assertEquals(true, state?.checked)
    }

    @Test
    fun `progress counts checked items`() {
        val (repo, _) = newRepo()
        assertEquals(0, repo.progress().checked)
        repo.setChecked("app.line", true)
        repo.setChecked("set.app_install", true)
        repo.setChecked("app.line", false)
        assertEquals(1, repo.progress().checked)
        assertEquals(ChecklistDefinitions.items.size, repo.progress().total)
    }

    @Test
    fun `unknown id throws`() {
        val (repo, _) = newRepo()
        var threw = false
        try { repo.setChecked("nonsense", true) } catch (e: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun `entry isChecked false for unset items`() {
        val (repo, _) = newRepo()
        val snapshot = repo.snapshot()
        val unset = snapshot.values.flatten().first()
        assertFalse(unset.isChecked)
    }
}
