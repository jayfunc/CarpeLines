package io.github.jayfunc.carpelines.presentation.screens

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.presentation.model.SettingsCategory
import io.github.jayfunc.carpelines.presentation.navigation.Screen
import io.github.jayfunc.carpelines.ui.theme.M3EShape

/** A row of the main Settings list. */
internal sealed interface MainSettingsEntry {
    val route: String

    /** A row drawn from the category's own title, subtitle and icon. */
    data class Category(val category: SettingsCategory) : MainSettingsEntry {
        override val route: String
            get() = when (category) {
                SettingsCategory.ABOUT -> Screen.About.route
                else -> Screen.SettingsCategory.createRoute(category.id)
            }
    }

    /** A row for a settings screen that is not a [SettingsCategory]. */
    enum class Destination(
        @StringRes val titleRes: Int,
        @StringRes val subtitleRes: Int,
        val icon: ImageVector,
        override val route: String
    ) : MainSettingsEntry {
        LYRICS(
            titleRes = R.string.setcat_lyrics_title,
            subtitleRes = R.string.setcat_lyrics_subtitle,
            icon = Icons.Rounded.Lyrics,
            route = Screen.LyricsSettings.createRoute()
        ),
        UPDATES(
            titleRes = R.string.updates_title,
            subtitleRes = R.string.updates_settings_subtitle,
            icon = Icons.Rounded.SystemUpdate,
            route = Screen.Updates.route
        )
    }
}

/**
 * Every row of the main Settings list, in display order. The gaps between rows are derived from
 * this list, so adding a row here cannot leave the list spacing out of step.
 */
internal val mainSettingsEntries: List<MainSettingsEntry> = buildList {
    add(MainSettingsEntry.Category(SettingsCategory.APPEARANCE))
    add(MainSettingsEntry.Destination.LYRICS)
    add(MainSettingsEntry.Category(SettingsCategory.ONLINE_SERVICES))
    add(MainSettingsEntry.Category(SettingsCategory.LOCAL_MUSIC))
    add(MainSettingsEntry.Category(SettingsCategory.BACKUP_RESTORE))
    add(MainSettingsEntry.Destination.UPDATES)
    add(MainSettingsEntry.Category(SettingsCategory.ABOUT))
}

/** Shape of row [index] in a group of [count] rows: only the group's outer corners are fully rounded. */
internal fun settingsGroupItemShape(index: Int, count: Int): Shape =
    M3EShape.grouped(index = index, count = count, radius = SettingsGroupCornerRadius)

private val SettingsGroupCornerRadius = 24.dp
