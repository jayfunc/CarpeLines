package io.github.jayfunc.carpelines.data.external

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExternalMediaStateTest {

    private val media = ExternalMediaState(
        packageName = "com.spotify.music",
        appLabel = "Spotify",
        title = "Night Drive",
        artist = "The Midnight",
        album = "Endless Summer",
        durationMs = 240_000L,
        artworkUri = "content://media/album/1",
    )

    @Test
    fun `identity ignores playback state artwork and duration`() {
        val paused = media.copy(
            isPlaying = false,
            playbackSpeed = 0.5f,
            artworkUri = null,
            durationMs = 300_000L,
        )

        assertEquals(media.identity, paused.identity)
    }

    @Test
    fun `identity changes when the track changes`() {
        assertNotEquals(media.identity, media.copy(title = "Other Song").identity)
        assertNotEquals(media.identity, media.copy(artist = "Other Artist").identity)
        assertNotEquals(media.identity, media.copy(mediaId = "track-2").identity)
    }

    @Test
    fun `toLyricsQuery carries the metadata providers need`() {
        val query = media.toLyricsQuery()

        assertEquals("Night Drive", query.title)
        assertEquals("The Midnight", query.artist)
        assertEquals("Endless Summer", query.album)
        assertEquals(240_000L, query.durationMs)
    }
}
