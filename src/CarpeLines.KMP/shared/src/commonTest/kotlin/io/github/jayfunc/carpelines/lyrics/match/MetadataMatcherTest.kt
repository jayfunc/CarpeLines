package io.github.jayfunc.carpelines.lyrics.match

import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery
import io.github.jayfunc.carpelines.lyrics.model.LyricsSearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetadataMatcherTest {

    private val query = LyricsQuery(
        title = "Bohemian Rhapsody",
        artist = "Queen",
        album = "A Night at the Opera",
        durationMs = 355_000L,
    )

    @Test
    fun `identical metadata scores one hundred`() {
        val result = LyricsSearchResult(
            provider = LyricsProvider.LRCLIB,
            title = "Bohemian Rhapsody",
            artist = "Queen",
            album = "A Night at the Opera",
            durationMs = 355_000L,
        )

        assertEquals(100, MetadataMatcher.score(query, result))
    }

    @Test
    fun `duration distance lowers the score`() {
        val near = LyricsSearchResult(
            provider = LyricsProvider.LRCLIB,
            title = query.title,
            artist = query.artist,
            album = query.album,
            durationMs = (query.durationMs ?: 0L) + 5_000L,
        )
        val far = near.copy(durationMs = (query.durationMs ?: 0L) + 30_000L)

        val nearScore = MetadataMatcher.score(query, near)
        val farScore = MetadataMatcher.score(query, far)

        assertTrue(nearScore < 100)
        assertTrue(nearScore > farScore)
    }

    @Test
    fun `jaro winkler rewards shared prefixes`() {
        assertTrue(MetadataMatcher.jaroWinkler("bohemian", "bohemian") == 1.0)
        assertTrue(MetadataMatcher.jaroWinkler("bohemian", "bohemia") > 0.9)
        assertTrue(MetadataMatcher.jaroWinkler("queen", "king") < 0.6)
    }
}
