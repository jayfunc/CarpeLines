package io.github.jayfunc.carpelines.lyrics

import io.github.jayfunc.carpelines.lyrics.model.LyricsFormat
import io.github.jayfunc.carpelines.lyrics.model.LyricsParseOptions
import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery
import io.github.jayfunc.carpelines.lyrics.model.LyricsSearchResult
import io.github.jayfunc.carpelines.lyrics.provider.LyricsSource
import io.github.jayfunc.carpelines.lyrics.translate.LyricsTranslationClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BetterLyricsEngineTest {

    private val query = LyricsQuery(
        title = "夜に駆ける",
        artist = "YOASOBI",
        album = "THE BOOK",
        durationMs = 260_000L,
    )

    private class FakeSource(
        override val provider: LyricsProvider,
        private val result: LyricsSearchResult?,
    ) : LyricsSource {
        override suspend fun search(query: LyricsQuery, limit: Int): List<LyricsSearchResult> =
            listOfNotNull(result)
    }

    private class FakeTranslator(private val lines: List<String>) : LyricsTranslationClient {
        override suspend fun translate(lines: List<String>, targetLanguage: String): List<String> = this.lines
    }

    @Test
    fun `load picks the best matching provider`() = runTest {
        val weak = LyricsSearchResult(
            provider = LyricsProvider.APPLE_MUSIC,
            raw = "[00:01.00]wrong song",
            title = "other",
            artist = "other",
            matchPercentage = 12,
        )
        val strong = LyricsSearchResult(
            provider = LyricsProvider.LRCLIB,
            raw = "[00:01.00]夜に駆ける",
            title = query.title,
            artist = query.artist,
            album = query.album,
            durationMs = query.durationMs,
            matchPercentage = 96,
        )

        val engine = BetterLyricsEngine(
            sources = listOf(
                FakeSource(LyricsProvider.APPLE_MUSIC, weak),
                FakeSource(LyricsProvider.LRCLIB, strong),
            ),
        )

        val document = engine.load(query, LyricsParseOptions.Default, minMatchPercentage = 60)

        assertNotNull(document)
        assertEquals(LyricsProvider.LRCLIB, document.provider)
        assertEquals("夜に駆ける", document.lines.single().text)
        assertEquals(96, document.matchPercentage)
    }

    @Test
    fun `load returns null below the threshold`() = runTest {
        val engine = BetterLyricsEngine(
            sources = listOf(
                FakeSource(
                    LyricsProvider.LRCLIB,
                    LyricsSearchResult(
                        provider = LyricsProvider.LRCLIB,
                        raw = "[00:01.00]line",
                        title = "different",
                        matchPercentage = 20,
                    ),
                ),
            ),
        )

        assertNull(engine.load(query, LyricsParseOptions.Default, minMatchPercentage = 60))
    }

    @Test
    fun `merges provider translation and romanization tracks`() = runTest {
        val raw = """
            [00:01.00]<00:01.00>夜<00:01.50>に<00:02.00>駆ける
            [00:04.00]another line
        """.trimIndent()
        val result = LyricsSearchResult(
            provider = LyricsProvider.NETEASE,
            raw = raw,
            translationRaw = """
                [00:01.00]在夜里奔跑
                [00:04.00]另一行
            """.trimIndent(),
            transliterationRaw = """
                [00:01.00]yoru ni kakeru
                [00:04.00]another line
            """.trimIndent(),
            title = query.title,
            artist = query.artist,
            durationMs = query.durationMs,
            matchPercentage = 100,
        )

        val document = BetterLyricsEngine(emptyList()).parse(
            result,
            LyricsParseOptions(translationEnabled = true, romanizationEnabled = true),
        )

        assertEquals(LyricsFormat.ESLRC, document.format)
        assertEquals(2, document.lines.size)
        assertEquals("在夜里奔跑", document.lines[0].translation)
        assertEquals("yoru ni kakeru", document.lines[0].romanization)
        assertTrue(document.lines[0].hasRealSyllableTiming)
        assertEquals(3, document.lines[0].syllables.size)
    }

    @Test
    fun `parse recognizes bracket word timings and keeps them out of the text`() = runTest {
        val raw = """
            [02:13.70]背[02:13.704]对[02:14.074]背
            [02:16.00]下一句
        """.trimIndent()
        val result = LyricsSearchResult(
            provider = LyricsProvider.QQ_MUSIC,
            raw = raw,
            title = query.title,
            artist = query.artist,
            durationMs = query.durationMs,
            matchPercentage = 100,
        )

        val document = BetterLyricsEngine(emptyList()).parse(result, durationMs = query.durationMs)

        assertEquals(LyricsFormat.ESLRC, document.format)
        assertEquals("背对背", document.lines[0].text)
        assertTrue(document.lines[0].hasRealSyllableTiming)
        assertEquals(3, document.lines[0].syllables.size)
    }

    @Test
    fun `falls back to libre translate when no provider translation exists`() = runTest {
        val raw = """
            [00:01.00]夜に駆ける
            [00:04.00]another line
        """.trimIndent()
        val result = LyricsSearchResult(
            provider = LyricsProvider.LRCLIB,
            raw = raw,
            title = query.title,
            artist = query.artist,
            durationMs = query.durationMs,
            matchPercentage = 90,
        )

        val engine = BetterLyricsEngine(
            sources = emptyList(),
            translationClientFactory = { FakeTranslator(listOf("在夜里奔跑", "另一行")) },
        )
        val options = LyricsParseOptions(
            translationEnabled = true,
            libreTranslateEnabled = true,
            libreTranslateServer = "https://libretranslate.example",
            targetTranslationLanguage = "zh",
        )

        val document = engine.parse(result, options)

        assertEquals("在夜里奔跑", document.lines[0].translation)
        assertEquals("zh", document.translationLanguageTag)
    }

    @Test
    fun `parseRaw accepts an imported ttml file`() = runTest {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml"><body>
              <p begin="00:01.000" end="00:03.000">
                <span begin="00:01.000" end="00:02.000">Hello</span>
              </p>
            </body></tt>
        """.trimIndent()

        val document = BetterLyricsEngine(emptyList()).parseRaw(
            raw = ttml,
            provider = LyricsProvider.LOCAL_FILE,
            durationMs = 10_000L,
        )

        assertEquals(LyricsFormat.TTML, document.format)
        assertEquals("Hello", document.lines.single().text)
        assertTrue(document.lines.single().syllables.isNotEmpty())
    }
}
