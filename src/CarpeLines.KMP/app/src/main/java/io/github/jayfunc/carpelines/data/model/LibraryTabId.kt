package io.github.jayfunc.carpelines.data.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import io.github.jayfunc.carpelines.R

@Immutable
enum class LibraryTabId(
    val storageKey: String,
    @StringRes val titleRes: Int,
    val defaultSort: SortOption
) {
    SONGS("SONGS", R.string.library_tab_songs, SortOption.SongTitleAZ),
    ALBUMS("ALBUMS", R.string.library_tab_albums, SortOption.AlbumTitleAZ),
    ARTISTS("ARTIST", R.string.library_tab_artists, SortOption.ArtistNameAZ),
    FOLDERS("FOLDERS", R.string.library_tab_folders, SortOption.FolderNameAZ),
    GENRES("GENRES", R.string.library_tab_genres, SortOption.GenreNameAZ);

    companion object {
        fun fromStorageKey(key: String): LibraryTabId =
            entries.firstOrNull { it.storageKey == key } ?: SONGS
    }
}

fun String.toLibraryTabIdOrNull(): LibraryTabId? =
    LibraryTabId.entries.firstOrNull { it.storageKey == this }