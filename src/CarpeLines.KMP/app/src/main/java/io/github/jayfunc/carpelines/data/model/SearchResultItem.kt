package io.github.jayfunc.carpelines.data.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface SearchResultItem {
    data class SongItem(val song: Song) : SearchResultItem
    data class AlbumItem(val album: Album) : SearchResultItem
    data class ArtistItem(val artist: Artist) : SearchResultItem
}
