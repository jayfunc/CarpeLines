package io.github.jayfunc.carpelines.presentation.components.lyrics

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.jayfunc.carpelines.data.preferences.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Per-track lyric offset for playback CarpeLines does not own.
 *
 * The player sheet already stores offsets per library song id, which an external media session
 * has no way to reach. This store keys offsets by a hash of "title|artist" so the immersive
 * lyrics page can calibrate whatever the system is playing, and remembers it across sessions.
 */
object LyricsOffsetStore {

    /** Snap step of the calibration slider: a tenth of a second is what feels adjustable by ear. */
    const val STEP_MS = 100

    /** Offsets beyond this are a wrong recording, not a calibration; keep the range tight. */
    const val MAX_OFFSET_MS = 15_000

    private const val MAX_ENTRIES = 300
    private val KEY = stringPreferencesKey("lyrics_track_offsets")

    fun trackKey(title: String, artist: String): String {
        val raw = "${title.trim().lowercase()}|${artist.trim().lowercase()}"
        return raw.hashCode().toUInt().toString(16)
    }

    fun offset(context: Context, trackKey: String): Flow<Int> =
        context.dataStore.data.map { preferences ->
            parse(preferences[KEY]).firstOrNull { it.first == trackKey }?.second ?: 0
        }

    /**
     * Writes an absolute offset. A slider knows the target value, so writing it directly keeps
     * repeated commits from compounding a delta computed against a stale value; zero removes the
     * entry instead of storing a no-op.
     */
    suspend fun set(context: Context, trackKey: String, offsetMs: Int): Int {
        val next = offsetMs.coerceIn(-MAX_OFFSET_MS, MAX_OFFSET_MS)
        context.dataStore.edit { preferences ->
            val entries = parse(preferences[KEY]).filterNot { it.first == trackKey }
            preferences[KEY] = if (next == 0) {
                serialize(entries)
            } else {
                serialize((entries + (trackKey to next)).takeLast(MAX_ENTRIES))
            }
        }
        return next
    }

    suspend fun clear(context: Context, trackKey: String): Int = set(context, trackKey, 0)

    private fun parse(raw: String?): List<Pair<String, Int>> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(';').mapNotNull { entry ->
            val separator = entry.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val key = entry.substring(0, separator)
            val value = entry.substring(separator + 1).toIntOrNull() ?: return@mapNotNull null
            key to value
        }
    }

    private fun serialize(entries: List<Pair<String, Int>>): String =
        entries.joinToString(";") { (key, value) -> "$key=$value" }
}
