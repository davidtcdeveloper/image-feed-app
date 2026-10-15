package com.example.imagefeed.core.network

import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionsExtTest {
    data class Item(
        val id: String,
        val name: String,
    )

    @Test
    fun filterDistinctAgainstEmptyExisting() {
        val incoming = listOf(Item("1", "A"), Item("2", "B"))
        val existing = emptyList<Item>()

        val result = incoming.filterDistinctAgainst(existing) { it.id }
        assertEquals(listOf(Item("1", "A"), Item("2", "B")), result)
    }

    @Test
    fun filterDistinctAgainstFiltersExistingItems() {
        val incoming = listOf(Item("1", "A"), Item("2", "B"), Item("3", "C"))
        val existing = listOf(Item("2", "B_Old"))

        val result = incoming.filterDistinctAgainst(existing) { it.id }
        assertEquals(listOf(Item("1", "A"), Item("3", "C")), result)
    }

    @Test
    fun filterDistinctAgainstFiltersIntraDuplicates() {
        val incoming = listOf(Item("1", "A"), Item("1", "A_dup"), Item("2", "B"))
        val existing = emptyList<Item>()

        val result = incoming.filterDistinctAgainst(existing) { it.id }
        assertEquals(listOf(Item("1", "A"), Item("2", "B")), result)
    }

    @Test
    fun filterDistinctAgainstWhenAllExistReturnsEmpty() {
        val incoming = listOf(Item("1", "A"), Item("2", "B"))
        val existing = listOf(Item("1", "A"), Item("2", "B"), Item("3", "C"))

        val result = incoming.filterDistinctAgainst(existing) { it.id }
        assertEquals(emptyList(), result)
    }
}
