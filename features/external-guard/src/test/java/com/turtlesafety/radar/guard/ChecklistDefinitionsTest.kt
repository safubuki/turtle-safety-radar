package com.turtlesafety.radar.guard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistDefinitionsTest {

    @Test
    fun `all spec sections are represented`() {
        val cats = ChecklistDefinitions.items.map { it.category }.toSet()
        assertEquals(ChecklistCategory.entries.toSet(), cats)
    }

    @Test
    fun `item ids are unique and non blank`() {
        val ids = ChecklistDefinitions.items.map { it.id }
        assertEquals("ids must be unique", ids.size, ids.toSet().size)
        assertTrue(ids.all { it.isNotBlank() })
    }

    @Test
    fun `byId returns the right item or null`() {
        val first = ChecklistDefinitions.items.first()
        assertEquals(first, ChecklistDefinitions.byId(first.id))
        assertNotNull(ChecklistDefinitions.byId("app.line"))
        assertEquals(null, ChecklistDefinitions.byId("does.not.exist"))
    }

    @Test
    fun `total item count matches spec section sums (7+5+5+4 = 21)`() {
        assertEquals(21, ChecklistDefinitions.items.size)
        assertEquals(7, ChecklistDefinitions.byCategory.getValue(ChecklistCategory.APP_RESTRICTIONS).size)
        assertEquals(5, ChecklistDefinitions.byCategory.getValue(ChecklistCategory.SETTINGS_LOCKDOWN).size)
        assertEquals(5, ChecklistDefinitions.byCategory.getValue(ChecklistCategory.COMMUNICATION).size)
        assertEquals(4, ChecklistDefinitions.byCategory.getValue(ChecklistCategory.LOCATION_AND_OUTING).size)
    }
}
