package io.github.jayfunc.carpelines

import io.github.jayfunc.carpelines.LocaleStringResources.assertTranslatedEverywhere
import io.github.jayfunc.carpelines.LocaleStringResources.nameOf
import io.github.jayfunc.carpelines.LocaleStringResources.stringsIn
import io.github.jayfunc.carpelines.presentation.navigation.Screen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MainNavItemsTest {

    @Test
    fun `navigation shows home lyrics stats and settings in order`() {
        assertEquals(
            listOf(Screen.Home, Screen.NowPlayingLyrics, Screen.Stats, Screen.Settings),
            MainNavItems.map { it.screen },
        )
        assertEquals(
            listOf(R.string.tab_home, R.string.tab_lyrics, R.string.tab_stats, R.string.tab_settings),
            MainNavItems.map { it.labelResId },
        )
    }

    @Test
    fun `every navigation label is translated in every locale`() {
        assertTranslatedEverywhere(MainNavItems.map { nameOf(it.labelResId) })
    }

    @Test
    fun `simplified chinese navigation labels are not left in english`() {
        val english = stringsIn("values")
        val chinese = stringsIn("values-zh-rCN")

        MainNavItems.forEach { item ->
            val name = nameOf(item.labelResId)
            val label = chinese[name]
            assertTrue(!label.isNullOrEmpty(), "values-zh-rCN has no text for $name")
            assertNotEquals(english[name], label, "$name is still English in values-zh-rCN")
            assertTrue(
                label!!.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN },
                "$name has no Chinese text in values-zh-rCN: $label",
            )
        }
    }

    @Test
    fun `strings shown around the navigation are translated in every locale`() {
        assertTranslatedEverywhere(
            listOf(
                R.string.setup_gate_preparing,
                R.string.home_your_mix_subtitle,
                R.string.home_your_mix_queue_name,
                R.string.song_info_label_provider,
                R.string.song_info_label_path,
                R.string.cd_playlist_artwork,
            ).map(LocaleStringResources::nameOf)
        )
    }

    @Test
    fun `queue names set from the library tab are translated in every locale`() {
        assertTranslatedEverywhere(
            listOf(
                R.string.tab_library,
                R.string.queue_name_liked_songs,
                R.string.queue_name_all_songs_shuffled,
                R.string.queue_name_liked_songs_shuffled,
                R.string.queue_name_selected_songs,
            ).map(LocaleStringResources::nameOf)
        )
    }
}
