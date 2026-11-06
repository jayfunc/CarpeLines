package io.github.jayfunc.carpelines.lyrics.pipeline

import io.github.jayfunc.carpelines.lyrics.model.LyricsLine
import io.github.jayfunc.carpelines.lyrics.model.LyricsParseOptions
import io.github.jayfunc.carpelines.lyrics.parse.CoreLyricsMapper
import io.github.jayfunc.carpelines.lyrics.parse.CoreLyricsParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LyricsNormalizerTest {

    @Test
    fun `synthesizes per-character syllables for plain lrc lines`() {
        val raw = """
            [00:01.00]abc
            [00:05.00]next
        """.trimIndent()

        val lines = LyricsNormalizer.normalize(
            CoreLyricsMapper.toLines(CoreLyricsParser.parse(raw)),
            durationMs = 10_000L,
        )

        assertEquals(2, lines.size)
        val first = lines[0]
        assertEquals(5_000L, first.endMs)
        assertEquals(3, first.syllables.size)
        assertEquals(1_000L, first.syllables.first().startMs)
        assertTrue(first.syllables.last().endMs!! > first.syllables.last().startMs)
        assertEquals(0, first.syllables.first().startIndex)
        assertEquals(1, first.syllables[1].startIndex)
    }

    @Test
    fun `fills missing syllable ends from the next syllable`() {
        val lines = LyricsNormalizer.normalize(
            lines = listOf(
                LyricsLine(
                    startMs = 1_000L,
                    text = "hello world",
                    syllables = listOf(
                        io.github.jayfunc.carpelines.lyrics.model.LyricsSyllable("hello ", 1_000L),
                        io.github.jayfunc.carpelines.lyrics.model.LyricsSyllable("world", 1_500L),
                    ),
                    hasRealSyllableTiming = true,
                ),
            ),
            durationMs = 3_000L,
        )

        val line = lines.single()
        assertEquals(3_000L, line.endMs)
        assertEquals(1_500L, line.syllables[0].endMs)
        assertEquals(3_000L, line.syllables[1].endMs)
    }

    @Test
    fun `filters credit lines`() {
        val options = LyricsParseOptions.Default
        assertTrue(LyricsNormalizer.isInfoLine("作词 : 张三", options.infoLineKeywords))
        assertTrue(LyricsNormalizer.isInfoLine("Lyrics by: Someone", options.infoLineKeywords))
        assertEquals(false, LyricsNormalizer.isInfoLine("词不达意", options.infoLineKeywords))

        val lines = LyricsNormalizer.normalize(
            lines = listOf(
                LyricsLine(0L, text = "作词 : 张三"),
                LyricsLine(1_000L, text = "歌词"),
            ),
            durationMs = 2_000L,
        )
        assertEquals(1, lines.size)
        assertEquals("歌词", lines.single().text)
    }
}
