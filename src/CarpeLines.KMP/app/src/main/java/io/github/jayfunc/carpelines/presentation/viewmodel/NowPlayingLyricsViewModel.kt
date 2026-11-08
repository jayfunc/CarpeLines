package io.github.jayfunc.carpelines.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsMapper
import io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsRepository
import io.github.jayfunc.carpelines.data.carpelines.LocalLyricsResolver
import io.github.jayfunc.carpelines.data.carpelines.ManualLyricsChange
import io.github.jayfunc.carpelines.data.carpelines.ManualLyricsStore
import io.github.jayfunc.carpelines.data.external.ExternalMediaState
import io.github.jayfunc.carpelines.data.model.CoverArtSource
import io.github.jayfunc.carpelines.data.model.Lyrics
import io.github.jayfunc.carpelines.data.model.Song
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.data.repository.DeezerCoverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Resolves lyrics for the shared lyrics page with the user's source order, for both CarpeLines
 * tracks and external media sessions. Local sources are read from embedded tags and sibling .lrc
 * files; online sources go through the ordered BetterLyrics engine.
 */
@HiltViewModel
class NowPlayingLyricsViewModel @Inject constructor(
    private val repository: BetterLyricsRepository,
    private val manualLyricsStore: ManualLyricsStore,
    private val localLyricsResolver: LocalLyricsResolver,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val deezerCoverRepository: DeezerCoverRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    data class UiState(
        val lyrics: Lyrics? = null,
        val providerLabel: String? = null,
        val loading: Boolean = false,
        val notFound: Boolean = false,
        val error: String? = null,
        /** Album art from the matching library song, used when the player exposes none. */
        val localArtworkModel: Any? = null,
        /** Cover fetched from Deezer for this track, when that source is enabled. */
        val deezerArtworkModel: Any? = null,
        /** The user's enabled cover sources, highest priority first. */
        val coverArtSourceOrder: List<CoverArtSource> = CoverArtSource.DefaultOrder,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var coverJob: Job? = null
    private var lastKey: String? = null
    private var lastTrackKey: String? = null
    private var lastExternal: ExternalMediaState? = null
    private var lastInternal: Song? = null
    private var lastLocalSong: Song? = null
    private var lastLocalLoading: Boolean = false

    init {
        // The order is edited on another destination. Mirror it here and re-pick the cover for the
        // current track so a reorder takes effect without waiting for the next song.
        viewModelScope.launch {
            userPreferencesRepository.coverArtSourceOrderFlow.collect { order ->
                _state.update { it.copy(coverArtSourceOrder = order) }
                lastExternal?.let { updateCoverArtwork(it, lastLocalSong) }
            }
        }
        // The manual search screen lives on another destination, so the page learns about its
        // save/clear through the store rather than through a shared ViewModel instance.
        viewModelScope.launch {
            manualLyricsStore.changes.collect { change ->
                val media = lastExternal ?: return@collect
                val key = manualLyricsStore.keyFor(media.title, media.artist, media.album)
                if (change.key != key) return@collect

                loadJob?.cancel()
                lastKey = null
                lastTrackKey = null
                when (change) {
                    is ManualLyricsChange.Selected -> {
                        if (change.selection.document.isEmpty) {
                            resolveExternal(media)
                        } else {
                            _state.update {
                                it.copy(
                                    loading = false,
                                    notFound = false,
                                    error = null,
                                    lyrics = repository.toPlayerLyrics(
                                        change.selection.document,
                                        fromRemote = true,
                                    ),
                                    providerLabel = change.selection.providerLabel
                                        .takeIf { it.isNotBlank() }
                                        ?: change.selection.document.provider.displayName,
                                )
                            }
                        }
                    }
                    is ManualLyricsChange.Cleared -> {
                        _state.update {
                            it.copy(
                                loading = true,
                                notFound = false,
                                error = null,
                                lyrics = null,
                                providerLabel = null,
                            )
                        }
                        resolveExternal(media)
                    }
                }
            }
        }
    }

    /** Changes whenever the settings page reorders sources; used as a LaunchedEffect key. */
    val orderKey: Int
        get() = repository.sourceOrder.hashCode()

    fun resolveInternal(song: Song?, localLyrics: Lyrics?, localLoading: Boolean) {
        lastInternal = song
        lastExternal = null
        lastLocalLoading = localLoading
        if (song == null) {
            clear()
            return
        }
        val key = "i|${song.id}|${song.lyrics != null}|$orderKey"
        // "loading" alone is not proof that work is still running: a cancelled job can leave the
        // flag set, and returning early on it used to pin the page on its spinner forever.
        if (key == lastKey && (loadJob?.isActive == true || _state.value.lyrics?.hasContent() == true)) return
        lastKey = key
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val order = repository.sourceOrder
            val localRaw = localLyricsResolver.findLocalRaw(song, order)
            if (repository.localSourcesFirst && localRaw != null) {
                if (applyLocal(localRaw, song.duration)) return@launch
                // The local payload parsed to nothing usable (a stub .lrc, or one that only carries
                // [ti:]/[ar:] tags). Falling through keeps the page on "loading" instead of
                // claiming the track has local lyrics it cannot show.
            }

            _state.update {
                it.copy(loading = true, notFound = false, error = null, lyrics = null, providerLabel = null)
            }
            // Give embedded-tag loading a moment to arrive before hitting the network.
            if (repository.localSourcesFirst && localLoading && localRaw == null) {
                delay(LOCAL_WAIT_MS)
            }
            try {
                val document = if (repository.onlineProviderOrder.isEmpty()) {
                    null
                } else {
                    withTimeoutOrNull(ONLINE_SEARCH_TIMEOUT_MS) {
                        repository.load(BetterLyricsMapper.toQuery(song))
                    }
                }
                if (document != null && !document.isEmpty) {
                    _state.update {
                        it.copy(
                            loading = false,
                            notFound = false,
                            lyrics = repository.toPlayerLyrics(document, fromRemote = true),
                            providerLabel = document.provider.displayName,
                            error = null,
                        )
                    }
                    return@launch
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (localRaw == null) {
                    _state.update {
                        it.copy(
                            loading = false,
                            notFound = false,
                            error = error.message ?: error::class.simpleName,
                        )
                    }
                    return@launch
                }
            }

            if (localRaw != null && repository.sourceOrder.any { it.isLocal }) {
                if (!applyLocal(localRaw, song.duration)) {
                    _state.update {
                        it.copy(loading = false, notFound = true, lyrics = null, providerLabel = null)
                    }
                }
            } else if (localLyrics != null && localLyrics.hasContent()) {
                // CarpeLines may already have parsed local lyrics from a source we cannot read.
                _state.update {
                    it.copy(
                        loading = false,
                        notFound = false,
                        lyrics = localLyrics,
                        providerLabel = context.getString(R.string.lyrics_source_order_local),
                        error = null,
                    )
                }
            } else {
                _state.update {
                    it.copy(loading = false, notFound = true, lyrics = null, providerLabel = null)
                }
            }
        }
    }

    fun resolveExternal(media: ExternalMediaState?, ignoreStored: Boolean = false) {
        lastExternal = media
        lastInternal = null
        if (media == null || !media.hasMetadata) {
            clear()
            return
        }
        val trackKey = "e|${media.identity}|$orderKey"
        // Duration is resolution input, not track identity: it often arrives a beat after the
        // title/artist. A later duration retries a failed lookup, but must not restart one that
        // already has lyrics on screen or is still running.
        val settled = trackKey == lastTrackKey &&
            (loadJob?.isActive == true || _state.value.lyrics?.hasContent() == true)
        if (!ignoreStored && settled) return
        lastTrackKey = trackKey
        lastKey = "$trackKey|${media.durationMs}"
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val order = repository.sourceOrder
            val localSong = localLyricsResolver.findMatchingSong(media)
            lastLocalSong = localSong
            val localDuration = media.durationMs ?: localSong?.duration
            updateCoverArtwork(media, localSong)

            // A version the user picked by hand is the strongest signal there is: it wins over
            // local files and automatic provider matching until they restore automatic matching.
            val manualSelection = manualLyricsStore.load(media.title, media.artist, media.album)
                ?.takeIf { !it.document.isEmpty }
            if (manualSelection != null) {
                _state.update {
                    it.copy(
                        loading = false,
                        notFound = false,
                        error = null,
                        lyrics = repository.toPlayerLyrics(manualSelection.document, fromRemote = true),
                        providerLabel = manualSelection.providerLabel
                            .takeIf { it.isNotBlank() }
                            ?: manualSelection.document.provider.displayName,
                        localArtworkModel = localSong?.albumArtUriString ?: it.localArtworkModel,
                    )
                }
                return@launch
            }

            // Lyrics the library already holds — imported by the user, picked from a search, or
            // scanned from a .lrc file — are the current choice for this track, so they win over
            // every source. A retry is the one path that deliberately looks again.
            val storedLyrics = localSong?.lyrics
                ?.takeIf { it.isNotBlank() && !ignoreStored }
            val localRaw = localSong?.let { localLyricsResolver.findLocalRaw(it, order) }

            // Paint something the moment it exists. With online sources enabled the page used to
            // hold a spinner for the whole network search even though the file sitting next to the
            // audio already had the words; the search then only upgrades what is on screen.
            val localApplied = when {
                storedLyrics != null -> applyLocal(storedLyrics, localDuration)
                localRaw != null -> applyLocal(localRaw, localDuration)
                else -> false
            }

            // A lyric the library holds is the user's own choice, not a cache: never search.
            if (storedLyrics != null && localApplied) return@launch

            if (repository.onlineProviderOrder.isEmpty() ||
                (repository.localSourcesFirst && localApplied)
            ) {
                if (!localApplied) markNotFound()
                return@launch
            }

            // The page is popped and rebuilt every time the user comes back to it, taking this
            // ViewModel's state with it. The providers' answer for this track outlives that: paint
            // it straight away instead of showing a spinner and searching all of them again.
            val query = media.toLyricsQuery()
            if (!ignoreStored) {
                val cached = repository.peek(query)
                if (cached != null) {
                    val cachedDocument = cached.document
                    if (cachedDocument != null && !cachedDocument.isEmpty) {
                        _state.update {
                            it.copy(
                                loading = false,
                                notFound = false,
                                error = null,
                                lyrics = repository.toPlayerLyrics(cachedDocument, fromRemote = true),
                                providerLabel = cachedDocument.provider.displayName,
                            )
                        }
                    } else if (!localApplied) {
                        markNotFound()
                    }
                    return@launch
                }
            }

            if (!localApplied) {
                _state.update {
                    it.copy(
                        loading = true,
                        notFound = false,
                        error = null,
                        lyrics = null,
                        providerLabel = null,
                        localArtworkModel = localSong?.albumArtUriString
                    )
                }
                delay(350)
            }

            val document = try {
                withTimeoutOrNull(ONLINE_SEARCH_TIMEOUT_MS) {
                    repository.load(query, useCache = !ignoreStored)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (!localApplied) {
                    _state.update {
                        it.copy(
                            loading = false,
                            notFound = false,
                            error = error.message ?: error::class.simpleName,
                        )
                    }
                    return@launch
                }
                null
            }

            if (document != null && !document.isEmpty) {
                _state.update {
                    it.copy(
                        loading = false,
                        notFound = false,
                        lyrics = repository.toPlayerLyrics(document, fromRemote = true),
                        providerLabel = document.provider.displayName,
                        error = null,
                    )
                }
            } else if (!localApplied) {
                markNotFound()
            }
        }
    }

    private fun markNotFound() {
        _state.update {
            it.copy(loading = false, notFound = true, lyrics = null, providerLabel = null)
        }
    }

    /**
     * Re-runs the lookup. [media] is the live media snapshot: the one captured when the failed
     * lookup ran may still carry the previous track's duration, which is enough to miss both the
     * library match and the scoring threshold.
     */
    fun retry(media: ExternalMediaState? = null) {
        lastKey = null
        lastTrackKey = null
        val current = media ?: lastExternal
        if (current != null) {
            // An explicit retry is the one place that must not be answered from the cache.
            repository.invalidate(current.toLyricsQuery())
            resolveExternal(current, ignoreStored = true)
        } else {
            resolveInternal(lastInternal, _state.value.lyrics, lastLocalLoading)
        }
    }

    /**
     * Picks the cover from the user's source order and, only when Deezer outranks every source
     * that can already answer, asks the network for it. The page renders the best available
     * source immediately; a late Deezer cover is faded in by the artwork handover.
     */
    private fun updateCoverArtwork(media: ExternalMediaState, localSong: Song?) {
        _state.update {
            it.copy(
                localArtworkModel = localSong?.albumArtUriString,
                deezerArtworkModel = null,
            )
        }
        coverJob?.cancel()

        val order = _state.value.coverArtSourceOrder
        val deezerIndex = order.indexOf(CoverArtSource.DEEZER)
        if (deezerIndex < 0) return

        val higherPriorityAvailable = order.take(deezerIndex).any { source ->
            when (source) {
                CoverArtSource.MEDIA_SESSION -> media.artworkBitmap != null || media.artworkUri != null
                CoverArtSource.LOCAL_EMBEDDED -> localSong?.albumArtUriString != null
                CoverArtSource.DEEZER -> false
            }
        }
        if (higherPriorityAvailable) return

        coverJob = viewModelScope.launch {
            val model = deezerCoverRepository.findCoverArtwork(media.title, media.artist) ?: return@launch
            _state.update { it.copy(deezerArtworkModel = model) }
        }
    }

    /**
     * Publishes a local payload, or reports `false` when it parses to nothing usable.
     *
     * A `.lrc` that only carries `[ti:]`/`[ar:]` tags, a byte-order-marked file the parser
     * cannot split, or an embedded tag holding a placeholder all produce an empty document.
     * Reporting that as success was the bug behind a lyric page stuck on "loading" while the
     * header already said the source was Local.
     */
    private suspend fun applyLocal(raw: String, durationMs: Long?): Boolean {
        val document = repository.parseRawLyrics(raw, durationMs = durationMs)
        val lyrics = repository.toPlayerLyrics(document, fromRemote = false)
        if (!lyrics.hasContent()) return false
        _state.update {
            it.copy(
                loading = false,
                notFound = false,
                lyrics = lyrics,
                providerLabel = context.getString(R.string.lyrics_source_order_local),
                error = null,
            )
        }
        return true
    }

    private fun clear() {
        loadJob?.cancel()
        coverJob?.cancel()
        lastKey = null
        lastTrackKey = null
        lastLocalSong = null
        _state.value = UiState(
            localArtworkModel = _state.value.localArtworkModel,
            coverArtSourceOrder = _state.value.coverArtSourceOrder,
        )
    }

    private companion object {
        const val LOCAL_WAIT_MS = 1_500L

        /**
         * A provider that never answers must not own the page. Once this elapses the search is
         * abandoned and the page settles on "not found" with a retry, instead of spinning.
         * It sits above the engine's per-provider ceiling so one slow source cannot cancel the
         * answers the others already returned.
         */
        const val ONLINE_SEARCH_TIMEOUT_MS = 15_000L
    }
}

/** Whether there is anything a lyrics page can actually draw. */
internal fun Lyrics.hasContent(): Boolean =
    !synced.isNullOrEmpty() || !plain.isNullOrEmpty()
