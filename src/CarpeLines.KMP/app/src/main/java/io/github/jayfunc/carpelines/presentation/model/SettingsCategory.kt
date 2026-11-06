package io.github.jayfunc.carpelines.presentation.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.jayfunc.carpelines.R

enum class SettingsCategory(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector? = null,
    val iconRes: Int? = null
) {
    APPEARANCE(
        id = "appearance",
        titleRes = R.string.settings_category_appearance_title,
        subtitleRes = R.string.settings_category_appearance_subtitle,
        icon = Icons.Rounded.Palette
    ),
    /**
     * Search-only grouping for the lyrics screen. It has no page of its own: the main Settings
     * list links to [Screen.LyricsSettings] directly, so these rows only label search results.
     */
    LYRICS(
        id = "lyrics",
        titleRes = R.string.setcat_lyrics_title,
        subtitleRes = R.string.setcat_lyrics_subtitle,
        icon = Icons.Rounded.Lyrics
    ),
    /**
     * The device library that feeds the local lyrics sources. CarpeLines does not play audio, but
     * tag and .lrc lyrics can only be found for tracks the library has actually scanned.
     */
    LOCAL_MUSIC(
        id = "local_music",
        titleRes = R.string.settings_category_local_music_title,
        subtitleRes = R.string.settings_category_local_music_subtitle,
        icon = Icons.Rounded.LibraryMusic
    ),
    BACKUP_RESTORE(
        id = "backup_restore",
        titleRes = R.string.settings_category_backup_title,
        subtitleRes = R.string.settings_category_backup_subtitle,
        iconRes = R.drawable.rounded_upload_file_24
    ),
    ABOUT(
        id = "about",
        titleRes = R.string.settings_category_about_title,
        subtitleRes = R.string.settings_category_about_subtitle,
        icon = Icons.Rounded.Info
    );

    companion object {
        fun fromId(id: String): SettingsCategory? = entries.find { it.id == id }
    }
}
