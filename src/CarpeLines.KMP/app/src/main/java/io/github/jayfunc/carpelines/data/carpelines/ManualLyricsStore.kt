package io.github.jayfunc.carpelines.data.carpelines

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jayfunc.carpelines.lyrics.model.LyricsDocument
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The lyrics the user picked by hand for one external-media track.
 *
 * BetterLyrics keeps a title/artist/album mapping plus the chosen provider and re-runs the search;
 * CarpeLines keeps the parsed document itself, so the exact selected version survives provider
 * changes and does not need another network round trip.
 */
@Serializable
data class ManualLyricsSelection(
    val document: LyricsDocument,
    val mappedTitle: String = "",
    val mappedArtist: String = "",
    val mappedAlbum: String = "",
    val savedAtEpochMs: Long = 0L,
    /** Localized label of the source the user picked (embedded, local .lrc, provider name). */
    val providerLabel: String = "",
)

sealed interface ManualLyricsChange {
    val key: String

    data class Selected(
        override val key: String,
        val selection: ManualLyricsSelection,
    ) : ManualLyricsChange

    data class Cleared(override val key: String) : ManualLyricsChange
}

/** One raw manual-lyrics file: [key] is the on-disk track key, [json] the stored document. */
data class RawManualLyrics(val key: String, val json: String)

/**
 * Stores manual selections as one JSON file per track under the app's private files directory.
 *
 * Kept outside DataStore because a document can carry syllable timings for every line and would
 * make the preferences blob expensive to rewrite on each save.
 */
@Singleton
class ManualLyricsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val changesFlow = MutableSharedFlow<ManualLyricsChange>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val changes: SharedFlow<ManualLyricsChange> = changesFlow.asSharedFlow()

    fun keyFor(title: String, artist: String, album: String): String {
        val normalized = listOf(title, artist, album)
            .joinToString("|") { it.trim().lowercase() }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.take(KEY_BYTES).joinToString("") { byte ->
            "%02x".format(Locale.US, byte.toInt() and 0xff)
        }
    }

    suspend fun load(title: String, artist: String, album: String): ManualLyricsSelection? =
        withContext(Dispatchers.IO) {
            val file = fileFor(keyFor(title, artist, album))
            if (!file.isFile) return@withContext null
            runCatching {
                json.decodeFromString(ManualLyricsSelection.serializer(), file.readText())
            }.getOrElse {
                // A damaged file should not pin the track to lyrics we cannot show.
                file.delete()
                null
            }
        }

    suspend fun save(
        title: String,
        artist: String,
        album: String,
        selection: ManualLyricsSelection,
    ): ManualLyricsSelection = withContext(Dispatchers.IO) {
        val key = keyFor(title, artist, album)
        val stored = selection.copy(savedAtEpochMs = System.currentTimeMillis())
        val directory = directory()
        directory.mkdirs()
        fileFor(key).writeText(json.encodeToString(ManualLyricsSelection.serializer(), stored))
        prune()
        changesFlow.emit(ManualLyricsChange.Selected(key, stored))
        stored
    }

    suspend fun clear(title: String, artist: String, album: String) = withContext(Dispatchers.IO) {
        val key = keyFor(title, artist, album)
        fileFor(key).delete()
        changesFlow.emit(ManualLyricsChange.Cleared(key))
    }

    /** Every stored selection, for backup. Damaged or foreign files are skipped. */
    suspend fun exportRaw(): List<RawManualLyrics> = withContext(Dispatchers.IO) {
        directory().listFiles { file -> file.isFile && file.extension == "json" }
            ?.mapNotNull { file ->
                val key = file.nameWithoutExtension
                if (!isValidKey(key)) return@mapNotNull null
                runCatching { RawManualLyrics(key, file.readText()) }.getOrNull()
            }
            ?: emptyList()
    }

    /** Replaces every stored selection, used by restore. Invalid keys are skipped. */
    suspend fun replaceRaw(entries: List<RawManualLyrics>) = withContext(Dispatchers.IO) {
        val directory = directory()
        directory.mkdirs()
        directory.listFiles { file -> file.isFile && file.extension == "json" }
            ?.forEach { it.delete() }
        entries.forEach { entry ->
            if (!isValidKey(entry.key)) return@forEach
            runCatching { fileFor(entry.key).writeText(entry.json) }
        }
        prune()
    }

    private fun directory(): File = File(context.filesDir, DIRECTORY_NAME)

    private fun fileFor(key: String): File = File(directory(), "$key.json")

    private fun prune() {
        val files = directory().listFiles { file -> file.isFile } ?: return
        if (files.size <= MAX_ENTRIES) return
        files.sortedBy { it.lastModified() }
            .take(files.size - MAX_ENTRIES)
            .forEach { it.delete() }
    }

    companion object {
        private const val DIRECTORY_NAME = "manual_lyrics"

        /** The old per-track offset store caps at 300 entries; keep the same order of magnitude. */
        private const val MAX_ENTRIES = 300

        /** 64 bits of SHA-256 is plenty for a local filename and keeps names short. */
        private const val KEY_BYTES = 8

        private val KEY_PATTERN = Regex("^[0-9a-f]{${KEY_BYTES * 2}}$")

        fun isValidKey(key: String): Boolean = KEY_PATTERN.matches(key)
    }
}
