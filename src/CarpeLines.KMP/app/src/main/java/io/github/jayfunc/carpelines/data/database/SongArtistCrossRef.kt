package io.github.jayfunc.carpelines.data.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.Relation

/**
 * Junction table for many-to-many relationship between songs and artists.
 * Enables multi-artist support where a song can have multiple artists
 * and an artist can have multiple songs.
 */
@Entity(
    tableName = "song_artist_cross_ref",
    primaryKeys = ["song_id", "artist_id"],
    indices = [
        Index(value = ["song_id"]),
        Index(value = ["artist_id"]),
        Index(value = ["is_primary"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongArtistCrossRef(
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "artist_id") val artistId: Long,
    @ColumnInfo(name = "is_primary", defaultValue = "0") val isPrimary: Boolean = false
)

/**
 * Data class for retrieving the primary artist of a song efficiently.
 */
data class PrimaryArtistInfo(
    @ColumnInfo(name = "artist_id") val artistId: Long,
    @ColumnInfo(name = "name") val artistName: String
)
