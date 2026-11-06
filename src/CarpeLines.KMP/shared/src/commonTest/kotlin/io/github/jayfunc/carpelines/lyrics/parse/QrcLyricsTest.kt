package io.github.jayfunc.carpelines.lyrics.parse

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import io.github.jayfunc.carpelines.lyrics.decrypt.QrcDecrypter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class QrcLyricsTest {

    @Test
    fun `decrypts QQ payload with the QRC DES variant`() {
        val decrypted = QrcDecrypter.decrypt(FIXTURE_HEX)

        val payload = decrypted.orEmpty()
        assertTrue(payload.startsWith("<?xml"), payload.take(40))
        assertTrue(payload.contains("LyricContent="), "missing LyricContent")
        assertTrue(payload.contains("[0,1000]Hel(0,400)"), "missing QRC body")
    }

    @Test
    fun `parses QRC word timings as absolute milliseconds`() {
        val raw = """
            [ti:Test]
            [offset:0]
            [0,1000]Hel(0,400)lo(400,300) (700,300)world(1000,200)
            [2000,1000]Bye(2000,500)
        """.trimIndent()

        val lines = CoreLyricsMapper.toLines(CoreLyricsParser.parse(raw))

        assertEquals(2, lines.size)

        val first = lines[0]
        assertEquals("Hello world", first.text)
        assertEquals(0L, first.startMs)
        assertEquals(1_200L, first.endMs)
        val syllables = first.syllables
        assertEquals(listOf("Hel", "lo", " ", "world"), syllables.map { it.text })
        assertEquals(0L, syllables[0].startMs)
        assertEquals(400L, syllables[0].endMs)
        assertEquals(400L, syllables[1].startMs)
        assertEquals(1_000L, syllables[3].startMs)
        assertEquals(1_200L, syllables[3].endMs)
        assertTrue(first.hasRealSyllableTiming)

        val second = lines[1]
        assertEquals("Bye", second.text)
        assertEquals(2_000L, second.startMs)
        assertEquals(2_500L, second.syllables.single().endMs)
    }

    @Test
    fun `uses the QRC parser instead of syllable or lrc parsers`() {
        val raw = "[2250,2250]词(2250,450)：(2700,450)周(3150,450)"


        val lyrics = CoreLyricsParser.parse(raw)

        val line = assertIs<KaraokeLine.MainKaraokeLine>(lyrics.lines.single())
        assertEquals(2250, line.syllables.first().start)
        assertEquals(3150, line.syllables[2].start)
    }

    private companion object {
        /**
         * zlib-compressed QRC XML encrypted with QQ's D(k1) → E(k2) → D(k3) DES variant,
         * generated with the same algorithm after validating it against a live QQ response.
         */
        const val FIXTURE_HEX =
            "4FDF1933B4F1586F46E0AD6FEE780669DC073BA447C4DFFC9C5617268B3B2CE8" +
                "96CD9DECDAD5187A8305364249E7059428770C37744B74A51E74ABFCDE4951F0" +
                "29C752DE813445843531F521769DDE26339BAC7C638BF0E0AB48B414C35D7B6F" +
                "A1DAFE21A486200D8CCDDAC50BFE8E8F3C6B0E5E0D2521C3F18A57D2BF5A769" +
                "930F511350819A7A68EA5717ED6412E4B782C277DD67C98FD4A7322A609CF33C" +
                "0D8E7F88C71CAEDA53E15582E9349DEFD7DF6724967DCD072C4EFC2714EF0FE1" +
                "CD411AFF0954E484C"
    }
}
