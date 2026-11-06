package io.github.jayfunc.carpelines.data.carpelines

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jayfunc.carpelines.data.betterlyrics.LyricsSourceId
import io.github.jayfunc.carpelines.data.database.MusicDao
import io.github.jayfunc.carpelines.data.database.toSong
import io.github.jayfunc.carpelines.data.external.ExternalMediaState
import io.github.jayfunc.carpelines.data.media.AudioMetadataReader
import io.github.jayfunc.carpelines.data.model.Song
import io.github.jayfunc.carpelines.lyrics.match.MetadataMatcher
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A local lyrics payload found for the track an external player is currently playing. */
data class LocalLyricsCandidate(
    val source: LyricsSourceId,
    val raw: String,
)

/**
 * Resolves the library copy of the external track and reads its local lyrics sources.
 *
 * Embedded tags are read from the audio file itself: a MediaStore scan never exposes tag lyrics,
 * and the library's `lyrics` column only holds text this app saved earlier. Sibling `.lrc` files
 * are looked up next to the audio file.
 */
@Singleton
class LocalLyricsResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicDao: MusicDao,
) {
    suspend fun findMatchingSong(media: ExternalMediaState): Song? = withContext(Dispatchers.IO) {
        media.mediaId
            ?.toLongOrNull()
            ?.let { id -> musicDao.getSongByIdOnce(id)?.toSong() }
            ?.takeIf { scoreMatch(it, media) }
            ?: findByMetadata(media)
    }

    /** Returns the enabled local sources that actually carry lyrics for this track. */
    suspend fun candidates(
        media: ExternalMediaState,
        sources: List<LyricsSourceId>,
    ): List<LocalLyricsCandidate> = withContext(Dispatchers.IO) {
        val song = findMatchingSong(media) ?: return@withContext emptyList()
        sources
            .filter { it.isLocal }
            .mapNotNull { source ->
                val raw = when (source) {
                    LyricsSourceId.EMBEDDED -> readEmbeddedLyrics(song)
                    LyricsSourceId.LOCAL_LRC -> readSiblingLrc(song)
                    else -> null
                }?.takeIf { it.isNotBlank() }
                raw?.let { LocalLyricsCandidate(source, it) }
            }
    }

    /** First local payload in the user's source order, as a single fallback. */
    suspend fun findLocalRaw(song: Song, order: List<LyricsSourceId>): String? {
        for (source in order.filter { it.isLocal }) {
            val raw = when (source) {
                LyricsSourceId.EMBEDDED -> readEmbeddedLyrics(song)
                LyricsSourceId.LOCAL_LRC -> readSiblingLrc(song)
                else -> null
            }
            if (!raw.isNullOrBlank()) return raw
        }
        return null
    }

    private suspend fun readEmbeddedLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        val fromFile = song.path
            .takeIf { it.isNotBlank() }
            ?.let { path -> File(path).takeIf { it.isFile } }
            ?.let { file ->
                runCatching {
                    AudioMetadataReader.read(file, readArtwork = false)?.lyrics
                }.getOrNull()
            }
        if (!fromFile.isNullOrBlank()) return@withContext fromFile

        val uri = song.contentUriString.takeIf { it.isNotBlank() }?.toUri()
            ?: return@withContext null
        runCatching { AudioMetadataReader.read(context, uri)?.lyrics }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private suspend fun readSiblingLrc(song: Song): String? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(song.path)
            val directory = file.parentFile ?: return@runCatching null
            val stem = file.nameWithoutExtension
            val candidates = listOf(
                File(directory, "$stem.lrc"),
                File(directory, "$stem.LRC"),
                File(directory, "${song.title}.lrc"),
            )
            candidates.firstOrNull { it.isFile }?.readText()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private suspend fun findByMetadata(media: ExternalMediaState): Song? {
        if (media.title.isBlank()) return null
        val candidates = musicDao.findLocalSongsByTitleOnce(media.title, MATCH_CANDIDATE_LIMIT)
        return candidates
            .map { it.toSong() }
            .filter { scoreMatch(it, media) }
            .minByOrNull { candidate ->
                abs((candidate.duration - (media.durationMs ?: candidate.duration)))
            }
    }

    /** Whether [song] is credibly the track [media] describes. */
    private fun scoreMatch(song: Song, media: ExternalMediaState): Boolean {
        val titleScore = MetadataMatcher.stringSimilarity(song.title, media.title)
        if (titleScore < TITLE_MATCH_THRESHOLD) return false

        val artistScore = MetadataMatcher.stringSimilarity(song.displayArtist, media.artist)
        if (media.artist.isNotBlank() && artistScore < ARTIST_MATCH_THRESHOLD) return false

        // Nothing to compare when either side hides its length; the text match has to do.
        val mediaDuration = media.durationMs ?: return true
        if (song.duration <= 0L || mediaDuration <= 0L) return true
        return abs(song.duration - mediaDuration) <= DURATION_TOLERANCE_MS
    }

    private companion object {
        /** Same thresholds the library used for a confident match: strong title, loose artist. */
        const val TITLE_MATCH_THRESHOLD = 0.8
        const val ARTIST_MATCH_THRESHOLD = 0.5

        /** Tag length and session length disagree by a second or two; far less than a different track. */
        const val DURATION_TOLERANCE_MS = 8_000L

        const val MATCH_CANDIDATE_LIMIT = 12
    }
}
