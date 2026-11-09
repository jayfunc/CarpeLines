package io.github.jayfunc.carpelines.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.jayfunc.carpelines.data.model.Artist
import io.github.jayfunc.carpelines.utils.normalizeMetadataTextOrEmpty

@Entity(
    tableName = "artists",
    indices = [Index(value = ["name"], unique = false)]
)
data class ArtistEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    // Legacy columns from the removed artist-portrait feature. They stay in the schema because
    // dropping them would rebuild a table referenced by two foreign keys; nothing reads or
    // writes them any more.
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "custom_image_uri") val customImageUri: String? = null
)

fun ArtistEntity.toArtist(): Artist {
    return Artist(
        id = this.id,
        name = this.name.normalizeMetadataTextOrEmpty(),
        songCount = this.trackCount
    )
}

fun List<ArtistEntity>.toArtists(): List<Artist> {
    return this.map { it.toArtist() }
}

fun Artist.toEntity(): ArtistEntity {
    return ArtistEntity(
        id = this.id,
        name = this.name,
        trackCount = this.songCount
    )
}
