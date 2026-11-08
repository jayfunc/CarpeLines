package io.github.jayfunc.carpelines.data.external

import android.graphics.Bitmap
import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery

/**
 * Snapshot of the media session Android is currently playing outside CarpeLines.
 *
 * Android has no SMTC; the equivalent is a notification listener that can read other apps'
 * [android.media.session.MediaSession] through [android.media.session.MediaSessionManager].
 */
data class ExternalMediaState(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long?,
    val artworkUri: String? = null,
    val artworkBitmap: Bitmap? = null,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1f,
    val mediaId: String? = null,
) {
    /** Stable enough to avoid reloading lyrics on every pause/play or artwork update. */
    val identity: String
        get() = listOf(packageName, mediaId.orEmpty(), title, artist, album)
            .joinToString("|")
            .lowercase()

    val hasMetadata: Boolean
        get() = title.isNotBlank()

    fun toLyricsQuery(): LyricsQuery = LyricsQuery(
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        songId = mediaId,
    )
}

/**
 * A player seen publishing a media session on the device, whether or not the player filter lets
 * CarpeLines follow it. The player filter screen lists these so an ignored player can be selected.
 */
data class ExternalPlayerCandidate(
    val packageName: String,
    val appLabel: String,
    val isPlaying: Boolean,
)

