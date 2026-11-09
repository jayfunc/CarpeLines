package io.github.jayfunc.carpelines.presentation.viewmodel

import io.github.jayfunc.carpelines.data.model.Song
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QueueStateHolderTest {

    private fun song(id: String) = Song(
        id = id,
        title = id,
        artist = "Artist",
        artistId = 1L,
        album = "Album",
        albumId = 1L,
        path = "/music/$id.mp3",
        contentUriString = "content://media/$id",
        albumArtUriString = null,
        duration = 180_000L,
        mimeType = "audio/mpeg",
        bitrate = null,
        sampleRate = null
    )

    @Test
    fun `original order keeps the songs that were queued`() {
        val holder = QueueStateHolder()

        holder.setOriginalQueueOrder(listOf(song("x"), song("a")))

        assertEquals(listOf("x", "a"), holder.originalQueueOrder.map { it.id })
    }

    @Test
    fun `shuffled playback keeps the original order`() = runTest {
        val holder = QueueStateHolder()
        val songs = listOf(song("a"), song("b"), song("c"))

        holder.prepareShuffledQueueSuspending(songs)

        assertEquals(songs, holder.originalQueueOrder)
    }
}
