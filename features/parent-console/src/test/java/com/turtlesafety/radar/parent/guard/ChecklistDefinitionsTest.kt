package com.turtlesafety.radar.parent.guard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistDefinitionsTest {

    @Test
    fun categories_match_spec_v0_4_section_6_count() {
        // 仕様書 §6 は 4 カテゴリ。
        assertEquals(4, ChecklistDefinitions.categories.size)
    }

    @Test
    fun totalItems_counts_all_items_across_categories() {
        val sum = ChecklistDefinitions.categories.sumOf { it.items.size }
        assertEquals(sum, ChecklistDefinitions.totalItems)
    }

    @Test
    fun item_ids_are_unique() {
        val ids = ChecklistDefinitions.categories.flatMap { c -> c.items.map { it.id } }
        assertEquals(
            "重複した checklist id が存在します — 永続化キーが衝突します",
            ids.size,
            ids.toSet().size,
        )
    }

    @Test
    fun knownItemIds_equals_flattened_item_ids() {
        val expected = ChecklistDefinitions.categories
            .flatMap { c -> c.items.map { it.id } }
            .toSet()
        assertEquals(expected, ChecklistDefinitions.knownItemIds)
    }

    @Test
    fun category_ids_are_unique() {
        val ids = ChecklistDefinitions.categories.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun spec_required_categories_are_present() {
        // §6 のカテゴリが揃っているか確認 (id ベースで)。
        val ids = ChecklistDefinitions.categories.map { it.id }.toSet()
        listOf("apps", "guard", "contact", "location").forEach { expected ->
            assertTrue(
                "expected category id '$expected' is missing",
                expected in ids,
            )
        }
    }

    @Test
    fun every_category_has_at_least_one_item() {
        ChecklistDefinitions.categories.forEach { category ->
            assertTrue(
                "category ${category.id} に項目がありません",
                category.items.isNotEmpty(),
            )
        }
    }

    @Test
    fun item_labels_are_non_blank() {
        ChecklistDefinitions.categories.flatMap { it.items }.forEach { item ->
            assertNotNull(item.label)
            assertTrue("item ${item.id} のラベルが空です", item.label.isNotBlank())
        }
    }
}
