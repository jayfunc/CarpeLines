package io.github.jayfunc.carpelines.data.backup.model

import androidx.annotation.StringRes
import io.github.jayfunc.carpelines.R

enum class BackupSection(
    val key: String,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val iconRes: Int
) {
    GLOBAL_SETTINGS(
        key = "global_settings",
        labelRes = R.string.backup_module_global_settings_label,
        descriptionRes = R.string.backup_module_global_settings_description,
        iconRes = R.drawable.rounded_settings_24
    ),
    LOCAL_MUSIC_FOLDERS(
        key = "local_music_folders",
        labelRes = R.string.backup_module_local_music_folders_label,
        descriptionRes = R.string.backup_module_local_music_folders_description,
        iconRes = R.drawable.rounded_folder_24
    ),
    LYRICS(
        key = "lyrics",
        labelRes = R.string.backup_module_lyrics_label,
        descriptionRes = R.string.backup_module_lyrics_description,
        iconRes = R.drawable.rounded_lyrics_24
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
    );

    companion object {
        val defaultSelection: Set<BackupSection> = entries.toSet()

        fun fromKey(key: String): BackupSection? = entries.find { it.key == key }
    }
}
