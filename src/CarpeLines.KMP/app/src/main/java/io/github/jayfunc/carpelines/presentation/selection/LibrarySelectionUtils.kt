package io.github.jayfunc.carpelines.presentation.selection

import io.github.jayfunc.carpelines.data.model.StorageFilter

/** Mirrors the filter used by the visible library when local media is globally hidden. */
internal fun effectiveLibraryStorageFilter(
    selected: StorageFilter,
    hideLocalMedia: Boolean,
): StorageFilter = if (hideLocalMedia) StorageFilter.ONLINE else selected

/**
 * Flattens resolved album/artist song groups without performing a destructive action twice when
 * the same song belongs to more than one selected artist.
 */
internal fun <T, K> flattenDistinctGroups(
    groups: Iterable<Iterable<T>>,
    keyOf: (T) -> K
): List<T> {
    val result = mutableListOf<T>()
    val seenKeys = hashSetOf<K>()
    groups.forEach { group ->
        group.forEach { item ->
            if (seenKeys.add(keyOf(item))) {
                result += item
            }
        }
    }
    return result
}
