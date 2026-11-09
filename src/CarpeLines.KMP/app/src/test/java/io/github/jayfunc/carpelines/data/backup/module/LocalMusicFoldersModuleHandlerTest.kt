package io.github.jayfunc.carpelines.data.backup.module

import com.google.gson.GsonBuilder
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalMusicFoldersModuleHandlerTest {

    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val handler = LocalMusicFoldersModuleHandler(
        userPreferencesRepository = userPreferencesRepository,
        gson = GsonBuilder().serializeNulls().create()
    )

    @Test
    fun `export writes folder paths in stable order`() = runTest {
        every { userPreferencesRepository.localLibraryFoldersFlow } returns
            flowOf(setOf("/storage/emulated/0/Zeta", "/storage/emulated/0/Music"))

        assertEquals(
            """["/storage/emulated/0/Music","/storage/emulated/0/Zeta"]""",
            handler.export()
        )
    }

    @Test
    fun `restore normalizes and deduplicates folder paths`() = runTest {
        coEvery { userPreferencesRepository.setLocalLibraryFolders(any()) } returns Unit

        handler.restore(
            """[" /storage/emulated/0/Music/ ", "/storage/emulated/0/Music", "   "]"""
        )

        coVerify {
            userPreferencesRepository.setLocalLibraryFolders(
                setOf("/storage/emulated/0/Music")
            )
        }
    }

    @Test
    fun `restore skips a payload whose entries are all unusable`() = runTest {
        assertThrows(ModuleRestoreSkippedException::class.java) {
            runBlocking { handler.restore("""[42, null, "  "]""") }
        }

        coVerify(exactly = 0) { userPreferencesRepository.setLocalLibraryFolders(any()) }
    }
}
