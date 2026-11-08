package io.github.jayfunc.carpelines.presentation.screens

import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.presentation.model.SettingsCategory
import io.github.jayfunc.carpelines.presentation.navigation.Screen
import io.github.jayfunc.carpelines.ui.theme.ShapeCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class SettingsMainListTest {

    // The main list uses M3 Expressive smooth corners, so the expectations come from the same
    // cache the screen draws with instead of a plain RoundedCornerShape.
    private val firstShape = ShapeCache.smoothTop(24.dp)
    private val middleShape = ShapeCache.smooth4
    private val lastShape = ShapeCache.smoothBottom(24.dp)

    @Test
    fun `main list ends with updates followed by about`() {
        assertEquals(MainSettingsEntry.Category(SettingsCategory.ABOUT), mainSettingsEntries.last())
        assertEquals(MainSettingsEntry.Destination.UPDATES, mainSettingsEntries[mainSettingsEntries.lastIndex - 1])
    }

    @Test
    fun `main list shows every listed category and destination exactly once`() {
        // LYRICS is a search-only grouping with no page of its own, so the main list links to the
        // lyrics screen directly instead of adding a category row for it. LYRICS_MANAGEMENT is a
        // real page, listed right after the appearance row it complements.
        val expected = listOf(
            MainSettingsEntry.Category(SettingsCategory.APPEARANCE),
            MainSettingsEntry.Destination.LYRICS,
            MainSettingsEntry.Category(SettingsCategory.LYRICS_MANAGEMENT),
            MainSettingsEntry.Category(SettingsCategory.COVER_SOURCES),
            MainSettingsEntry.Category(SettingsCategory.LOCAL_MUSIC),
            MainSettingsEntry.Category(SettingsCategory.BACKUP_RESTORE),
            MainSettingsEntry.Destination.UPDATES,
            MainSettingsEntry.Category(SettingsCategory.ABOUT)
        )

        assertEquals(expected, mainSettingsEntries)
    }

    @Test
    fun `lyrics row sits directly below appearance`() {
        val appearanceIndex = mainSettingsEntries.indexOf(MainSettingsEntry.Category(SettingsCategory.APPEARANCE))

        assertEquals(MainSettingsEntry.Destination.LYRICS, mainSettingsEntries[appearanceIndex + 1])
    }

    @Test
    fun `only the last row of the main list gets rounded bottom corners`() {
        val count = mainSettingsEntries.size

        assertEquals(firstShape, settingsGroupItemShape(0, count))
        for (index in 1 until count - 1) {
            assertEquals(middleShape, settingsGroupItemShape(index, count), "row $index: ${mainSettingsEntries[index]}")
        }
        assertEquals(lastShape, settingsGroupItemShape(count - 1, count))
    }

    @Test
    fun `group shape rounds only the outer corners of the group`() {
        assertEquals(ShapeCache.smooth(24.dp), settingsGroupItemShape(0, 1))
        assertEquals(firstShape, settingsGroupItemShape(0, 2))
        assertEquals(lastShape, settingsGroupItemShape(1, 2))
        assertEquals(firstShape, settingsGroupItemShape(0, 3))
        assertEquals(middleShape, settingsGroupItemShape(1, 3))
        assertEquals(lastShape, settingsGroupItemShape(2, 3))
    }

    @Test
    fun `group shapes are shared instances`() {
        assertSame(settingsGroupItemShape(1, 5), settingsGroupItemShape(3, 5))
    }

    @Test
    fun `rows open their own screens`() {
        assertEquals(
            Screen.SettingsCategory.createRoute("appearance"),
            MainSettingsEntry.Category(SettingsCategory.APPEARANCE).route
        )
        assertEquals(
            Screen.SettingsCategory.createRoute("backup_restore"),
            MainSettingsEntry.Category(SettingsCategory.BACKUP_RESTORE).route
        )
        assertEquals(
            Screen.CoverSources.route,
            MainSettingsEntry.Category(SettingsCategory.COVER_SOURCES).route
        )
        assertEquals(Screen.About.route, MainSettingsEntry.Category(SettingsCategory.ABOUT).route)
        assertEquals(Screen.LyricsSettings.createRoute(), MainSettingsEntry.Destination.LYRICS.route)
        assertEquals(Screen.Updates.route, MainSettingsEntry.Destination.UPDATES.route)
    }
}
