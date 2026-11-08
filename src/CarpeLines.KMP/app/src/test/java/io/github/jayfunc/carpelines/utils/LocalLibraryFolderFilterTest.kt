package io.github.jayfunc.carpelines.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalLibraryFolderFilterTest {

    private val music = "/storage/emulated/0/Music"

    @Test
    fun `a file inside the imported folder matches`() {
        assertTrue(LocalLibraryFolderFilter.isUnder("$music/Album/track.mp3", music))
        assertTrue(LocalLibraryFolderFilter.isUnder(music, music))
    }

    @Test
    fun `a sibling with a shared prefix does not match`() {
        assertFalse(LocalLibraryFolderFilter.isUnder("/storage/emulated/0/MusicBackup/track.mp3", music))
    }

    @Test
    fun `trailing separators and case do not change the match`() {
        assertTrue(LocalLibraryFolderFilter.isUnder("/storage/emulated/0/Music/track.mp3", "$music/"))
        assertTrue(LocalLibraryFolderFilter.isUnder("/storage/emulated/0/music/track.mp3", music))
    }

    @Test
    fun `no imported folders means no deep-search match`() {
        assertFalse(LocalLibraryFolderFilter.isInside("$music/track.mp3", emptySet()))
    }

    @Test
    fun `any imported folder can match`() {
        assertTrue(
            LocalLibraryFolderFilter.isInside(
                "/storage/emulated/0/Download/track.mp3",
                setOf(music, "/storage/emulated/0/Download"),
            )
        )
    }
}
