package io.github.jayfunc.carpelines.data.external

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jayfunc.carpelines.data.model.Song
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * What other players have actually played on this device.
 *
 * Two things need this. The listening stats screen aggregates by song id out of the library, and
 * CarpeLines has no library playback of its own, so everything the user hears comes from a foreign
 * media session — without a local snapshot of those tracks the stats page has nothing to name. And
 * the home screen wants the *players* that were really used, not a hardcoded list of "supported"
 * apps, which is what the old section showed.
 *
 * The data is a small JSON file; it is a snapshot of what happened, not a source of truth for the
 * library, so it never touches the database.
 */
@Singleton
class ExternalPlaybackStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class TrackRecord(
        val songId: String,
        val title: String,
        val artist: String,
        val album: String,
        val artworkUri: String?,
        val durationMs: Long,
        val packageName: String,
        val appLabel: String,
        val lastPlayedAt: Long,
        val playCount: Int
    )

    data class PlayerRecord(
        val packageName: String,
        val appLabel: String,
        val lastPlayedAt: Long,
        val playCount: Int,
        val trackIds: List<String>
    )

    data class Snapshot(
        val tracks: List<TrackRecord> = emptyList(),
        val players: List<PlayerRecord> = emptyList()
    ) {
        val isEmpty: Boolean get() = tracks.isEmpty() && players.isEmpty()
    }

    private val gson = Gson()
    private val file = File(context.filesDir, "external_playback.json")
    private val atomicFile = AtomicFile(file)
    private val lock = Any()

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()

    @Volatile
    private var loaded = false

    private val initScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.IO
    )

    init {
        // The home screen reads the snapshot as soon as it composes; load it off the main thread
        // instead of waiting for the first play.
        initScope.launch { runCatching { ensureLoaded() } }
    }

    /** Songs for the stats aggregation, oldest play first, ready to merge with the library. */
    suspend fun songs(): List<Song> = withContext(Dispatchers.IO) {
        ensureLoaded()
        _snapshot.value.tracks
            .sortedBy { it.lastPlayedAt }
            .map { record ->
                Song(
                    id = record.songId,
                    title = record.title,
                    artist = record.artist,
                    artistId = -1,
                    album = record.album,
                    albumId = -1,
                    path = "",
                    contentUriString = "",
                    albumArtUriString = record.artworkUri,
                    duration = record.durationMs,
                    mimeType = null,
                    bitrate = null,
                    sampleRate = null,
                    dateAdded = record.lastPlayedAt / 1000L
                )
            }
    }

    /**
     * Records that [media] started playing. Called once per track change, not per progress tick:
     * [playCount] is a play count, and re-writing the file on every tick would be pointless I/O.
     */
    suspend fun recordStart(media: ExternalMediaState, artwork: Bitmap? = null) =
        withContext(Dispatchers.IO) {
            if (media.title.isBlank()) return@withContext
            ensureLoaded()
            val songId = songIdFor(media)
            val artworkUri = media.artworkUri ?: preserveArtwork(songId, artwork)
            val now = System.currentTimeMillis()

            updateSnapshot { current ->
                val existingTrack = current.tracks.firstOrNull { it.songId == songId }
                val track = TrackRecord(
                    songId = songId,
                    title = media.title,
                    artist = media.artist.ifBlank { existingTrack?.artist.orEmpty() },
                    album = media.album.ifBlank { existingTrack?.album.orEmpty() },
                    artworkUri = artworkUri ?: existingTrack?.artworkUri,
                    durationMs = media.durationMs ?: existingTrack?.durationMs ?: 0L,
                    packageName = media.packageName,
                    appLabel = media.appLabel,
                    lastPlayedAt = now,
                    playCount = (existingTrack?.playCount ?: 0) + 1
                )
                val tracks = (current.tracks.filterNot { it.songId == songId } + track)
                    .filter { it.lastPlayedAt >= now - MAX_AGE_MS }
                    .sortedByDescending { it.lastPlayedAt }
                    .take(MAX_TRACKS)

                val existingPlayer = current.players.firstOrNull {
                    it.packageName == media.packageName
                }
                val player = PlayerRecord(
                    packageName = media.packageName,
                    appLabel = media.appLabel,
                    lastPlayedAt = now,
                    playCount = (existingPlayer?.playCount ?: 0) + 1,
                    trackIds = ((existingPlayer?.trackIds.orEmpty()) + songId)
                        .distinct()
                        .takeLast(MAX_TRACK_IDS_PER_PLAYER)
                )
                val players = (current.players.filterNot {
                    it.packageName == media.packageName
                } + player)
                    .sortedByDescending { it.lastPlayedAt }
                    .take(MAX_PLAYERS)

                Snapshot(tracks = tracks, players = players)
            }
        }

    /**
     * Identity for a track played by another app. Two players reporting the same title and artist
     * are the same song, so the id deliberately ignores the package: the same track heard in
     * Spotify and in the system player should be one row in the statistics, not two.
     */
    fun songIdFor(media: ExternalMediaState): String {
        val title = normalize(media.title)
        val artist = normalize(media.artist)
        if (title.isBlank()) return "external:${media.packageName}:${media.identity.hashCode()}"
        return "external:${title}|$artist"
    }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun ensureLoaded() {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            _snapshot.value = readSnapshot()
            loaded = true
        }
    }

    private fun updateSnapshot(transform: (Snapshot) -> Snapshot) {
        val updated = synchronized(lock) { transform(_snapshot.value).also { _snapshot.value = it } }
        writeSnapshot(updated)
    }

    private fun readSnapshot(): Snapshot {
        val raw = runCatching {
            atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return Snapshot()
        return runCatching {
            val type = object : TypeToken<Snapshot>() {}.type
            val parsed: Snapshot? = gson.fromJson(raw, type)
            // Minified builds used to write this file with obfuscated field names, which no longer
            // match. Gson then leaves the lists null even though their Kotlin types are non-null,
            // so normalise here rather than letting the nulls reach the view models.
            Snapshot(
                tracks = parsed?.tracks.orEmpty(),
                players = parsed?.players.orEmpty(),
            )
        }.onFailure { Timber.w(it, "Unable to read external playback snapshot") }
            .getOrDefault(Snapshot())
    }

    private fun writeSnapshot(snapshot: Snapshot) {
        runCatching {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) parent.mkdirs()
            var stream: FileOutputStream? = null
            try {
                stream = atomicFile.startWrite()
                stream.write(gson.toJson(snapshot).toByteArray(Charsets.UTF_8))
                stream.fd.sync()
                atomicFile.finishWrite(stream)
                stream = null
            } catch (error: Throwable) {
                stream?.let { atomicFile.failWrite(it) }
                throw error
            }
        }.onFailure { Timber.w(it, "Unable to persist external playback snapshot") }
    }

    /**
     * Keeps the cover of a track only the player could see. The bitmap lives in memory in the
     * media session, so it has to be copied out before the session goes away or the statistics
     * page would show a grey square forever.
     */
    private fun preserveArtwork(songId: String, artwork: Bitmap?): String? {
        if (artwork == null) return null
        return runCatching {
            val directory = File(context.filesDir, "external_playback_art")
            if (!directory.exists()) directory.mkdirs()
            val target = File(directory, "${songId.hashCode().toUInt()}.jpg")
            if (!target.exists()) {
                val scaled = scaleDown(artwork, MAX_ARTWORK_PX)
                target.outputStream().use { output ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }
                if (scaled !== artwork) scaled.recycle()
            }
            Uri.fromFile(target).toString()
        }.onFailure { Timber.w(it, "Unable to preserve external artwork") }.getOrNull()
    }

    private fun scaleDown(bitmap: Bitmap, maxPx: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxPx) return bitmap
        val ratio = maxPx.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    /** Newest first. Players the user uninstalled stay listed until the snapshot is pruned. */
    suspend fun playersByRecency(): List<PlayerRecord> = withContext(Dispatchers.IO) {
        ensureLoaded()
        _snapshot.value.players.sortedByDescending { it.lastPlayedAt }
    }

    /**
     * Maps every remembered track to the player that last reported it.
     *
     * Playback history written before events carried their own package has to be attributed
     * somehow, and this snapshot is the only per-track source the app kept. It is an
     * approximation: a song played by two players maps to the most recent one.
     */
    suspend fun sourcePackagesBySongId(): Map<String, String> = withContext(Dispatchers.IO) {
        ensureLoaded()
        _snapshot.value.tracks.associate { it.songId to it.packageName }
    }

    companion object {
        private const val MAX_TRACKS = 400
        private const val MAX_PLAYERS = 24
        private const val MAX_TRACK_IDS_PER_PLAYER = 200
        private const val MAX_ARTWORK_PX = 512

        /** Everything older than this is only interesting to nobody; keep the file small. */
        private val MAX_AGE_MS = TimeUnit.DAYS.toMillis(400)
    }
}
