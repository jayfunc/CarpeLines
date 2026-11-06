package io.github.jayfunc.carpelines.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class LyricsDisplayPreferencesTest {

    @Test
    fun `lyrics display settings default on and persist`() = runTest {
        val tempDir = Files.createTempDirectory("lyrics-display-preferences-test")
        try {
            val dataStore = PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { tempDir.resolve("settings.preferences_pb").toFile() }
            )
            val repository = UserPreferencesRepository(dataStore = dataStore, json = Json)

            assertTrue(repository.lyricsDuetLayoutEnabledFlow.first())
            assertTrue(repository.showLyricsBackgroundVocalsFlow.first())

            repository.setLyricsDuetLayoutEnabled(false)
            repository.setShowLyricsBackgroundVocals(false)

            assertFalse(repository.lyricsDuetLayoutEnabledFlow.first())
            assertFalse(repository.showLyricsBackgroundVocalsFlow.first())
            // The lyrics sheet reads the same keys straight from the DataStore.
            val stored = dataStore.data.first()
            assertEquals(false, stored[LYRICS_DUET_LAYOUT_KEY])
            assertEquals(false, stored[SHOW_LYRICS_BACKGROUND_VOCALS_KEY])

            dataStore.edit { it[SHOW_LYRICS_BACKGROUND_VOCALS_KEY] = true }
            assertTrue(repository.showLyricsBackgroundVocalsFlow.first())
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
