package io.github.jayfunc.carpelines.data.betterlyrics

import io.github.jayfunc.carpelines.lyrics.model.LyricsDocument
import io.github.jayfunc.carpelines.lyrics.model.LyricsFormat
import io.github.jayfunc.carpelines.lyrics.model.LyricsLine
import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsSyllable
import io.github.jayfunc.carpelines.lyrics.parse.CoreLyricsMapper
import io.github.jayfunc.carpelines.lyrics.parse.CoreLyricsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BetterLyricsMapperTest {

    private fun document(
        lines: List<LyricsLine>,
        isSynced: Boolean = true,
    ) = LyricsDocument(
        lines = lines,
        format = LyricsFormat.ESLRC,
        provider = LyricsProvider.LRCLIB,
        isSynced = isSynced,
    )

    @Test
    fun `maps syllables to synced words and re-clusters at whitespace`() {
        val line = LyricsLine(
            startMs = 1_000L,
            endMs = 3_000L,
            text = "Hello world",
            syllables = listOf(
                LyricsSyllable(text = "Hello ", startMs = 1_000L, endMs = 1_500L, startIndex = 0),
                LyricsSyllable(text = "world", startMs = 1_500L, endMs = 3_000L, startIndex = 6),
            ),
            hasRealSyllableTiming = true,
            translation = "你好世界",
            romanization = "haro warudo",
        )

        val lyrics = BetterLyricsMapper.toPlayerLyrics(document(listOf(line)), fromRemote = true)
        val synced = lyrics.synced

        assertNotNull(synced)
        assertEquals(1, synced!!.size)
        val words = synced.single().words
        assertNotNull(words)
        assertEquals(listOf("Hello", "world"), words!!.map { it.word })
        assertTrue(words[0].startsNewWord)
        assertTrue(words[1].startsNewWord)
        assertEquals(1_000, words[0].time)
        assertEquals(1_500, words[0].endTime)
        assertEquals("你好世界", synced.single().translation)
        assertEquals("haro warudo", synced.single().romanization)
        assertTrue(synced.single().hasSyllableTiming)
    }

    @Test
    fun `synthesized plain lrc syllables are not marked as real word timings`() {
        val line = LyricsLine(
            startMs = 1_000L,
            endMs = 3_000L,
            text = "Hello world",
            syllables = listOf(
                LyricsSyllable("H", 1_000L, 1_083L, 0),
                LyricsSyllable("e", 1_083L, 1_166L, 1),
            ),
            hasRealSyllableTiming = false,
        )

        val synced = BetterLyricsMapper.toPlayerLyrics(document(listOf(line))).synced!!.single()

        assertNotNull(synced.words)
        assertEquals(false, synced.hasSyllableTiming)
    }

    @Test
    fun `keeps cjk characters in one cluster`() {
        val line = LyricsLine(
            startMs = 0L,
            text = "夜に駆ける",
            syllables = listOf(
                LyricsSyllable("夜", 0L, 500L, 0),
                LyricsSyllable("に", 500L, 800L, 1),
                LyricsSyllable("駆ける", 800L, 1_500L, 2),
            ),
            hasRealSyllableTiming = true,
        )

        val words = BetterLyricsMapper.toPlayerLyrics(document(listOf(line)), fromRemote = true)
            .synced!!.single().words!!

        assertEquals(3, words.size)
        // No whitespace anywhere: the whole line stays one wrapping cluster.
        assertTrue(words.first().startsNewWord)
        assertEquals(false, words[1].startsNewWord)
        assertEquals(false, words[2].startsNewWord)
    }

    @Test
    fun `maps background vocals`() {
        val line = LyricsLine(
            startMs = 1_000L,
            text = "Main line",
            syllables = listOf(LyricsSyllable("Main line", 1_000L, 2_000L, 0)),
            hasRealSyllableTiming = true,
            backgroundText = "(whisper)",
            backgroundSyllables = listOf(LyricsSyllable("(whisper)", 1_100L, 1_900L, 0)),
        )

        val synced = BetterLyricsMapper.toPlayerLyrics(document(listOf(line))).synced!!.single()

        assertEquals("(whisper)", synced.background?.text)
        assertEquals("(whisper)", synced.background?.words?.single()?.word)
    }

    @Test
    fun `keeps plain lyrics for unsynced documents`() {
        val doc = LyricsDocument(
            lines = listOf(
                LyricsLine(startMs = 0L, text = "first"),
                LyricsLine(startMs = 0L, text = "second"),
            ),
            format = LyricsFormat.PLAIN,
            provider = LyricsProvider.LOCAL_FILE,
            isSynced = false,
        )

        val lyrics = BetterLyricsMapper.toPlayerLyrics(doc, fromRemote = false)

        assertEquals(listOf("first", "second"), lyrics.plain)
        assertNull(lyrics.synced)
    }

    @Test
    fun `enhanced lrc round-trips through the shared parser`() {
        val line = LyricsLine(
            startMs = 1_000L,
            endMs = 3_000L,
            text = "Hello world",
            syllables = listOf(
                LyricsSyllable("Hello ", 1_000L, 1_500L, 0),
                LyricsSyllable("world", 1_500L, 3_000L, 6),
            ),
            hasRealSyllableTiming = true,
            voice = 1,
            backgroundText = "(echo)",
            backgroundSyllables = listOf(LyricsSyllable("(echo)", 1_200L, 1_800L, 0)),
        )

        val lrc = BetterLyricsMapper.toEnhancedLrc(document(listOf(line)))
        val parsed = CoreLyricsMapper.toLines(CoreLyricsParser.parse(lrc)).single()

        assertEquals("Hello world", parsed.text)
        assertEquals(1, parsed.voice)
        assertEquals(2, parsed.syllables.size)
        assertEquals("echo", parsed.backgroundText)
        assertEquals(1_000L, parsed.startMs)
    }

    @Test
    fun `enhanced lrc export keeps the translation readable by the parser`() {
        val line = LyricsLine(
            startMs = 1_000L,
            endMs = 3_000L,
            text = "Hello world",
            syllables = listOf(
                LyricsSyllable("Hello ", 1_000L, 1_500L, 0),
                LyricsSyllable("world", 1_500L, 3_000L, 6),
            ),
            hasRealSyllableTiming = true,
            translation = "你好世界",
        )

        val lrc = BetterLyricsMapper.toEnhancedLrc(document(listOf(line)))
        val parsed = CoreLyricsMapper.toLines(CoreLyricsParser.parse(lrc)).single()

        assertEquals("Hello world", parsed.text)
        assertEquals("你好世界", parsed.translation)
    }
}
