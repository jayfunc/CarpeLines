package io.github.jayfunc.carpelines.data.carpelines

import io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsRepository
import io.github.jayfunc.carpelines.data.external.ExternalMediaState
import io.github.jayfunc.carpelines.data.model.Lyrics
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Resolves the lyrics for the external media session shared by every compact surface: the home
 * screen widgets and the floating desktop/lock-screen lyrics.
 *
 * The rules mirror the lyrics page: a manual selection wins, library-held lyrics come next, the
 * enabled local sources follow the same order, and online providers are only asked when local
 * sources did not already answer or when they outrank local ones. Results are cached per track,
 * source order and parse options, so the 250 ms position tick never starts a provider search.
 */
@Singleton
class ExternalLyricsResolver @Inject constructor(
    private val betterLyricsRepository: BetterLyricsRepository,
    private val localLyricsResolver: LocalLyricsResolver,
    private val manualLyricsStore: ManualLyricsStore,
) {

    private data class CacheEntry(val lyrics: Lyrics?)

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // A version picked by hand in the search screen must replace whatever was cached.
        scope.launch {
            manualLyricsStore.changes.collect { cache.clear() }
        }
    }

    /** The cached answer for [media], if one has already been resolved. */
    fun peek(media: ExternalMediaState): Lyrics? = cache[cacheKey(media)]?.lyrics

    /** Whether [media] already has an answer, including the "nothing found" answer. */
    fun isResolved(media: ExternalMediaState): Boolean = cache.containsKey(cacheKey(media))

    suspend fun resolve(media: ExternalMediaState): Lyrics? {
        val key = cacheKey(media)
        cache[key]?.let { return it.lyrics }

        val lyrics = runCatching {
            withContext(Dispatchers.IO) { resolveInternal(media) }
        }.getOrNull()
        cache[key] = CacheEntry(lyrics)
        if (cache.size > MAX_ENTRIES) cache.clear()
        return lyrics
    }

    private suspend fun resolveInternal(media: ExternalMediaState): Lyrics? {
        val manual = manualLyricsStore.load(media.title, media.artist, media.album)
            ?.takeIf { !it.document.isEmpty }
        if (manual != null) {
            return betterLyricsRepository.toPlayerLyrics(manual.document, fromRemote = true)
        }

        val order = betterLyricsRepository.sourceOrder
        val localSong = localLyricsResolver.findMatchingSong(media)
        val storedLyrics = localSong?.lyrics?.takeIf { it.isNotBlank() }
        val localRaw = localSong?.let { localLyricsResolver.findLocalRaw(it, order) }

        val localLyrics = when {
            storedLyrics != null -> parseLocal(storedLyrics, media.durationMs)
            localRaw != null -> parseLocal(localRaw, media.durationMs)
            else -> null
        }

        // A local source sitting above every online source is the page's rule as well: do not
        // spend a provider search when the words on disk already answer the request.
        if (localLyrics != null && betterLyricsRepository.localSourcesFirst) return localLyrics
        if (betterLyricsRepository.onlineProviderOrder.isEmpty()) return localLyrics

        val document = runCatching {
            withTimeoutOrNull(ONLINE_SEARCH_TIMEOUT_MS) {
                betterLyricsRepository.load(media.toLyricsQuery())
            }
        }.getOrNull()
        if (document != null && !document.isEmpty) {
            return betterLyricsRepository.toPlayerLyrics(document, fromRemote = true)
        }
        return localLyrics
    }

    private suspend fun parseLocal(raw: String, durationMs: Long?): Lyrics? {
        val document = betterLyricsRepository.parseRawLyrics(raw, durationMs = durationMs)
        return betterLyricsRepository.toPlayerLyrics(document, fromRemote = false)
            .takeIf { !it.synced.isNullOrEmpty() || !it.plain.isNullOrEmpty() }
    }

    private fun cacheKey(media: ExternalMediaState): String = buildString {
        append(media.identity).append('\u0001')
        // Duration wobbles by a beat between the session and the scanned file; bucket it so one
        // track cannot occupy several cache entries.
        append((media.durationMs ?: 0L) / DURATION_BUCKET_MS).append('\u0001')
        append(betterLyricsRepository.sourceOrder.hashCode()).append('\u0001')
        append(betterLyricsRepository.parseOptionsKey)
    }

    private companion object {
        const val MAX_ENTRIES = 32
        const val ONLINE_SEARCH_TIMEOUT_MS = 10_000L
        const val DURATION_BUCKET_MS = 5_000L
    }
}
