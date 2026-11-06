package io.github.jayfunc.carpelines.data.backup.model

import androidx.annotation.StringRes
import io.github.jayfunc.carpelines.R

enum class BackupSection(
    val key: String,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val iconRes: Int,
    val sinceVersion: Int = 1
) {
    PLAYLISTS(
        key = "playlists",
        labelRes = R.string.backup_module_playlists_label,
        descriptionRes = R.string.backup_module_playlists_description,
        iconRes = R.drawable.rounded_playlist_play_24
    ),
    GLOBAL_SETTINGS(
        key = "global_settings",
        labelRes = R.string.backup_module_global_settings_label,
        descriptionRes = R.string.backup_module_global_settings_description,
        iconRes = R.drawable.rounded_settings_24
    ),
    FAVORITES(
        key = "favorites",
        labelRes = R.string.backup_module_favorites_label,
        descriptionRes = R.string.backup_module_favorites_description,
        iconRes = R.drawable.rounded_favorite_24
    ),
    LYRICS(
        key = "lyrics",
        labelRes = R.string.backup_module_lyrics_label,
        descriptionRes = R.string.backup_module_lyrics_description,
        iconRes = R.drawable.rounded_lyrics_24
    ),
    SEARCH_HISTORY(
        key = "search_history",
        labelRes = R.string.backup_module_search_history_label,
        descriptionRes = R.string.backup_module_search_history_description,
        iconRes = R.drawable.rounded_search_24
    ),
    TRANSITIONS(
        key = "transitions",
        labelRes = R.string.backup_module_transitions_label,
        descriptionRes = R.string.backup_module_transitions_description,
        iconRes = R.drawable.rounded_align_justify_space_even_24
    ),
    ENGAGEMENT_STATS(
        key = "engagement_stats",
        labelRes = R.string.backup_module_engagement_stats_label,
        descriptionRes = R.string.backup_module_engagement_stats_description,
        iconRes = R.drawable.rounded_monitoring_24
    ),
    PLAYBACK_HISTORY(
        key = "playback_history",
        labelRes = R.string.backup_module_playback_history_label,
        descriptionRes = R.string.backup_module_playback_history_description,
        iconRes = R.drawable.rounded_schedule_24
    ),
    QUICK_FILL(
        key = "quick_fill",
        labelRes = R.string.backup_module_quick_fill_label,
        descriptionRes = R.string.backup_module_quick_fill_description,
        iconRes = R.drawable.rounded_instant_mix_24,
        sinceVersion = 3
    ),
    ARTIST_IMAGES(
        key = "artist_images",
        labelRes = R.string.backup_module_artist_images_label,
        descriptionRes = R.string.backup_module_artist_images_description,
        iconRes = R.drawable.rounded_person_24,
        sinceVersion = 3
    ),
    EQUALIZER(
        key = "equalizer",
        labelRes = R.string.backup_module_equalizer_label,
        descriptionRes = R.string.backup_module_equalizer_description,
        iconRes = R.drawable.rounded_surround_sound_24,
        sinceVersion = 3
    ),
    AUDIO_BOOKMARKS(
        key = "audio_bookmarks",
        labelRes = R.string.backup_module_audio_bookmarks_label,
        descriptionRes = R.string.backup_module_audio_bookmarks_description,
        iconRes = R.drawable.round_bookmark_24,
        sinceVersion = 4
    );

    companion object {
        val defaultSelection: Set<BackupSection> = entries.toSet()

        fun fromKey(key: String): BackupSection? = entries.find { it.key == key }
    }
}
