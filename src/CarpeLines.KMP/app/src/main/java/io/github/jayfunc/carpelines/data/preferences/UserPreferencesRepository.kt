package io.github.jayfunc.carpelines.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import io.github.jayfunc.carpelines.data.diagnostics.AdvancedPerformanceDiagnostics
import io.github.jayfunc.carpelines.data.model.PlaybackQueueSnapshot
import io.github.jayfunc.carpelines.data.model.SortOption
import io.github.jayfunc.carpelines.data.model.FolderSource
import io.github.jayfunc.carpelines.data.model.LyricsSourcePreference
import io.github.jayfunc.carpelines.data.model.CoverArtSource
import io.github.jayfunc.carpelines.data.model.AudioOutputMode
import io.github.jayfunc.carpelines.data.equalizer.EqualizerPreset
import io.github.jayfunc.carpelines.data.model.StorageFilter
import io.github.jayfunc.carpelines.data.update.UpdateChannel
import io.github.jayfunc.carpelines.utils.PlaybackSpeedUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.get
import kotlin.text.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object ThemePreference {
    const val DEFAULT = "default"
    const val DYNAMIC = "dynamic"
    const val ALBUM_ART = "album_art"
    const val GLOBAL = "global"
}

object AppThemeMode {
    const val FOLLOW_SYSTEM = "follow_system"
    const val LIGHT = "light"
    const val DARK = "dark"
}

const val MIN_NAV_BAR_CORNER_RADIUS = 0
const val MAX_NAV_BAR_CORNER_RADIUS = 60

internal fun sanitizeNavBarCornerRadius(radius: Int): Int =
        radius.coerceIn(MIN_NAV_BAR_CORNER_RADIUS, MAX_NAV_BAR_CORNER_RADIUS)

/**
 * Album art quality settings for developer options.
 * Controls maximum resolution for album artwork in player view.
 * Thumbnails in lists always use low resolution for performance.
 *
 * @property maxSize Maximum size in pixels (0 = original size)
 * @property label Human-readable label for UI
 */
enum class AlbumArtQuality(val maxSize: Int, val label: String) {
    LOW(256, "Low (256px) - Better performance"),
    MEDIUM(512, "Medium (512px) - Balanced"),
    HIGH(800, "High (800px) - Best quality"),
    ORIGINAL(0, "Original - Maximum quality")
}

data class AdvancedPerformanceDiagnosticsSettings(
    val enabled: Boolean,
    val sessionStartedEpochMs: Long?,
    val expiresAtEpochMs: Long?
) {
    fun isActive(nowEpochMs: Long = System.currentTimeMillis()): Boolean =
        enabled && expiresAtEpochMs?.let { nowEpochMs < it } == true
}

@Singleton
class UserPreferencesRepository
@Inject
constructor(
        private val dataStore: DataStore<Preferences>,
        private val json: Json
) {

    private val backupExcludedKeyNames = setOf(
        PreferencesKeys.INITIAL_SETUP_DONE.name,
        // Describes this device's artwork cache, not a user choice — restoring another
        // device's value would misreport what the local cache was built from.
        PreferencesKeys.FOLDER_ALBUM_ART_CACHE_STATE.name,
        // Describes which text repair this install's library database went through, not a user
        // choice — restoring another device's value could skip or repeat the repair here.
        PreferencesKeys.METADATA_CHARSET_REPAIR_VERSION.name,
        // Update settings describe this install (alpha/beta/stable), not user taste;
        // restoring them onto another channel's install would offer a cross-signed update.
        PreferencesKeys.UPDATE_CHANNEL.name,
        PreferencesKeys.UPDATE_AUTO_CHECK.name,
        PreferencesKeys.UPDATE_LAST_NOTIFIED_VERSION.name,
        // Absolute storage paths of *this* device. Restoring another device's values would
        // either hide the local library behind its block rules or leave the imported folders
        // pointing at directories that do not exist here. The imported folders have their own
        // backup module, so they stay out of the global settings payload.
        PreferencesKeys.ALLOWED_DIRECTORIES.name,
        PreferencesKeys.BLOCKED_DIRECTORIES.name,
        PreferencesKeys.LOCAL_LIBRARY_FOLDERS.name
    )

    private object PreferencesKeys {
        val APP_REBRAND_DIALOG_SHOWN = booleanPreferencesKey("app_rebrand_dialog_shown")
        val RECAP_HOME_CARD_DISMISSED_YEAR = intPreferencesKey("recap_home_card_dismissed_year")
        val ALLOWED_DIRECTORIES = stringSetPreferencesKey("allowed_directories")
        val BLOCKED_DIRECTORIES = stringSetPreferencesKey("blocked_directories")
        /** User-imported folders for the deep lyric/artwork/.lrc search. */
        val LOCAL_LIBRARY_FOLDERS = stringSetPreferencesKey("local_library_folders")
        val INITIAL_SETUP_DONE = booleanPreferencesKey("initial_setup_done")
        val PLAYER_THEME_PREFERENCE = stringPreferencesKey("player_theme_preference_v2")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")

        val SONGS_SORT_OPTION = stringPreferencesKey("songs_sort_option")
        val SONGS_SORT_OPTION_MIGRATED = booleanPreferencesKey("songs_sort_option_migrated_v2")
        val ALBUMS_SORT_OPTION = stringPreferencesKey("albums_sort_option")
        val ARTISTS_SORT_OPTION = stringPreferencesKey("artists_sort_option")
        val FOLDERS_SORT_OPTION = stringPreferencesKey("folders_sort_option")
        val FOLDER_SONGS_SORT_OPTION = stringPreferencesKey("folder_songs_sort_option")
        val GENRES_SORT_OPTION = stringPreferencesKey("genres_sort_option")

        val LAST_LIBRARY_TAB_INDEX =
                intPreferencesKey("last_library_tab_index")
        val LAST_STORAGE_FILTER = stringPreferencesKey("last_storage_filter")
        val MOCK_GENRES_ENABLED = booleanPreferencesKey("mock_genres_enabled")
        val LAST_DAILY_MIX_UPDATE = longPreferencesKey("last_daily_mix_update")
        val DAILY_MIX_SONG_IDS = stringPreferencesKey("daily_mix_song_ids")
        val YOUR_MIX_SONG_IDS = stringPreferencesKey("your_mix_song_ids")
        val NAV_BAR_CORNER_RADIUS = intPreferencesKey("nav_bar_corner_radius")
        val NAV_BAR_STYLE = stringPreferencesKey("nav_bar_style")
        val NAV_BAR_COMPACT_MODE = booleanPreferencesKey("nav_bar_compact_mode")
        val CAROUSEL_STYLE = stringPreferencesKey("carousel_style")
        val LIBRARY_NAVIGATION_MODE = stringPreferencesKey("library_navigation_mode")
        val LAUNCH_TAB = stringPreferencesKey("launch_tab")

        val LIBRARY_TABS_ORDER = stringPreferencesKey("library_tabs_order")
        val LIBRARY_TABS_FOLDERS_MIGRATED = booleanPreferencesKey("library_tabs_folders_migrated")
        val LIBRARY_TABS_GENRES_MIGRATED = booleanPreferencesKey("library_tabs_genres_migrated")
        val IS_FOLDER_FILTER_ACTIVE = booleanPreferencesKey("is_folder_filter_active")
        val IS_FOLDERS_PLAYLIST_VIEW = booleanPreferencesKey("is_folders_playlist_view")
        val HIDE_LOCAL_MEDIA = booleanPreferencesKey("hide_local_media")
        val FOLDERS_SOURCE = stringPreferencesKey("folders_source")
        val FOLDER_BACK_GESTURE_NAVIGATION = booleanPreferencesKey("folder_back_gesture_navigation")
        val USE_SMOOTH_CORNERS = booleanPreferencesKey("use_smooth_corners")
        val KEEP_PLAYING_IN_BACKGROUND = booleanPreferencesKey("keep_playing_in_background")
        val KEEP_SCREEN_AWAKE_WHILE_PLAYING =
                booleanPreferencesKey("keep_screen_awake_while_playing")
        val IS_CROSSFADE_ENABLED = booleanPreferencesKey("is_crossfade_enabled")
        val AUDIO_OUTPUT_MODE = stringPreferencesKey("audio_output_mode_v1")
        val SMART_CROSSFADE_ENABLED = booleanPreferencesKey("smart_crossfade_enabled")
        val HI_FI_MODE_ENABLED = booleanPreferencesKey("hi_fi_mode_enabled")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        val PLAYBACK_SPEED = androidx.datastore.preferences.core.floatPreferencesKey("playback_speed")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val IS_SHUFFLE_ON = booleanPreferencesKey("is_shuffle_on")
        val PERSISTENT_SHUFFLE_ENABLED = booleanPreferencesKey("persistent_shuffle_enabled")
        val RESUME_ON_HEADSET_RECONNECT = booleanPreferencesKey("resume_on_headset_reconnect")
        val SHOW_QUEUE_HISTORY = booleanPreferencesKey("show_queue_history")
        val PLAYBACK_QUEUE_SNAPSHOT = stringPreferencesKey("playback_queue_snapshot_v1")
        val FULL_PLAYER_SHOW_FILE_INFO = booleanPreferencesKey("full_player_show_file_info")
        val FULL_PLAYER_DELAY_ALL = booleanPreferencesKey("full_player_delay_all")
        val FULL_PLAYER_DELAY_ALBUM = booleanPreferencesKey("full_player_delay_album")
        val FULL_PLAYER_DELAY_METADATA = booleanPreferencesKey("full_player_delay_metadata")
        val FULL_PLAYER_DELAY_PROGRESS = booleanPreferencesKey("full_player_delay_progress")
        val FULL_PLAYER_DELAY_CONTROLS = booleanPreferencesKey("full_player_delay_controls")
        val FULL_PLAYER_PLACEHOLDERS = booleanPreferencesKey("full_player_placeholders")
        val FULL_PLAYER_PLACEHOLDER_TRANSPARENT = booleanPreferencesKey("full_player_placeholder_transparent")
        val FULL_PLAYER_PLACEHOLDERS_ON_CLOSE = booleanPreferencesKey("full_player_placeholders_on_close")
        val FULL_PLAYER_SWITCH_ON_DRAG_RELEASE = booleanPreferencesKey("full_player_switch_on_drag_release")
        val FULL_PLAYER_DELAY_THRESHOLD = intPreferencesKey("full_player_delay_threshold_percent")
        val FULL_PLAYER_CLOSE_THRESHOLD = intPreferencesKey("full_player_close_threshold_percent")
        val USE_PLAYER_SHEET_V2 = booleanPreferencesKey("use_player_sheet_v2")

        val ARTIST_DELIMITERS = stringPreferencesKey("artist_delimiters")
        val ARTIST_WORD_DELIMITERS = stringPreferencesKey("artist_word_delimiters")
        val EXTRACT_ARTISTS_FROM_TITLE = booleanPreferencesKey("extract_artists_from_title")
        val GROUP_BY_ALBUM_ARTIST = booleanPreferencesKey("group_by_album_artist")
        val ARTIST_ALBUMS_COLLAPSED_BY_DEFAULT =
                booleanPreferencesKey("artist_albums_collapsed_by_default")
        val ARTIST_SETTINGS_RESCAN_REQUIRED =
                booleanPreferencesKey("artist_settings_rescan_required")

        val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val EQUALIZER_PRESET = stringPreferencesKey("equalizer_preset")
        val EQUALIZER_CUSTOM_BANDS = stringPreferencesKey("equalizer_custom_bands")
        val BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")
        val VIRTUALIZER_STRENGTH = intPreferencesKey("virtualizer_strength")
        val BASS_BOOST_ENABLED = booleanPreferencesKey("bass_boost_enabled")
        val VIRTUALIZER_ENABLED = booleanPreferencesKey("virtualizer_enabled")
        val LOUDNESS_ENHANCER_ENABLED = booleanPreferencesKey("loudness_enhancer_enabled")
        val LOUDNESS_ENHANCER_STRENGTH = intPreferencesKey("loudness_enhancer_strength")

        val BASS_BOOST_DISMISSED = booleanPreferencesKey("bass_boost_dismissed")
        val VIRTUALIZER_DISMISSED = booleanPreferencesKey("virtualizer_dismissed")
        val LOUDNESS_DISMISSED = booleanPreferencesKey("loudness_dismissed")
        val BACKUP_INFO_DISMISSED = booleanPreferencesKey("backup_info_dismissed")

        val VIEW_MODE = stringPreferencesKey("equalizer_view_mode")

        val CUSTOM_PRESETS = stringPreferencesKey("custom_presets_json")
        val PINNED_PRESETS = stringPreferencesKey("pinned_presets_json")

        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val DIRECTORY_RULES_VERSION = intPreferencesKey("directory_rules_version")
        val LAST_APPLIED_DIRECTORY_RULES_VERSION =
            intPreferencesKey("last_applied_directory_rules_version")
        val METADATA_CHARSET_REPAIR_VERSION = intPreferencesKey("metadata_charset_repair_version")

        val LYRICS_SYNC_OFFSETS = stringPreferencesKey("lyrics_sync_offsets_json")

        val LYRICS_SOURCE_PREFERENCE = stringPreferencesKey("lyrics_source_preference")
        val EXTERNAL_LYRICS_ENABLED = booleanPreferencesKey("external_lyrics_enabled")
        val COVER_ART_SOURCE_ORDER = stringPreferencesKey("cover_art_source_order")
        val EXTERNAL_PLAYER_FILTER_ENABLED = booleanPreferencesKey("external_player_filter_enabled")
        val EXTERNAL_PLAYER_ALLOWED_PACKAGES =
            stringSetPreferencesKey("external_player_allowed_packages")

        val ALBUM_ART_QUALITY = stringPreferencesKey("album_art_quality")
        val ALBUM_ART_CACHE_LIMIT_MB = intPreferencesKey("album_art_cache_limit_mb")
        val USE_FOLDER_ALBUM_ART = booleanPreferencesKey("use_folder_album_art")
        val FOLDER_ALBUM_ART_CACHE_STATE = booleanPreferencesKey("folder_album_art_cache_state")
        val TAP_BACKGROUND_CLOSES_PLAYER = booleanPreferencesKey("tap_background_closes_player")
        val MINI_PLAYER_SWIPE_TO_SKIP = booleanPreferencesKey("mini_player_swipe_to_skip")
        val AUTOPLAY_ON_SKIP = booleanPreferencesKey("autoplay_on_skip")
        val PREVIOUS_BUTTON_BEHAVIOR = stringPreferencesKey("previous_button_behavior")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val ADVANCED_PERFORMANCE_DIAGNOSTICS_ENABLED =
            booleanPreferencesKey("advanced_performance_diagnostics_enabled")
        val ADVANCED_PERFORMANCE_DIAGNOSTICS_STARTED_AT =
            longPreferencesKey("advanced_performance_diagnostics_started_at_epoch_ms")
        val ADVANCED_PERFORMANCE_DIAGNOSTICS_EXPIRES_AT =
            longPreferencesKey("advanced_performance_diagnostics_expires_at_epoch_ms")
        val IMMERSIVE_LYRICS_ENABLED = booleanPreferencesKey("immersive_lyrics_enabled")
        val IMMERSIVE_LYRICS_TIMEOUT = longPreferencesKey("immersive_lyrics_timeout")
        val ANIMATED_LYRICS_BLUR_ENABLED = booleanPreferencesKey("animated_lyrics_blur_enabled")
        val ANIMATED_LYRICS_BLUR_STRENGTH = androidx.datastore.preferences.core.floatPreferencesKey("animated_lyrics_blur_strength")

        val IS_GENRE_GRID_VIEW = booleanPreferencesKey("is_genre_grid_view")

        val IS_ALBUMS_LIST_VIEW = booleanPreferencesKey("is_albums_list_view")
        val SHOW_SCROLLBAR = booleanPreferencesKey("show_scrollbar")

        val COLLAGE_PATTERN = stringPreferencesKey("collage_pattern")
        val COLLAGE_AUTO_ROTATE = booleanPreferencesKey("collage_auto_rotate")


        val MIN_SONG_DURATION = intPreferencesKey("min_song_duration_ms")

        val MIN_TRACKS_PER_ALBUM = intPreferencesKey("min_tracks_per_album")

        val REPLAYGAIN_ENABLED = booleanPreferencesKey("replaygain_enabled")
        val REPLAYGAIN_USE_ALBUM_GAIN = booleanPreferencesKey("replaygain_use_album_gain")

        val UPDATE_CHANNEL = stringPreferencesKey("update_channel")
        val UPDATE_AUTO_CHECK = booleanPreferencesKey("update_auto_check")
        val UPDATE_LAST_NOTIFIED_VERSION = stringPreferencesKey("update_last_notified_version")
    }

    /** Null until the user picks a channel; the updater then follows the installed build. */
    val updateChannelFlow: Flow<UpdateChannel?> = dataStore.data.map { preferences ->
        UpdateChannel.fromStorageKey(preferences[PreferencesKeys.UPDATE_CHANNEL])
    }

    suspend fun setUpdateChannel(channel: UpdateChannel) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.UPDATE_CHANNEL] = channel.storageKey }
    }

    /** Null until the user toggles it; the default depends on the installed channel. */
    val updateAutoCheckFlow: Flow<Boolean?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.UPDATE_AUTO_CHECK]
    }

    suspend fun setUpdateAutoCheck(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.UPDATE_AUTO_CHECK] = enabled }
    }

    suspend fun getLastNotifiedUpdateVersion(): String? =
        dataStore.data.first()[PreferencesKeys.UPDATE_LAST_NOTIFIED_VERSION]

    suspend fun setLastNotifiedUpdateVersion(version: String) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.UPDATE_LAST_NOTIFIED_VERSION] = version }
    }

    val appRebrandDialogShownFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.APP_REBRAND_DIALOG_SHOWN] ?: false
            }

    suspend fun setAppRebrandDialogShown(wasShown: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_REBRAND_DIALOG_SHOWN] = wasShown
        }
    }

    /** The year whose seasonal Home recap card the user dismissed, or 0 when none was. */
    val recapHomeCardDismissedYearFlow: Flow<Int> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.RECAP_HOME_CARD_DISMISSED_YEAR] ?: 0
            }.distinctUntilChanged()

    suspend fun setRecapHomeCardDismissedYear(year: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECAP_HOME_CARD_DISMISSED_YEAR] = year
        }
    }

    val isCrossfadeEnabledFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.IS_CROSSFADE_ENABLED] ?: false
            }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_CROSSFADE_ENABLED] = enabled
        }
    }

    val smartCrossfadeEnabledFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.SMART_CROSSFADE_ENABLED] ?: false
            }

    suspend fun setSmartCrossfadeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMART_CROSSFADE_ENABLED] = enabled
        }
    }

    val audioOutputModeFlow: Flow<AudioOutputMode> =
        dataStore.data.map { preferences ->
            AudioOutputMode.fromStorageKey(
                value = preferences[PreferencesKeys.AUDIO_OUTPUT_MODE],
                legacyHiFiEnabled = preferences[PreferencesKeys.HI_FI_MODE_ENABLED] ?: false
            )
        }.distinctUntilChanged()

    suspend fun setAudioOutputMode(mode: AudioOutputMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_OUTPUT_MODE] = mode.storageKey
            preferences.remove(PreferencesKeys.HI_FI_MODE_ENABLED)
        }
    }

    val backupInfoDismissedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BACKUP_INFO_DISMISSED] ?: false
    }

    suspend fun setBackupInfoDismissed(dismissed: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.BACKUP_INFO_DISMISSED] = dismissed }
    }

    enum class EqualizerViewMode {
        SLIDERS, GRAPH, HYBRID
    }

    val crossfadeDurationFlow: Flow<Int> =
            dataStore.data.map { preferences ->
                (preferences[PreferencesKeys.CROSSFADE_DURATION] ?: 2000).coerceIn(1000, 12000)
            }

    suspend fun setCrossfadeDuration(duration: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CROSSFADE_DURATION] = duration.coerceIn(1000, 12000)
        }
    }

    /**
     * Normalized to hundredths on read and write, so legacy noisy values such as 0.74999994f,
     * NaN or backup imports outside the range come back as a clean, supported speed.
     */
    val playbackSpeedFlow: Flow<Float> =
            dataStore.data.map { preferences ->
                PlaybackSpeedUtils.normalize(
                    preferences[PreferencesKeys.PLAYBACK_SPEED] ?: PlaybackSpeedUtils.DEFAULT_SPEED
                )
            }.distinctUntilChanged()

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYBACK_SPEED] = PlaybackSpeedUtils.normalize(speed)
        }
    }

    val repeatModeFlow: Flow<Int> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
            }

    suspend fun setRepeatMode(@Player.RepeatMode mode: Int) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.REPEAT_MODE] = mode }
    }

    val isShuffleOnFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.IS_SHUFFLE_ON] ?: false
            }

    suspend fun setShuffleOn(on: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.IS_SHUFFLE_ON] = on }
    }

    val persistentShuffleEnabledFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.PERSISTENT_SHUFFLE_ENABLED] ?: false
            }

    suspend fun setPersistentShuffleEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.PERSISTENT_SHUFFLE_ENABLED] = enabled }
    }

    val playbackQueueSnapshotFlow: Flow<PlaybackQueueSnapshot?> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.PLAYBACK_QUEUE_SNAPSHOT]?.let { raw ->
                    runCatching { json.decodeFromString<PlaybackQueueSnapshot>(raw) }.getOrNull()
                }
            }

    suspend fun getPlaybackQueueSnapshotOnce(): PlaybackQueueSnapshot? {
        return playbackQueueSnapshotFlow.first()
    }

    suspend fun setPlaybackQueueSnapshot(snapshot: PlaybackQueueSnapshot?) {
        dataStore.edit { preferences ->
            if (snapshot == null || snapshot.items.isEmpty()) {
                preferences.remove(PreferencesKeys.PLAYBACK_QUEUE_SNAPSHOT)
            } else {
                preferences[PreferencesKeys.PLAYBACK_QUEUE_SNAPSHOT] = json.encodeToString(snapshot)
            }
        }
    }

    val artistDelimitersFlow: Flow<List<String>> =
            dataStore.data.map { preferences ->
                val stored = preferences[PreferencesKeys.ARTIST_DELIMITERS]
                if (stored != null) {
                    try {
                        json.decodeFromString<List<String>>(stored)
                    } catch (e: Exception) {
                        DEFAULT_ARTIST_DELIMITERS
                    }
                } else {
                    DEFAULT_ARTIST_DELIMITERS
                }
            }

    suspend fun setArtistDelimiters(delimiters: List<String>) {
        if (delimiters.isEmpty()) {
            return
        }

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARTIST_DELIMITERS] = json.encodeToString(delimiters)
            preferences[PreferencesKeys.ARTIST_SETTINGS_RESCAN_REQUIRED] = true
        }
    }

    suspend fun resetArtistDelimitersToDefault() {
        setArtistDelimiters(DEFAULT_ARTIST_DELIMITERS)
    }

    val artistWordDelimitersFlow: Flow<List<String>> =
        dataStore.data.map { preferences ->
            val stored = preferences[PreferencesKeys.ARTIST_WORD_DELIMITERS]
            if (stored != null) {
                try {
                    json.decodeFromString<List<String>>(stored)
                } catch (e: Exception) {
                    DEFAULT_ARTIST_WORD_DELIMITERS
                }
            } else {
                DEFAULT_ARTIST_WORD_DELIMITERS
            }
        }

    suspend fun setArtistWordDelimiters(delimiters: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARTIST_WORD_DELIMITERS] = json.encodeToString(delimiters)
            preferences[PreferencesKeys.ARTIST_SETTINGS_RESCAN_REQUIRED] = true
        }
    }

    suspend fun resetArtistWordDelimitersToDefault() {
        setArtistWordDelimiters(DEFAULT_ARTIST_WORD_DELIMITERS)
    }

    val extractArtistsFromTitleFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.EXTRACT_ARTISTS_FROM_TITLE] ?: true
        }

    suspend fun setExtractArtistsFromTitle(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXTRACT_ARTISTS_FROM_TITLE] = enabled
            preferences[PreferencesKeys.ARTIST_SETTINGS_RESCAN_REQUIRED] = true
        }
    }

    val groupByAlbumArtistFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.GROUP_BY_ALBUM_ARTIST] ?: false
            }

    suspend fun setGroupByAlbumArtist(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GROUP_BY_ALBUM_ARTIST] = enabled
            preferences[PreferencesKeys.ARTIST_SETTINGS_RESCAN_REQUIRED] = true
        }
    }

    /** Display-only: the artist page reads it when it opens, so no rescan is needed. */
    val artistAlbumsCollapsedByDefaultFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.ARTIST_ALBUMS_COLLAPSED_BY_DEFAULT] ?: false
            }.distinctUntilChanged()

    suspend fun setArtistAlbumsCollapsedByDefault(collapsed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARTIST_ALBUMS_COLLAPSED_BY_DEFAULT] = collapsed
        }
    }

    val artistSettingsRescanRequiredFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.ARTIST_SETTINGS_RESCAN_REQUIRED] ?: false
            }

    suspend fun clearArtistSettingsRescanRequired() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARTIST_SETTINGS_RESCAN_REQUIRED] = false
        }
    }

    val lastSyncTimestampFlow: Flow<Long> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L
            }

    val directoryRulesVersionFlow: Flow<Int> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.DIRECTORY_RULES_VERSION] ?: 0
            }

    val lastAppliedDirectoryRulesVersionFlow: Flow<Int> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.LAST_APPLIED_DIRECTORY_RULES_VERSION] ?: 0
            }

    suspend fun getLastSyncTimestamp(): Long {
        return lastSyncTimestampFlow.first()
    }

    suspend fun getDirectoryRulesVersion(): Int {
        return directoryRulesVersionFlow.first()
    }

    suspend fun getLastAppliedDirectoryRulesVersion(): Int {
        return lastAppliedDirectoryRulesVersionFlow.first()
    }

    /**
     * Version of the metadata text repair the stored library was synced with. Bumping
     * `SyncWorker.CURRENT_METADATA_CHARSET_REPAIR_VERSION` makes the next sync re-check every
     * song once.
     */
    suspend fun getMetadataCharsetRepairVersion(): Int =
        dataStore.data.first()[PreferencesKeys.METADATA_CHARSET_REPAIR_VERSION] ?: 0

    suspend fun setMetadataCharsetRepairVersion(version: Int) {
        dataStore.edit { it[PreferencesKeys.METADATA_CHARSET_REPAIR_VERSION] = version }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun markDirectoryRulesVersionApplied(version: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_APPLIED_DIRECTORY_RULES_VERSION] = version
        }
    }

    /**
     * Lyrics sync offset per song in milliseconds.
     * Stored as a JSON map: { "songId": offsetMs, ... }
     * Positive values = lyrics appear later (use when lyrics are ahead of audio)
     * Negative values = lyrics appear earlier (use when lyrics are behind audio)
     */
    private val lyricsSyncOffsetsFlow: Flow<Map<String, Int>> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.LYRICS_SYNC_OFFSETS]?.let { jsonString ->
                    try {
                        json.decodeFromString<Map<String, Int>>(jsonString)
                    } catch (e: Exception) {
                        emptyMap()
                    }
                } ?: emptyMap()
            }

    fun getLyricsSyncOffsetFlow(songId: String): Flow<Int> {
        return lyricsSyncOffsetsFlow.map { offsets -> offsets[songId] ?: 0 }
    }

    suspend fun getLyricsSyncOffset(songId: String): Int {
        return getLyricsSyncOffsetFlow(songId).first()
    }

    suspend fun setLyricsSyncOffset(songId: String, offsetMs: Int) {
        dataStore.edit { preferences ->
            val currentOffsets = preferences[PreferencesKeys.LYRICS_SYNC_OFFSETS]?.let { jsonString ->
                try {
                    json.decodeFromString<Map<String, Int>>(jsonString).toMutableMap()
                } catch (e: Exception) {
                    mutableMapOf()
                }
            } ?: mutableMapOf()

            if (offsetMs == 0) {
                currentOffsets.remove(songId)
            } else {
                currentOffsets[songId] = offsetMs
            }

            preferences[PreferencesKeys.LYRICS_SYNC_OFFSETS] = json.encodeToString(currentOffsets)
        }
    }

    val lyricsSourcePreferenceFlow: Flow<LyricsSourcePreference> =
            dataStore.data.map { preferences ->
                LyricsSourcePreference.fromName(preferences[PreferencesKeys.LYRICS_SOURCE_PREFERENCE])
            }

    suspend fun setLyricsSourcePreference(preference: LyricsSourcePreference) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LYRICS_SOURCE_PREFERENCE] = preference.name
        }
    }

    val externalLyricsEnabledFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                // Online providers (QQ Music, NetEase, Kugou, LRCLIB, Apple Music) are the default
                // way lyrics are found; only an explicit opt-out disables them.
                preferences[PreferencesKeys.EXTERNAL_LYRICS_ENABLED] ?: true
            }.distinctUntilChanged()

    suspend fun setExternalLyricsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXTERNAL_LYRICS_ENABLED] = enabled
            if (!enabled && preferences[PreferencesKeys.LYRICS_SOURCE_PREFERENCE] == LyricsSourcePreference.API_FIRST.name) {
                preferences[PreferencesKeys.LYRICS_SOURCE_PREFERENCE] = LyricsSourcePreference.EMBEDDED_FIRST.name
            }
        }
    }

    /**
     * The enabled cover sources, highest priority first. The list doubles as the enabled set:
     * a source the user switched off is simply absent.
     */
    val coverArtSourceOrderFlow: Flow<List<CoverArtSource>> =
            dataStore.data.map { preferences ->
                CoverArtSource.decode(preferences[PreferencesKeys.COVER_ART_SOURCE_ORDER])
            }.distinctUntilChanged()

    suspend fun setCoverArtSourceOrder(order: List<CoverArtSource>) {
        if (order.isEmpty()) return
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.COVER_ART_SOURCE_ORDER] = CoverArtSource.encode(order)
        }
    }

    /**
     * Whether only the players listed in [externalPlayerAllowedPackagesFlow] may publish a media
     * session. Off by default, so an install that never opens the filter follows every player.
     */
    val externalPlayerFilterEnabledFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.EXTERNAL_PLAYER_FILTER_ENABLED] ?: false
        }.distinctUntilChanged()

    suspend fun setExternalPlayerFilterEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXTERNAL_PLAYER_FILTER_ENABLED] = enabled
        }
    }

    /** Package names allowed to publish a media session while the filter is on. */
    val externalPlayerAllowedPackagesFlow: Flow<Set<String>> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.EXTERNAL_PLAYER_ALLOWED_PACKAGES].orEmpty()
        }.distinctUntilChanged()

    suspend fun setExternalPlayerAllowedPackages(packages: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXTERNAL_PLAYER_ALLOWED_PACKAGES] = packages
        }
    }

    val floatingLyricsEnabledFlow: Flow<Boolean> =
        dataStore.data
            .map { preferences ->
                preferences[FLOATING_LYRICS_ENABLED_KEY] ?: DEFAULT_FLOATING_LYRICS_ENABLED
            }
            .distinctUntilChanged()

    val lockScreenLyricsEnabledFlow: Flow<Boolean> =
        dataStore.data
            .map { preferences ->
                preferences[LOCK_SCREEN_LYRICS_ENABLED_KEY] ?: DEFAULT_LOCK_SCREEN_LYRICS_ENABLED
            }
            .distinctUntilChanged()

    suspend fun setFloatingLyricsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[FLOATING_LYRICS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setLockScreenLyricsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LOCK_SCREEN_LYRICS_ENABLED_KEY] = enabled
        }
    }

    val immersiveLyricsEnabledFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.IMMERSIVE_LYRICS_ENABLED] ?: false
            }

    val immersiveLyricsTimeoutFlow: Flow<Long> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.IMMERSIVE_LYRICS_TIMEOUT] ?: 4000L
            }

    suspend fun setImmersiveLyricsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IMMERSIVE_LYRICS_ENABLED] = enabled
        }
    }

    suspend fun setImmersiveLyricsTimeout(timeout: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IMMERSIVE_LYRICS_TIMEOUT] = timeout
        }
    }

    val dailyMixSongIdsFlow: Flow<List<String>> =
            dataStore.data.map { preferences ->
                val jsonString = preferences[PreferencesKeys.DAILY_MIX_SONG_IDS]
                if (jsonString != null) {
                    try {
                        json.decodeFromString<List<String>>(jsonString)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }

    suspend fun saveDailyMixSongIds(songIds: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_MIX_SONG_IDS] = json.encodeToString(songIds)
        }
    }

    val yourMixSongIdsFlow: Flow<List<String>> =
            dataStore.data.map { preferences ->
                val jsonString = preferences[PreferencesKeys.YOUR_MIX_SONG_IDS]
                if (jsonString != null) {
                    try {
                        json.decodeFromString<List<String>>(jsonString)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }

    suspend fun saveYourMixSongIds(songIds: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.YOUR_MIX_SONG_IDS] = json.encodeToString(songIds)
        }
    }

    val isGenreGridViewFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.IS_GENRE_GRID_VIEW] ?: true
        }

    suspend fun setGenreGridView(isGrid: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_GENRE_GRID_VIEW] = isGrid
        }
    }

    val isAlbumsListViewFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.IS_ALBUMS_LIST_VIEW] ?: false
        }

    suspend fun setAlbumsListView(isList: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ALBUMS_LIST_VIEW] = isList
        }
    }

    val showScrollbarFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SHOW_SCROLLBAR] ?: true
        }

    suspend fun setShowScrollbar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_SCROLLBAR] = enabled
        }
    }

    val lastDailyMixUpdateFlow: Flow<Long> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.LAST_DAILY_MIX_UPDATE] ?: 0L
            }

    suspend fun saveLastDailyMixUpdateTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_DAILY_MIX_UPDATE] = timestamp
        }
    }

    /** Minimum song duration in milliseconds. Default 10000ms (10 seconds). */
    val minSongDurationFlow: Flow<Int> =
        dataStore.data.map { preferences ->
            (preferences[PreferencesKeys.MIN_SONG_DURATION] ?: 10000).coerceIn(0, 120000)
        }

    suspend fun setMinSongDuration(durationMs: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MIN_SONG_DURATION] = durationMs.coerceIn(0, 120000)
        }
    }

    suspend fun getMinSongDuration(): Int {
        return minSongDurationFlow.first()
    }

    val minTracksPerAlbumFlow: Flow<Int> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.MIN_TRACKS_PER_ALBUM] ?: 1
        }.distinctUntilChanged()

    suspend fun setMinTracksPerAlbum(minTracks: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MIN_TRACKS_PER_ALBUM] = minTracks
        }
    }

    val replayGainEnabledFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.REPLAYGAIN_ENABLED] ?: false
        }

    val replayGainUseAlbumGainFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.REPLAYGAIN_USE_ALBUM_GAIN] ?: false
        }

    suspend fun setReplayGainEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPLAYGAIN_ENABLED] = enabled
        }
    }

    suspend fun setReplayGainUseAlbumGain(useAlbumGain: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPLAYGAIN_USE_ALBUM_GAIN] = useAlbumGain
        }
    }

    val allowedDirectoriesFlow: Flow<Set<String>> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.ALLOWED_DIRECTORIES] ?: emptySet()
            }.distinctUntilChanged()

    val blockedDirectoriesFlow: Flow<Set<String>> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.BLOCKED_DIRECTORIES] ?: emptySet()
            }.distinctUntilChanged()

    /**
     * Directories the local music page imported. They scope the deep local search for embedded
     * lyrics, embedded album art and sibling .lrc files; they do not limit the song library,
     * which still comes from the whole-device MediaStore scan.
     */
    val localLibraryFoldersFlow: Flow<Set<String>> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.LOCAL_LIBRARY_FOLDERS] ?: emptySet()
            }.distinctUntilChanged()

    suspend fun setLocalLibraryFolders(folders: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCAL_LIBRARY_FOLDERS] = folders
        }
    }

    val initialSetupDoneFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.INITIAL_SETUP_DONE] ?: false
            }

    val keepPlayingInBackgroundFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.KEEP_PLAYING_IN_BACKGROUND] ?: true
            }

    val keepScreenAwakeWhilePlayingFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.KEEP_SCREEN_AWAKE_WHILE_PLAYING] ?: false
            }.distinctUntilChanged()

    val resumeOnHeadsetReconnectFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.RESUME_ON_HEADSET_RECONNECT] ?: false
            }

    val showQueueHistoryFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.SHOW_QUEUE_HISTORY] ?: false
            }

    suspend fun setShowQueueHistory(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_QUEUE_HISTORY] = show
        }
    }

    val showPlayerFileInfoFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.FULL_PLAYER_SHOW_FILE_INFO] ?: true
            }

    suspend fun setShowPlayerFileInfo(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_SHOW_FILE_INFO] = show
        }
    }

    val fullPlayerLoadingTweaksFlow: Flow<FullPlayerLoadingTweaks> = dataStore.data
        .map { preferences ->
            val delayAlbum = preferences[PreferencesKeys.FULL_PLAYER_DELAY_ALBUM] ?: true
            val delayMetadata = preferences[PreferencesKeys.FULL_PLAYER_DELAY_METADATA] ?: true
            val delayProgress = preferences[PreferencesKeys.FULL_PLAYER_DELAY_PROGRESS] ?: true
            val delayControls = preferences[PreferencesKeys.FULL_PLAYER_DELAY_CONTROLS] ?: true

            val delayAll = delayAlbum && delayMetadata && delayProgress && delayControls

            FullPlayerLoadingTweaks(
                delayAll = delayAll,
                delayAlbumCarousel = delayAlbum,
                delaySongMetadata = delayMetadata,
                delayProgressBar = delayProgress,
                delayControls = delayControls,
                showPlaceholders = preferences[PreferencesKeys.FULL_PLAYER_PLACEHOLDERS] ?: true,
                transparentPlaceholders = preferences[PreferencesKeys.FULL_PLAYER_PLACEHOLDER_TRANSPARENT] ?: false,
                applyPlaceholdersOnClose = preferences[PreferencesKeys.FULL_PLAYER_PLACEHOLDERS_ON_CLOSE] ?: false,
                switchOnDragRelease = preferences[PreferencesKeys.FULL_PLAYER_SWITCH_ON_DRAG_RELEASE] ?: true,
                contentAppearThresholdPercent = preferences[PreferencesKeys.FULL_PLAYER_DELAY_THRESHOLD] ?: 98,
                contentCloseThresholdPercent = preferences[PreferencesKeys.FULL_PLAYER_CLOSE_THRESHOLD] ?: 0
            )
        }

    suspend fun updateAllowedDirectories(allowedPaths: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALLOWED_DIRECTORIES] = allowedPaths
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = 0L
            val currentVersion = preferences[PreferencesKeys.DIRECTORY_RULES_VERSION] ?: 0
            preferences[PreferencesKeys.DIRECTORY_RULES_VERSION] =
                if (currentVersion == Int.MAX_VALUE) 0 else currentVersion + 1
        }
    }

    suspend fun updateDirectorySelections(allowedPaths: Set<String>, blockedPaths: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALLOWED_DIRECTORIES] = allowedPaths
            preferences[PreferencesKeys.BLOCKED_DIRECTORIES] = blockedPaths
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = 0L
            val currentVersion = preferences[PreferencesKeys.DIRECTORY_RULES_VERSION] ?: 0
            preferences[PreferencesKeys.DIRECTORY_RULES_VERSION] =
                if (currentVersion == Int.MAX_VALUE) 0 else currentVersion + 1
        }
    }

    suspend fun setInitialSetupDone(isDone: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.INITIAL_SETUP_DONE] = isDone }
    }

    val songsSortOptionFlow: Flow<String> =
            dataStore.data.map { preferences ->
                SortOption.fromStorageKey(
                                preferences[PreferencesKeys.SONGS_SORT_OPTION],
                                SortOption.SONGS,
                                SortOption.SongTitleAZ
                        )
                        .storageKey
            }

    val albumsSortOptionFlow: Flow<String> =
            dataStore.data.map { preferences ->
                SortOption.fromStorageKey(
                                preferences[PreferencesKeys.ALBUMS_SORT_OPTION],
                                SortOption.ALBUMS,
                                SortOption.AlbumTitleAZ
                        )
                        .storageKey
            }

    val artistsSortOptionFlow: Flow<String> =
            dataStore.data.map { preferences ->
                SortOption.fromStorageKey(
                                preferences[PreferencesKeys.ARTISTS_SORT_OPTION],
                                SortOption.ARTISTS,
                                SortOption.ArtistNameAZ
                        )
                        .storageKey
            }

    val foldersSortOptionFlow: Flow<String> =
            dataStore.data.map { preferences ->
                SortOption.fromStorageKey(
                                preferences[PreferencesKeys.FOLDERS_SORT_OPTION],
                                SortOption.FOLDERS,
                                SortOption.FolderNameAZ
                        )
                        .storageKey
            }

    val folderSongsSortOptionFlow: Flow<String> =
            dataStore.data.map { preferences ->
                resolveFolderSongsSortOption(preferences).storageKey
            }

    /**
     * Folder songs used to follow the folder sort: Name (Z-A) listed them Z-A too. Until a folder
     * songs sort is stored, that order is derived from the folder sort so updating keeps it.
     */
    private fun resolveFolderSongsSortOption(preferences: Preferences): SortOption {
        val stored = preferences[PreferencesKeys.FOLDER_SONGS_SORT_OPTION]
        if (stored != null) {
            return SortOption.fromStorageKey(
                    stored,
                    SortOption.FOLDER_SONGS,
                    SortOption.FolderSongTitleAZ
            )
        }
        val folderSort = SortOption.fromStorageKey(
                preferences[PreferencesKeys.FOLDERS_SORT_OPTION],
                SortOption.FOLDERS,
                SortOption.FolderNameAZ
        )
        return if (folderSort == SortOption.FolderNameZA) {
            SortOption.FolderSongTitleZA
        } else {
            SortOption.FolderSongTitleAZ
        }
    }

    val genresSortOptionFlow: Flow<String> =
            dataStore.data.map { preferences ->
                SortOption.fromStorageKey(
                                preferences[PreferencesKeys.GENRES_SORT_OPTION],
                                SortOption.GENRES,
                                SortOption.GenreNameAZ
                        )
                        .storageKey
            }

    suspend fun setSongsSortOption(optionKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SONGS_SORT_OPTION] = optionKey
            preferences[PreferencesKeys.SONGS_SORT_OPTION_MIGRATED] = true
        }
    }

    suspend fun setAlbumsSortOption(optionKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALBUMS_SORT_OPTION] = optionKey
        }
    }

    suspend fun setArtistsSortOption(optionKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARTISTS_SORT_OPTION] = optionKey
        }
    }

    suspend fun setFoldersSortOption(optionKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLDERS_SORT_OPTION] = optionKey
        }
    }

    suspend fun setFolderSongsSortOption(optionKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLDER_SONGS_SORT_OPTION] = optionKey
        }
    }

    suspend fun setGenresSortOption(optionKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GENRES_SORT_OPTION] = optionKey
        }
    }

    suspend fun ensureLibrarySortDefaults() {
        dataStore.edit { preferences ->
            val songsMigrated = preferences[PreferencesKeys.SONGS_SORT_OPTION_MIGRATED] ?: false
            val rawSongSort = preferences[PreferencesKeys.SONGS_SORT_OPTION]
            val resolvedSongSort =
                    SortOption.fromStorageKey(rawSongSort, SortOption.SONGS, SortOption.SongTitleAZ)
            val shouldForceSongDefault =
                    !songsMigrated &&
                            (rawSongSort.isNullOrBlank() ||
                                    rawSongSort == SortOption.SongTitleZA.storageKey ||
                                    rawSongSort == SortOption.SongTitleZA.displayName)

            preferences[PreferencesKeys.SONGS_SORT_OPTION] =
                    if (shouldForceSongDefault) {
                        SortOption.SongTitleAZ.storageKey
                    } else {
                        resolvedSongSort.storageKey
                    }
            if (!songsMigrated) {
                preferences[PreferencesKeys.SONGS_SORT_OPTION_MIGRATED] = true
            }

            migrateSortPreference(
                    preferences,
                    PreferencesKeys.SONGS_SORT_OPTION,
                    SortOption.SONGS,
                    SortOption.SongTitleAZ
            )
            migrateSortPreference(
                    preferences,
                    PreferencesKeys.ALBUMS_SORT_OPTION,
                    SortOption.ALBUMS,
                    SortOption.AlbumTitleAZ
            )
            migrateSortPreference(
                    preferences,
                    PreferencesKeys.ARTISTS_SORT_OPTION,
                    SortOption.ARTISTS,
                    SortOption.ArtistNameAZ
            )
            migrateSortPreference(
                    preferences,
                    PreferencesKeys.FOLDERS_SORT_OPTION,
                    SortOption.FOLDERS,
                    SortOption.FolderNameAZ
            )
            // Stored once, so the folder songs sort stops following a later folder sort change.
            val folderSongsSort = resolveFolderSongsSortOption(preferences).storageKey
            if (preferences[PreferencesKeys.FOLDER_SONGS_SORT_OPTION] != folderSongsSort) {
                preferences[PreferencesKeys.FOLDER_SONGS_SORT_OPTION] = folderSongsSort
            }
            migrateSortPreference(
                    preferences,
                    PreferencesKeys.GENRES_SORT_OPTION,
                    SortOption.GENRES,
                    SortOption.GenreNameAZ
            )
        }
    }

    private fun migrateSortPreference(
            preferences: MutablePreferences,
            key: Preferences.Key<String>,
            allowed: Collection<SortOption>,
            fallback: SortOption
    ) {
        val resolved = SortOption.fromStorageKey(preferences[key], allowed, fallback)
        if (preferences[key] != resolved.storageKey) {
            preferences[key] = resolved.storageKey
        }
    }

    val lastLibraryTabIndexFlow: Flow<Int> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.LAST_LIBRARY_TAB_INDEX] ?: 0
            }

    suspend fun saveLastLibraryTabIndex(tabIndex: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_LIBRARY_TAB_INDEX] = tabIndex
        }
    }

    val lastStorageFilterFlow: Flow<StorageFilter> =
        dataStore.data.map { preferences ->
            when (preferences[PreferencesKeys.LAST_STORAGE_FILTER]) {
                "ONLINE"  -> StorageFilter.ONLINE
                "OFFLINE" -> StorageFilter.OFFLINE
                else      -> StorageFilter.ALL
            }
        }

    suspend fun saveLastStorageFilter(filter: StorageFilter) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_STORAGE_FILTER] = filter.name
        }
    }

    val mockGenresEnabledFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.MOCK_GENRES_ENABLED] ?: false
            }

    suspend fun setMockGenresEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.MOCK_GENRES_ENABLED] = enabled }
    }

    companion object {
        /** Default character delimiters for splitting multi-artist tags */
        val DEFAULT_ARTIST_DELIMITERS = listOf("/", ";", ",", "+", "&")
        /** Default word-based delimiters (matched case-insensitively with whitespace boundaries) */
        val DEFAULT_ARTIST_WORD_DELIMITERS = listOf("featuring", "feat.", "feat", "ft.", "ft", "vs.", "vs", "versus", "with", "prod.", "prod")
        const val DEFAULT_ALBUM_ART_CACHE_LIMIT_MB = 200

        /**
         * Synchronous read of the folder-cover-art opt-in.
         *
         * [io.github.jayfunc.carpelines.utils.AlbumArtUtils] resolves artwork from a Coil fetcher
         * and a ContentProvider, both off the main thread, and it cannot proceed without knowing
         * this value: resolving under a wrong assumption persists a cache entry or a "no art"
         * marker that later reads then trust. The mirror is normally pushed in long before any
         * artwork is requested, so this only runs if a very early request wins that race.
         */
        fun readUseFolderAlbumArtBlocking(context: Context): Boolean = runBlocking {
            runCatching {
                context.applicationContext.dataStore.data.first()[PreferencesKeys.USE_FOLDER_ALBUM_ART]
            }.getOrNull() ?: false
        }
    }

    val navBarCornerRadiusFlow: Flow<Int> =
            dataStore.data.map { preferences ->
                sanitizeNavBarCornerRadius(preferences[PreferencesKeys.NAV_BAR_CORNER_RADIUS] ?: 32)
            }

    suspend fun setNavBarCornerRadius(radius: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NAV_BAR_CORNER_RADIUS] = sanitizeNavBarCornerRadius(radius)
        }
    }

    val navBarStyleFlow: Flow<String> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.NAV_BAR_STYLE] ?: NavBarStyle.DEFAULT
            }

    suspend fun setNavBarStyle(style: String) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.NAV_BAR_STYLE] = style }
    }

    val navBarCompactModeFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.NAV_BAR_COMPACT_MODE] ?: false
            }

    suspend fun setNavBarCompactMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NAV_BAR_COMPACT_MODE] = enabled
        }
    }

    val libraryNavigationModeFlow: Flow<String> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.LIBRARY_NAVIGATION_MODE]
                        ?: LibraryNavigationMode.TAB_ROW
            }

    suspend fun setLibraryNavigationMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LIBRARY_NAVIGATION_MODE] = mode
        }
    }

    val carouselStyleFlow: Flow<String> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.CAROUSEL_STYLE] ?: CarouselStyle.NO_PEEK
            }

    suspend fun setCarouselStyle(style: String) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.CAROUSEL_STYLE] = style }
    }

    val launchTabFlow: Flow<String> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.LAUNCH_TAB] ?: LaunchTab.HOME
            }

    suspend fun setLaunchTab(tab: String) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.LAUNCH_TAB] = tab }
    }

    suspend fun setKeepPlayingInBackground(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_PLAYING_IN_BACKGROUND] = enabled
        }
    }

    suspend fun setKeepScreenAwakeWhilePlaying(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_SCREEN_AWAKE_WHILE_PLAYING] = enabled
        }
    }

    suspend fun setResumeOnHeadsetReconnect(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RESUME_ON_HEADSET_RECONNECT] = enabled
        }
    }

    suspend fun setDelayAllFullPlayerContent(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_DELAY_ALL] = enabled

            preferences[PreferencesKeys.FULL_PLAYER_DELAY_ALBUM] = enabled
            preferences[PreferencesKeys.FULL_PLAYER_DELAY_METADATA] = enabled
            preferences[PreferencesKeys.FULL_PLAYER_DELAY_PROGRESS] = enabled
            preferences[PreferencesKeys.FULL_PLAYER_DELAY_CONTROLS] = enabled
        }
    }

    suspend fun setDelayAlbumCarousel(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_DELAY_ALBUM] = enabled
        }
    }

    suspend fun setDelaySongMetadata(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_DELAY_METADATA] = enabled
        }
    }

    suspend fun setDelayProgressBar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_DELAY_PROGRESS] = enabled
        }
    }

    suspend fun setDelayControls(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_DELAY_CONTROLS] = enabled
        }
    }

    suspend fun setFullPlayerPlaceholders(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_PLACEHOLDERS] = enabled
            if (!enabled) {
                preferences[PreferencesKeys.FULL_PLAYER_PLACEHOLDER_TRANSPARENT] = false
            }
        }
    }

    suspend fun setTransparentPlaceholders(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_PLACEHOLDER_TRANSPARENT] = enabled
        }
    }

    suspend fun setFullPlayerPlaceholdersOnClose(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_PLACEHOLDERS_ON_CLOSE] = enabled
        }
    }

    suspend fun setFullPlayerSwitchOnDragRelease(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_SWITCH_ON_DRAG_RELEASE] = enabled
        }
    }

    suspend fun setFullPlayerAppearThreshold(thresholdPercent: Int) {
        val coercedValue = thresholdPercent.coerceIn(0, 100)
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_DELAY_THRESHOLD] = coercedValue
        }
    }

    suspend fun setFullPlayerCloseThreshold(thresholdPercent: Int) {
        val coercedValue = thresholdPercent.coerceIn(0, 100)
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_PLAYER_CLOSE_THRESHOLD] = coercedValue
        }
    }

    suspend fun clearDeprecatedPlayerSheetPreference() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USE_PLAYER_SHEET_V2)
        }
    }

    val animatedLyricsBlurEnabledFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ANIMATED_LYRICS_BLUR_ENABLED] ?: true
        }

    suspend fun setAnimatedLyricsBlurEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANIMATED_LYRICS_BLUR_ENABLED] = enabled
        }
    }

    val animatedLyricsBlurStrengthFlow: Flow<Float> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ANIMATED_LYRICS_BLUR_STRENGTH] ?: 5f
        }

    suspend fun setAnimatedLyricsBlurStrength(strength: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANIMATED_LYRICS_BLUR_STRENGTH] = strength
        }
    }

    val lyricsFontSizeFlow: Flow<Float> = dataStore.data
        .map { preferences ->
            sanitizeLyricsFontSize(preferences[LYRICS_FONT_SIZE_SP_KEY] ?: DEFAULT_LYRICS_FONT_SIZE_SP)
        }
        .distinctUntilChanged()

    suspend fun setLyricsFontSize(fontSizeSp: Float) {
        dataStore.edit { preferences ->
            preferences[LYRICS_FONT_SIZE_SP_KEY] = sanitizeLyricsFontSize(fontSizeSp)
        }
    }

    val lyricsDuetLayoutEnabledFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[LYRICS_DUET_LAYOUT_KEY] ?: DEFAULT_LYRICS_DUET_LAYOUT_ENABLED
        }
        .distinctUntilChanged()

    suspend fun setLyricsDuetLayoutEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LYRICS_DUET_LAYOUT_KEY] = enabled
        }
    }

    val showLyricsBackgroundVocalsFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SHOW_LYRICS_BACKGROUND_VOCALS_KEY] ?: DEFAULT_SHOW_LYRICS_BACKGROUND_VOCALS
        }
        .distinctUntilChanged()

    suspend fun setShowLyricsBackgroundVocals(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_LYRICS_BACKGROUND_VOCALS_KEY] = enabled
        }
    }

    val libraryTabsOrderFlow: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LIBRARY_TABS_ORDER]
        }

    suspend fun saveLibraryTabsOrder(order: String) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.LIBRARY_TABS_ORDER] = order }
    }

    suspend fun resetLibraryTabsOrder() {
        dataStore.edit { preferences -> preferences.remove(PreferencesKeys.LIBRARY_TABS_ORDER) }
    }

    /**
     * One-shot backfill of the Folders tab for orders saved before it existed.
     *
     * The stored order doubles as the list of *visible* tabs, so this must never run more
     * than once: re-adding FOLDERS on every launch would undo a user who deliberately hid
     * that tab.
     */
    suspend fun migrateTabOrder() {
        dataStore.edit { preferences ->
            if (preferences[PreferencesKeys.LIBRARY_TABS_FOLDERS_MIGRATED] == true) return@edit
            preferences[PreferencesKeys.LIBRARY_TABS_FOLDERS_MIGRATED] = true

            val orderJson = preferences[PreferencesKeys.LIBRARY_TABS_ORDER] ?: return@edit
            val order = runCatching {
                json.decodeFromString<MutableList<String>>(orderJson)
            }.getOrNull() ?: return@edit // Abort on malformed data; don't overwrite user data.

            if ("FOLDERS" !in order) {
                val insertAfter = order.lastIndex
                order.add(insertAfter + 1, "FOLDERS")
                preferences[PreferencesKeys.LIBRARY_TABS_ORDER] = json.encodeToString(order)
            }
        }
    }

    /**
     * One-shot backfill of the Genres tab, appended last, for orders saved before it existed.
     *
     * Like [migrateTabOrder] it runs once, so a user who later hides Genres keeps it hidden.
     * Appending keeps every existing tab at its index, which the remembered last tab relies on.
     */
    suspend fun migrateGenresTab() {
        dataStore.edit { preferences ->
            if (preferences[PreferencesKeys.LIBRARY_TABS_GENRES_MIGRATED] == true) return@edit
            preferences[PreferencesKeys.LIBRARY_TABS_GENRES_MIGRATED] = true

            val orderJson = preferences[PreferencesKeys.LIBRARY_TABS_ORDER] ?: return@edit
            val order = runCatching {
                json.decodeFromString<MutableList<String>>(orderJson)
            }.getOrNull() ?: return@edit // Abort on malformed data; don't overwrite user data.

            if (order.isNotEmpty() && "GENRES" !in order) {
                order.add("GENRES")
                preferences[PreferencesKeys.LIBRARY_TABS_ORDER] = json.encodeToString(order)
            }
        }
    }

    val isFolderFilterActiveFlow: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.IS_FOLDER_FILTER_ACTIVE] ?: false
            }

    suspend fun setFolderFilterActive(isActive: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FOLDER_FILTER_ACTIVE] = isActive
        }
    }

    val isFoldersPlaylistViewFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_FOLDERS_PLAYLIST_VIEW] ?: false
        }

    val hideLocalMediaFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HIDE_LOCAL_MEDIA] ?: false
        }.distinctUntilChanged()

    val foldersSourceFlow: Flow<FolderSource> = dataStore.data
        .map { preferences ->
            FolderSource.fromStorageKey(preferences[PreferencesKeys.FOLDERS_SOURCE])
        }

    val folderBackGestureNavigationFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FOLDER_BACK_GESTURE_NAVIGATION] ?: true
        }

    val useSmoothCornersFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USE_SMOOTH_CORNERS] ?: false
        }

    suspend fun setUseSmoothCorners(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_SMOOTH_CORNERS] = enabled
        }
    }

    suspend fun setFoldersPlaylistView(isPlaylistView: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FOLDERS_PLAYLIST_VIEW] = isPlaylistView
        }
    }

    suspend fun setHideLocalMedia(hide: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_LOCAL_MEDIA] = hide
        }
    }

    suspend fun setFoldersSource(source: FolderSource) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLDERS_SOURCE] = source.storageKey
        }
    }

    suspend fun setFolderBackGestureNavigation(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLDER_BACK_GESTURE_NAVIGATION] = enabled
        }
    }

    /**
     * Album art quality for player view.
     * Controls the maximum resolution for album artwork displayed in the full player.
     * Thumbnails in lists always use low resolution (256px) for optimal performance.
     */
    val albumArtQualityFlow: Flow<AlbumArtQuality> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ALBUM_ART_QUALITY]
                ?.let {
                    try { AlbumArtQuality.valueOf(it) }
                    catch (e: Exception) { AlbumArtQuality.MEDIUM }
                }
                ?: AlbumArtQuality.MEDIUM
        }

    suspend fun setAlbumArtQuality(quality: AlbumArtQuality) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALBUM_ART_QUALITY] = quality.name
        }
    }

    val albumArtCacheLimitMbFlow: Flow<Int> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ALBUM_ART_CACHE_LIMIT_MB] ?: DEFAULT_ALBUM_ART_CACHE_LIMIT_MB
        }

    suspend fun setAlbumArtCacheLimitMb(limitMb: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALBUM_ART_CACHE_LIMIT_MB] = limitMb.coerceIn(50, 1500)
        }
    }

    /**
     * Whether a cover image sitting next to the audio file (cover.jpg, folder.jpg, …) should be
     * used as album art in preference to embedded artwork. Off by default: folder images are only
     * trusted when the user explicitly opts in, because generic directories can hold unrelated
     * pictures. See [io.github.jayfunc.carpelines.utils.AlbumArtUtils.findExternalAlbumArtFile].
     */
    val useFolderAlbumArtFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.USE_FOLDER_ALBUM_ART] ?: false
        }.distinctUntilChanged()

    suspend fun setUseFolderAlbumArt(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_FOLDER_ALBUM_ART] = enabled
        }
    }

    /**
     * The effective folder-cover state the artwork cache was last built under, or null if it has
     * never been recorded.
     *
     * Persisted because the cache it describes is: covers live in `filesDir` and outlive the
     * process, so an in-memory flag could not tell whether entries were produced with or without
     * folder access. Effective means the preference *and* the image permission — losing the
     * permission silently changes what gets cached just as much as switching the setting off.
     */
    val folderAlbumArtCacheStateFlow: Flow<Boolean?> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.FOLDER_ALBUM_ART_CACHE_STATE]
        }

    suspend fun setFolderAlbumArtCacheState(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLDER_ALBUM_ART_CACHE_STATE] = enabled
        }
    }

    /**
     * Whether tapping the background area of the player sheet closes it.
     * Default is false to avoid accidental dismissals while interacting with the full player.
     */
    val tapBackgroundClosesPlayerFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.TAP_BACKGROUND_CLOSES_PLAYER] ?: false
        }

    suspend fun setTapBackgroundClosesPlayer(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TAP_BACKGROUND_CLOSES_PLAYER] = enabled
        }
    }

    /**
     * Whether horizontal swipes on the mini player change track, moving dismissal to a downward
     * swipe. Off by default, where short swipes change track and long swipes dismiss.
     */
    val miniPlayerSwipeToSkipFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.MINI_PLAYER_SWIPE_TO_SKIP] ?: false
        }

    suspend fun setMiniPlayerSwipeToSkip(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MINI_PLAYER_SWIPE_TO_SKIP] = enabled
        }
    }

    /**
     * Whether skipping to another song from the app's own controls while paused starts
     * playback. On by default.
     */
    val autoplayOnSkipFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.AUTOPLAY_ON_SKIP] ?: true
        }.distinctUntilChanged()

    suspend fun setAutoplayOnSkip(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOPLAY_ON_SKIP] = enabled
        }
    }

    /**
     * What previous does partway through a song. Applies to the app's own controls and to the
     * session (notification, lock screen, headset keys, Android Auto, widget). Restarts by default.
     */
    val previousButtonBehaviorFlow: Flow<PreviousButtonBehavior> =
        dataStore.data.map { preferences ->
            PreviousButtonBehavior.fromStorageKey(preferences[PreferencesKeys.PREVIOUS_BUTTON_BEHAVIOR])
        }.distinctUntilChanged()

    suspend fun setPreviousButtonBehavior(behavior: PreviousButtonBehavior) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREVIOUS_BUTTON_BEHAVIOR] = behavior.storageKey
        }
    }

    val hapticsEnabledFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true
        }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTICS_ENABLED] = enabled
        }
    }

    val advancedPerformanceDiagnosticsSettingsFlow: Flow<AdvancedPerformanceDiagnosticsSettings> =
        dataStore.data.map { preferences ->
            AdvancedPerformanceDiagnosticsSettings(
                enabled = preferences[PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_ENABLED] ?: false,
                sessionStartedEpochMs =
                    preferences[PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_STARTED_AT],
                expiresAtEpochMs =
                    preferences[PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_EXPIRES_AT]
            )
        }.distinctUntilChanged()

    suspend fun setAdvancedPerformanceDiagnosticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            if (enabled) {
                val now = System.currentTimeMillis()
                preferences[PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_ENABLED] = true
                preferences[PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_STARTED_AT] = now
                preferences[PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_EXPIRES_AT] =
                    now + AdvancedPerformanceDiagnostics.DEFAULT_SESSION_DURATION_MS
            } else {
                preferences[PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_ENABLED] = false
                preferences.remove(PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_STARTED_AT)
                preferences.remove(PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_EXPIRES_AT)
            }
        }
    }

    suspend fun disableExpiredAdvancedPerformanceDiagnostics(nowEpochMs: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            val enabled = preferences[PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_ENABLED] ?: false
            val expiresAt = preferences[PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_EXPIRES_AT]
            if (enabled && (expiresAt == null || nowEpochMs >= expiresAt)) {
                preferences[PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_ENABLED] = false
                preferences.remove(PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_STARTED_AT)
                preferences.remove(PreferencesKeys.ADVANCED_PERFORMANCE_DIAGNOSTICS_EXPIRES_AT)
            }
        }
    }

    suspend fun clearPreferencesByKeys(keyNames: Set<String>) {
        if (keyNames.isEmpty()) return
        dataStore.edit { preferences ->
            preferences.asMap().keys
                .filter { key -> key.name in keyNames && key.name !in backupExcludedKeyNames }
                .forEach { key ->
                    @Suppress("UNCHECKED_CAST")
                    preferences.remove(key as Preferences.Key<Any>)
                }
        }
    }

    suspend fun clearPreferencesExceptKeys(excludedKeyNames: Set<String>) {
        val protectedKeyNames = excludedKeyNames + backupExcludedKeyNames
        dataStore.edit { preferences ->
            preferences.asMap().keys
                .filterNot { key -> key.name in protectedKeyNames }
                .forEach { key ->
                    @Suppress("UNCHECKED_CAST")
                    preferences.remove(key as Preferences.Key<Any>)
                }
        }
    }

    suspend fun exportPreferencesForBackup(): List<PreferenceBackupEntry> {
        val snapshot = dataStore.data.first().asMap()
        return snapshot.mapNotNull { (key, value) ->
            val keyName = key.name
            if (keyName in backupExcludedKeyNames) {
                return@mapNotNull null
            }
            when (value) {
                is String -> PreferenceBackupEntry(
                    key = keyName,
                    type = "string",
                    stringValue = value
                )
                is Int -> PreferenceBackupEntry(
                    key = keyName,
                    type = "int",
                    intValue = value
                )
                is Long -> PreferenceBackupEntry(
                    key = keyName,
                    type = "long",
                    longValue = value
                )
                is Boolean -> PreferenceBackupEntry(
                    key = keyName,
                    type = "boolean",
                    booleanValue = value
                )
                is Float -> PreferenceBackupEntry(
                    key = keyName,
                    type = "float",
                    floatValue = value
                )
                is Double -> PreferenceBackupEntry(
                    key = keyName,
                    type = "double",
                    doubleValue = value
                )
                is Set<*> -> {
                    val stringSet = value.filterIsInstance<String>().toSet()
                    PreferenceBackupEntry(
                        key = keyName,
                        type = "string_set",
                        stringSetValue = stringSet
                    )
                }
                else -> null
            }
        }
    }

    suspend fun importPreferencesFromBackup(
        entries: List<PreferenceBackupEntry>,
        clearExisting: Boolean = true
    ) {
        dataStore.edit { preferences ->
            if (clearExisting) {
                preferences.asMap().keys
                    .filterNot { key -> key.name in backupExcludedKeyNames }
                    .forEach { key ->
                        @Suppress("UNCHECKED_CAST")
                        preferences.remove(key as Preferences.Key<Any>)
                    }
            }

            entries.forEach { entry ->
                if (entry.key in backupExcludedKeyNames) {
                    return@forEach
                }
                when (entry.type) {
                    "string" -> {
                        val value = entry.stringValue ?: return@forEach
                        preferences[stringPreferencesKey(entry.key)] = value
                    }
                    "int" -> {
                        val value = entry.intValue
                            ?: entry.doubleValue?.toInt()
                            ?: entry.longValue?.toInt()
                            ?: return@forEach
                        preferences[intPreferencesKey(entry.key)] = value
                    }
                    "long" -> {
                        val value = entry.longValue
                            ?: entry.doubleValue?.toLong()
                            ?: entry.intValue?.toLong()
                            ?: return@forEach
                        preferences[longPreferencesKey(entry.key)] = value
                    }
                    "boolean" -> {
                        val value = entry.booleanValue ?: return@forEach
                        preferences[booleanPreferencesKey(entry.key)] = value
                    }
                    "float" -> {
                        val value = entry.floatValue
                            ?: entry.doubleValue?.toFloat()
                            ?: return@forEach
                        preferences[androidx.datastore.preferences.core.floatPreferencesKey(entry.key)] = value
                    }
                    "double" -> {
                        val value = entry.doubleValue
                            ?: entry.floatValue?.toDouble()
                            ?: return@forEach
                        preferences[androidx.datastore.preferences.core.doublePreferencesKey(entry.key)] = value
                    }
                    "string_set" -> {
                        val value = entry.stringSetValue ?: return@forEach
                        preferences[stringSetPreferencesKey(entry.key)] = value
                    }
                }
            }
        }
    }

    val collagePatternFlow: Flow<CollagePattern> =
        dataStore.data.map { preferences ->
            CollagePattern.fromStorageKey(preferences[PreferencesKeys.COLLAGE_PATTERN])
        }

    suspend fun setCollagePattern(pattern: CollagePattern) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLLAGE_PATTERN] = pattern.storageKey
        }
    }

    val collageAutoRotateFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[PreferencesKeys.COLLAGE_AUTO_ROTATE] ?: false
        }

    suspend fun setCollageAutoRotate(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLLAGE_AUTO_ROTATE] = enabled
        }
    }

}
