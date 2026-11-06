package io.github.jayfunc.carpelines.data.carpelines

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.jayfunc.carpelines.lyrics.model.LyricsDocument
import io.github.jayfunc.carpelines.lyrics.model.LyricsFormat
import io.github.jayfunc.carpelines.lyrics.model.LyricsLine
import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsSyllable
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class ManualLyricsStoreTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun saveLoadAndClear_roundTripsTheDocument() = runTest {
        val directory = Files.createTempDirectory("manual-lyrics").toFile()
        val store = createStore(directory)
        val document = LyricsDocument(
            lines = listOf(
                LyricsLine(
                    startMs = 1_000L,
                    text = "hello",
                    syllables = listOf(LyricsSyllable(text = "hello", startMs = 1_000L)),
                    hasRealSyllableTiming = true,
                    translation = "你好",
                    romanization = "ni hao",
                ),
                LyricsLine(startMs = 4_000L, text = "world"),
            ),
            format = LyricsFormat.ESLRC,
            provider = LyricsProvider.QQ_MUSIC,
            languageTag = "en",
            isSynced = true,
            durationMs = 10_000L,
        )

        val saved = store.save(
            title = "Song",
            artist = "Artist",
            album = "Album",
            selection = ManualLyricsSelection(
                document = document,
                mappedTitle = "Song (Live)",
                mappedArtist = "Artist",
                mappedAlbum = "Album",
                providerLabel = "Embedded lyrics",
            ),
        )
        val loaded = store.load("Song", "Artist", "Album")

        assertThat(loaded).isNotNull()
        assertThat(loaded!!.document.provider).isEqualTo(LyricsProvider.QQ_MUSIC)
        assertThat(loaded.document.lines).hasSize(2)
        assertThat(loaded.document.lines.first().translation).isEqualTo("你好")
        assertThat(loaded.document.lines.first().syllables).hasSize(1)
        assertThat(loaded.mappedTitle).isEqualTo("Song (Live)")
        assertThat(loaded.providerLabel).isEqualTo("Embedded lyrics")
        assertThat(loaded.savedAtEpochMs).isEqualTo(saved.savedAtEpochMs)

        store.clear("Song", "Artist", "Album")
        assertThat(store.load("Song", "Artist", "Album")).isNull()

        directory.deleteRecursively()
    }

    @Test
    fun keyFor_ignoresCaseAndOuterWhitespaceButKeepsAlbum() {
        val directory = Files.createTempDirectory("manual-lyrics-key").toFile()
        val store = createStore(directory)

        val base = store.keyFor(" Song ", " Artist ", " Album ")
        assertThat(store.keyFor("song", "artist", "album")).isEqualTo(base)
        assertThat(store.keyFor("song", "artist", "other album")).isNotEqualTo(base)

        directory.deleteRecursively()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun saveAndClear_emitChangesForTheTrackKey() = runTest {
        val directory = Files.createTempDirectory("manual-lyrics-events").toFile()
        val store = createStore(directory)
        val selection = ManualLyricsSelection(
            document = LyricsDocument(
                lines = listOf(LyricsLine(startMs = 0L, text = "line")),
                format = LyricsFormat.LRC,
                provider = LyricsProvider.LRCLIB,
            ),
        )
        val expectedKey = store.keyFor("Song", "Artist", "Album")

        store.changes.test {
            runCurrent()
            store.save("Song", "Artist", "Album", selection)
            val saved = awaitItem()
            assertThat(saved).isInstanceOf(ManualLyricsChange.Selected::class.java)
            assertThat(saved.key).isEqualTo(expectedKey)

            store.clear("Song", "Artist", "Album")
            val cleared = awaitItem()
            assertThat(cleared).isInstanceOf(ManualLyricsChange.Cleared::class.java)
            assertThat(cleared.key).isEqualTo(expectedKey)

            cancelAndIgnoreRemainingEvents()
        }

        directory.deleteRecursively()
    }

    private fun createStore(directory: java.io.File): ManualLyricsStore {
        val context = mockk<Context>(relaxed = true)
        every { context.filesDir } returns directory
        return ManualLyricsStore(context, json)
    }
}
