package io.github.jayfunc.carpelines.data.carpelines

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.github.jayfunc.carpelines.data.betterlyrics.LyricsSourceId
import io.github.jayfunc.carpelines.data.database.MusicDao
import io.github.jayfunc.carpelines.data.model.Song
import io.mockk.mockk
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class LocalLyricsResolverTest {

    @Test
    fun findLocalRaw_readsSiblingLrcWhenEnabled() = runTest {
        val directory = Files.createTempDirectory("local-lyrics").toFile()
        val audio = File(directory, "Song.mp3").apply { writeBytes(ByteArray(0)) }
        File(directory, "Song.lrc").writeText("[00:01.00]hello")
        val song = Song.emptySong().copy(
            id = "1",
            title = "Song",
            artist = "Artist",
            album = "Album",
            path = audio.absolutePath,
            contentUriString = "",
            duration = 10_000L,
        )
        val resolver = LocalLyricsResolver(
            context = mockk<Context>(relaxed = true),
            musicDao = mockk<MusicDao>(relaxed = true),
        )

        assertThat(resolver.findLocalRaw(song, listOf(LyricsSourceId.LOCAL_LRC)))
            .isEqualTo("[00:01.00]hello")
        // The empty fixture has no embedded tag, so that source must stay empty.
        assertThat(resolver.findLocalRaw(song, listOf(LyricsSourceId.EMBEDDED))).isNull()

        directory.deleteRecursively()
    }

    @Test
    fun findLocalRaw_respectsTheEnabledLocalOrder() = runTest {
        val directory = Files.createTempDirectory("local-lyrics-order").toFile()
        val audio = File(directory, "Song.mp3").apply { writeBytes(ByteArray(0)) }
        File(directory, "Song.lrc").writeText("[00:01.00]local")
        val song = Song.emptySong().copy(
            id = "1",
            title = "Song",
            artist = "Artist",
            album = "Album",
            path = audio.absolutePath,
            contentUriString = "",
            duration = 10_000L,
        )
        val resolver = LocalLyricsResolver(
            context = mockk<Context>(relaxed = true),
            musicDao = mockk<MusicDao>(relaxed = true),
        )

        assertThat(resolver.findLocalRaw(song, listOf(LyricsSourceId.EMBEDDED)))
            .isNull()
        assertThat(resolver.findLocalRaw(song, listOf(LyricsSourceId.LOCAL_LRC)))
            .isEqualTo("[00:01.00]local")

        directory.deleteRecursively()
    }
}
