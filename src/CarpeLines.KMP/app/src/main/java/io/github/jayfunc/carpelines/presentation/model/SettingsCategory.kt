package io.github.jayfunc.carpelines.presentation.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.FilterAlt
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
     * Where lyrics come from: provider credentials and the source order. It has its own page
     * ([Screen.LyricsManagement]) rather than a section at the bottom of [LYRICS], so the
     * long-press sheet can send a wrong lyric straight here.
     */
    LYRICS_MANAGEMENT(
        id = "lyrics_management",
        titleRes = R.string.setcat_lyrics_management,
        subtitleRes = R.string.settings_category_lyrics_management_subtitle,
        icon = Icons.AutoMirrored.Rounded.ManageSearch
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
    /**
     * Which picture represents the playing track and in what order the app looks for it: the
     * session's own artwork, the matching library cover, or a Deezer album cover.
     */
    COVER_SOURCES(
        id = "cover_sources",
        titleRes = R.string.settings_category_cover_sources_title,
        subtitleRes = R.string.settings_category_cover_sources_subtitle,
        icon = Icons.Outlined.Image
    ),
    /**
     * Which players may publish a media session. Ignored players are not followed at all, so their
     * tracks never reach the lyrics screen or the listening statistics.
     */
    PLAYER_FILTER(
        id = "player_filter",
        titleRes = R.string.settings_category_player_filter_title,
        subtitleRes = R.string.settings_category_player_filter_subtitle,
        icon = Icons.Rounded.FilterAlt
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
