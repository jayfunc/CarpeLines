package io.github.jayfunc.carpelines.utils

import io.github.jayfunc.carpelines.data.model.BackgroundVocals
import io.github.jayfunc.carpelines.data.model.Lyrics
import io.github.jayfunc.carpelines.data.model.SyncedLine
import io.github.jayfunc.carpelines.data.model.SyncedWord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Contract of the accompanist-lyrics-core-backed parser: timed lines, syllable timing, voices,
 * background vocals, translations and the enhanced-LRC round trip the app persists.
 */
class EnhancedLrcParsingTest {

    private fun parseSynced(lrc: String): List<SyncedLine> =
        requireNotNull(LyricsUtils.parseLyrics(lrc).synced)

    @Test
    fun `plain timed lines keep their text and order`() {
        val lines = parseSynced(
            """
            [00:04.00]Second
            [00:01.00]First
            """.trimIndent()
        )

        assertEquals(listOf("First", "Second"), lines.map { it.line })
        assertEquals(listOf(1_000, 4_000), lines.map { it.time })
    }

    @Test
    fun `word timings keep the rendered text and syllable timings`() {
        val line = parseSynced("[00:01.00]<00:01.00>Hello <00:01.40>world<00:02.00>").single()

        assertEquals("Hello world", line.line)
        assertTrue(line.hasSyllableTiming)
        val words = requireNotNull(line.words)
        assertEquals(listOf("Hello", "world"), words.map { it.word })
        assertEquals(listOf(1_000, 1_400), words.map { it.time })
    }

    @Test
    fun `singer prefixes become voices`() {
        val lines = parseSynced(
            """
            [00:01.00]v1:<00:01.00>Lead
            [00:02.00]v2:<00:02.00>Answer
            """.trimIndent()
        )

        assertEquals(listOf(1, 2), lines.map { it.voice })
    }

    @Test
    fun `background line attaches to the line above`() {
        val line = parseSynced(
            """
            [00:01.00]<00:01.00>Main <00:01.50>line
            [bg:<00:01.10>whis<00:01.50>per<00:02.00>]
            """.trimIndent()
        ).single()

        assertEquals("Main line", line.line)
        assertEquals("whisper", line.background?.text)
        assertEquals(2, requireNotNull(line.background?.words).size)
    }

    @Test
    fun `same timestamp line becomes the translation`() {
        val line = parseSynced(
            """
            [00:01.00]Original
            [00:01.00]译文
            """.trimIndent()
        ).single()

        assertEquals("Original", line.line)
        assertEquals("译文", line.translation)
    }

    @Test
    fun `enhanced lrc export reads back main text voice background and translation`() {
        val source = listOf(
            SyncedLine(
                time = 2_000,
                line = "There's a whisper",
                words = listOf(
                    SyncedWord(2_000, "There's", endTime = 2_600),
                    SyncedWord(2_600, "a", endTime = 3_000),
                    SyncedWord(3_000, "whisper", startsNewWord = true, endTime = 3_800),
                ),
                translation = "Hay un susurro",
                voice = 1,
                hasSyllableTiming = true,
                background = BackgroundVocals(
                    text = "whisper",
                    words = listOf(
                        SyncedWord(4_000, "whis", endTime = 4_400),
                        SyncedWord(4_400, "per", startsNewWord = false, endTime = 5_000),
                    ),
                ),
            ),
        )

        val replayed = parseSynced(LyricsUtils.toEnhancedLrc(source)).single()

        assertEquals("There's a whisper", replayed.line)
        assertEquals("Hay un susurro", replayed.translation)
        assertEquals(1, replayed.voice)
        assertEquals("whisper", replayed.background?.text)
        assertEquals(listOf("There's", "a", "whisper"), replayed.words?.map { it.word })
    }

    @Test
    fun `kugou syllables keep their durations`() {
        val line = parseSynced("[1000,2000]<0,500,0>Hello <500,500,0>world").single()

        assertEquals("Hello world", line.line)
        val words = requireNotNull(line.words)
        assertEquals(listOf(1_000, 1_500), words.map { it.time })
        assertEquals(listOf(1_500, 2_000), words.map { it.endTime })
    }

    @Test
    fun `qrc syllables use line-relative offsets`() {
        val line = parseSynced("[2000,3000](0,500,0)Hel(500,500,0)lo").single()

        assertEquals("Hello", line.line)
        val words = requireNotNull(line.words)
        assertEquals(listOf(2_000, 2_500), words.map { it.time })
        assertEquals(listOf(2_500, 3_000), words.map { it.endTime })
    }

    @Test
    fun `plain lyrics stay plain`() {
        val lyrics = LyricsUtils.parseLyrics("Verse one\nVerse two")

        assertEquals(listOf("Verse one", "Verse two"), lyrics.plain)
        assertTrue(lyrics.synced.isNullOrEmpty())
    }

    @Test
    fun `empty input stays empty`() {
        val lyrics = LyricsUtils.parseLyrics("")

        assertTrue(lyrics.synced.isNullOrEmpty())
        assertTrue(lyrics.plain.isNullOrEmpty())
    }

    @Test
    fun `lrc export keeps the source line readable`() {
        val lines = listOf(
            SyncedLine(time = 10_000, line = "Hello world", translation = "你好世界"),
        )

        val lrc = LyricsUtils.syncedToLrcString(lines)

        assertNotNull(lrc)
        assertEquals(listOf("Hello world"), parseSynced(lrc).map { it.line })
        assertEquals("你好世界", parseSynced(lrc).single().translation)
    }
}
