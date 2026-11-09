package io.github.jayfunc.carpelines.presentation.selection

import io.github.jayfunc.carpelines.data.model.StorageFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LibrarySelectionUtilsTest {

    private data class Item(val id: Long, val label: String)

    @Test
    fun `hidden local media forces online filter for category batch actions`() {
        assertEquals(
            StorageFilter.ONLINE,
            effectiveLibraryStorageFilter(StorageFilter.ALL, hideLocalMedia = true),
        )
        assertEquals(
            StorageFilter.OFFLINE,
            effectiveLibraryStorageFilter(StorageFilter.OFFLINE, hideLocalMedia = false),
        )
    }

    @Test
    fun `flattenDistinctGroups keeps category and song order while removing overlaps`() {
        val first = Item(1, "First")
        val shared = Item(2, "Shared")
        val last = Item(3, "Last")

        val resolved = flattenDistinctGroups(
            groups = listOf(
                listOf(first, shared),
                listOf(shared.copy(label = "Shared duplicate"), last)
            ),
            keyOf = Item::id
        )

        assertEquals(listOf(first, shared, last), resolved)
    }
}
