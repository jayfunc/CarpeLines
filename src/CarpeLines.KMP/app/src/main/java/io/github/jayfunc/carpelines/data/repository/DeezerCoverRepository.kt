package io.github.jayfunc.carpelines.data.repository

import android.content.Context
import android.util.LruCache
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jayfunc.carpelines.data.image.RemoteArtworkCache
import io.github.jayfunc.carpelines.data.network.deezer.DeezerApiService
import io.github.jayfunc.carpelines.data.network.deezer.DeezerTrack
import io.github.jayfunc.carpelines.utils.NetworkRetryUtils
import io.github.jayfunc.carpelines.utils.isRetryableNetworkError
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Resolves an album cover for the playing track from Deezer.
 *
 * The cover-source chain asks this repository only when Deezer outranks the sources that can
 * already produce a picture, so a track whose session or library already has artwork never pays
 * for a network search.
 *
 * The cover is downloaded into the app cache and handed back as a `file` URI: the lyrics page
 * samples that bitmap for its palette and the share card decodes it, and neither can read a
 * remote https image.
 */
@Singleton
class DeezerCoverRepository @Inject constructor(
    private val deezerApiService: DeezerApiService,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context,
) {
    private val memoryCache = LruCache<String, String>(MAX_CACHED_COVERS)

    suspend fun findCoverArtwork(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        if (title.isBlank() || artist.isBlank()) return@withContext null
        val key = "${normalize(title)}|${normalize(artist)}"
        memoryCache.get(key)?.let { return@withContext it }

        val tracks = try {
            NetworkRetryUtils.withNetworkRetry(
                operationName = "deezer_cover:$key",
                maxAttempts = NETWORK_RETRY_ATTEMPTS,
                initialDelayMs = NETWORK_RETRY_INITIAL_DELAY_MS,
                shouldRetry = { it.isRetryableNetworkError() },
            ) {
                deezerApiService.searchTrack("$title $artist", limit = SEARCH_LIMIT).data
            }
        } catch (error: Throwable) {
            Timber.tag(TAG).d(error, "Deezer cover search failed for %s - %s", artist, title)
            return@withContext null
        }

        val track = chooseTrack(tracks, title, artist) ?: return@withContext null
        val coverUrl = track.album?.let {
            it.coverXl ?: it.coverBig ?: it.coverMedium ?: it.cover
        }?.takeIf { it.isNotBlank() } ?: return@withContext null

        cachedCover(coverUrl)?.also { memoryCache.put(key, it) }
    }

    /**
     * Prefers a result that matches both title and artist; a title-only match is still better
     * than the first unrelated result the search may have returned.
     */
    private fun chooseTrack(tracks: List<DeezerTrack>, title: String, artist: String): DeezerTrack? {
        val wantedTitle = normalize(title)
        val wantedArtist = normalize(artist)
        return tracks.firstOrNull {
            normalize(it.title) == wantedTitle && normalize(it.artist?.name) == wantedArtist
        } ?: tracks.firstOrNull { normalize(it.title) == wantedTitle }
                ?: tracks.firstOrNull()
    }

    /** A stable cache file per URL, downloaded once and reused across tracks and rotations. */
    private suspend fun cachedCover(url: String): String? {
        val directory = File(context.cacheDir, CACHE_DIRECTORY).apply { mkdirs() }
        val file = File(directory, url.sha1() + ".jpg")
        if (file.isFile && file.length() > 0L) return file.toUri().toString()

        return try {
            RemoteArtworkCache.download(
                client = okHttpClient,
                request = Request.Builder().url(url).get().build(),
                file = file,
            )
            file.toUri().toString()
        } catch (error: Throwable) {
            // The picture itself can still be rendered from the URL; only the palette and the
            // share card lose local access, which is better than dropping the source entirely.
            Timber.tag(TAG).w(error, "Could not cache Deezer cover")
            url
        }
    }

    private fun String.sha1(): String = MessageDigest.getInstance("SHA-1")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }

    /** Case- and punctuation-insensitive comparison, so "Song Title" and "song-title" match. */
    private fun normalize(value: String?): String =
        value.orEmpty().lowercase().replace(NON_ALNUM, "")

    private companion object {
        const val TAG = "DeezerCover"
        const val CACHE_DIRECTORY = "deezer_covers"
        const val MAX_CACHED_COVERS = 64
        const val SEARCH_LIMIT = 5
        const val NETWORK_RETRY_ATTEMPTS = 3
        const val NETWORK_RETRY_INITIAL_DELAY_MS = 500L
        val NON_ALNUM = Regex("[^\\p{L}\\p{N}]")
    }
}
