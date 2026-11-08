package io.github.jayfunc.carpelines.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoverArtSourceTest {

    @Test
    fun `default order keeps the session's cover first`() {
        assertEquals(
            listOf(CoverArtSource.MEDIA_SESSION, CoverArtSource.LOCAL_EMBEDDED, CoverArtSource.DEEZER),
            CoverArtSource.DefaultOrder
        )
    }

    @Test
    fun `decode preserves the enabled set and order`() {
        assertEquals(
            listOf(CoverArtSource.DEEZER, CoverArtSource.MEDIA_SESSION),
            CoverArtSource.decode("DEEZER,MEDIA_SESSION")
        )
    }

    @Test
    fun `decode drops unknown names and duplicates`() {
        assertEquals(
            listOf(CoverArtSource.LOCAL_EMBEDDED, CoverArtSource.DEEZER),
            CoverArtSource.decode("LOCAL_EMBEDDED,UNKNOWN,DEEZER,LOCAL_EMBEDDED")
        )
    }

    @Test
    fun `empty or unreadable values fall back to the default order`() {
        assertEquals(CoverArtSource.DefaultOrder, CoverArtSource.decode(null))
        assertEquals(CoverArtSource.DefaultOrder, CoverArtSource.decode(""))
        assertEquals(CoverArtSource.DefaultOrder, CoverArtSource.decode("UNKNOWN"))
    }

    @Test
    fun `encode round-trips through decode`() {
        val order = listOf(CoverArtSource.DEEZER, CoverArtSource.LOCAL_EMBEDDED)
        assertEquals(order, CoverArtSource.decode(CoverArtSource.encode(order)))
    }
}
