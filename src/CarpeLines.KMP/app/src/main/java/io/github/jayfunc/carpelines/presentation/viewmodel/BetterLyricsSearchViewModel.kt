package io.github.jayfunc.carpelines.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsCandidate
import io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsRepository
import io.github.jayfunc.carpelines.data.betterlyrics.LyricsSourceId
import io.github.jayfunc.carpelines.data.carpelines.LocalLyricsCandidate
import io.github.jayfunc.carpelines.data.carpelines.LocalLyricsResolver
import io.github.jayfunc.carpelines.data.carpelines.ManualLyricsSelection
import io.github.jayfunc.carpelines.data.carpelines.ManualLyricsStore
import io.github.jayfunc.carpelines.data.external.ExternalMediaState
import io.github.jayfunc.carpelines.lyrics.model.LyricsDocument
import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery
import io.github.jayfunc.carpelines.lyrics.model.LyricsSearchResult
import io.github.jayfunc.carpelines.lyrics.parse.LyricsFormatDetector
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manual lyrics search, modelled on BetterLyrics' Lyrics Search window: the user can override the
 * metadata used for the search, browse every provider's answer in one list, preview one result, and
 * keep it for this track.
 */
@HiltViewModel
class BetterLyricsSearchViewModel @Inject constructor(
    private val repository: BetterLyricsRepository,
    private val manualLyricsStore: ManualLyricsStore,
    private val localLyricsResolver: LocalLyricsResolver,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    enum class PreviewTrack {
        ORIGINAL,
        TRANSLATION,
        ROMANIZATION,
    }

    data class UiState(
        val mediaIdentity: String? = null,
        val queryTitle: String = "",
        val queryArtist: String = "",
        val queryAlbum: String = "",
        val searching: Boolean = false,
        val hasSearched: Boolean = false,
        val results: List<BetterLyricsCandidate> = emptyList(),
        val error: String? = null,
        val previewCandidate: BetterLyricsCandidate? = null,
        val previewDocument: LyricsDocument? = null,
        val previewTrack: PreviewTrack = PreviewTrack.ORIGINAL,
        val previewLoading: Boolean = false,
        val previewKey: String? = null,
        val applying: Boolean = false,
        val appliedCount: Int = 0,
        val manualSelection: ManualLyricsSelection? = null,
    ) {
        val canSearch: Boolean
            get() = queryTitle.isNotBlank() && !searching

        val previewHasTranslation: Boolean
            get() = previewDocument?.hasTranslation == true

        val previewHasRomanization: Boolean
            get() = previewDocument?.hasRomanization == true
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var syncJob: Job? = null
    private var searchJob: Job? = null
    private var previewJob: Job? = null

    /**
     * Seeds the editable query from the chosen lyrics for this track, or from the track metadata
     * when this is the first manual search.
     */
    fun syncWithMedia(media: ExternalMediaState?) {
        val identity = media?.identity
        if (identity == _state.value.mediaIdentity) return

        syncJob?.cancel()
        searchJob?.cancel()
        previewJob?.cancel()

        if (media == null) {
            _state.update {
                it.copy(
                    mediaIdentity = null,
                    queryTitle = "",
                    queryArtist = "",
                    queryAlbum = "",
                    searching = false,
                    hasSearched = false,
                    results = emptyList(),
                    error = null,
                    previewCandidate = null,
                    previewDocument = null,
                    previewLoading = false,
                    previewKey = null,
                    applying = false,
                    manualSelection = null,
                )
            }
            return
        }

        // Publish the identity immediately so media updates (pause/play, artwork) do not restart
        // the form while the saved mapping is being read.
        _state.update {
            it.copy(
                mediaIdentity = identity,
                searching = false,
                hasSearched = false,
                results = emptyList(),
                error = null,
                previewCandidate = null,
                previewDocument = null,
                previewLoading = false,
                previewKey = null,
                applying = false,
            )
        }

        syncJob = viewModelScope.launch {
            val selection = manualLyricsStore.load(media.title, media.artist, media.album)
            _state.update { current ->
                if (current.mediaIdentity != identity) return@update current
                current.copy(
                    queryTitle = selection?.mappedTitle?.takeIf { it.isNotBlank() } ?: media.title,
                    queryArtist = selection?.mappedArtist?.takeIf { it.isNotBlank() } ?: media.artist,
                    queryAlbum = selection?.mappedAlbum?.takeIf { it.isNotBlank() } ?: media.album,
                    manualSelection = selection,
                )
            }
        }
    }

    fun setQueryTitle(value: String) = _state.update { it.copy(queryTitle = value) }

    fun setQueryArtist(value: String) = _state.update { it.copy(queryArtist = value) }

    fun setQueryAlbum(value: String) = _state.update { it.copy(queryAlbum = value) }

    /** Restores the form to the metadata the media session reports. */
    fun resetQueryToMedia(media: ExternalMediaState?) {
        if (media == null) return
        _state.update {
            it.copy(
                queryTitle = media.title,
                queryArtist = media.artist,
                queryAlbum = media.album,
            )
        }
    }

    fun search(media: ExternalMediaState?) {
        val current = _state.value
        if (media == null || current.searching || current.queryTitle.isBlank()) return

        searchJob?.cancel()
        previewJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    searching = true,
                    hasSearched = true,
                    error = null,
                    results = emptyList(),
                    previewCandidate = null,
                    previewDocument = null,
                    previewLoading = false,
                    previewKey = null,
                )
            }

            // Local sources describe the file this session is playing, so they do not depend on
            // the (possibly edited) search metadata. Surface them first, like BetterLyrics does
            // for its local providers, while the online lookup is still running.
            val localResults = try {
                localLyricsResolver
                    .candidates(media, repository.sourceOrder.filter { it.isLocal })
                    .mapNotNull { local -> localCandidate(media, local) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                emptyList()
            }
            if (localResults.isNotEmpty()) {
                _state.update { it.copy(results = localResults) }
            }

            try {
                val onlineResults = repository.search(
                    LyricsQuery(
                        title = current.queryTitle.trim(),
                        artist = current.queryArtist.trim(),
                        album = current.queryAlbum.trim(),
                        durationMs = media.durationMs,
                        songId = media.mediaId,
                    )
                )
                _state.update {
                    it.copy(
                        searching = false,
                        results = (localResults + onlineResults)
                            .sortedByDescending { candidate -> candidate.score },
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        searching = false,
                        results = localResults,
                        error = error.message ?: error::class.simpleName,
                    )
                }
            }
        }
    }

    private suspend fun localCandidate(
        media: ExternalMediaState,
        local: LocalLyricsCandidate,
    ): BetterLyricsCandidate? {
        val provider = when (local.source) {
            LyricsSourceId.EMBEDDED -> LyricsProvider.EMBEDDED
            LyricsSourceId.LOCAL_LRC -> LyricsProvider.LOCAL_LRC
            else -> return null
        }
        val labelRes = when (local.source) {
            LyricsSourceId.EMBEDDED -> R.string.lyrics_source_embedded
            else -> R.string.lyrics_source_local_lrc
        }
        return BetterLyricsCandidate(
            provider = provider,
            providerLabel = context.getString(labelRes),
            result = LyricsSearchResult(
                provider = provider,
                raw = local.raw,
                title = media.title,
                artist = media.artist,
                album = media.album,
                durationMs = media.durationMs,
                matchPercentage = 100,
            ),
            wordTimed = LyricsFormatDetector.detect(local.raw).isWordTimed,
            previewLines = repository.previewLines(local.raw),
        )
    }

    /** Parses a candidate so the preview can show its lines and track availability. */
    fun openPreview(candidate: BetterLyricsCandidate, media: ExternalMediaState?) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    previewCandidate = candidate,
                    previewDocument = null,
                    previewTrack = PreviewTrack.ORIGINAL,
                    previewLoading = true,
                    previewKey = media?.let {
                        manualLyricsStore.keyFor(it.title, it.artist, it.album)
                    },
                    error = null,
                )
            }
            try {
                val document = repository.parse(candidate, media?.durationMs)
                if (document.isEmpty) {
                    _state.update {
                        it.copy(
                            previewCandidate = null,
                            previewLoading = false,
                            error = context.getString(R.string.betterlyrics_preview_failed),
                        )
                    }
                    return@launch
                }
                _state.update {
                    it.copy(
                        previewDocument = document,
                        previewLoading = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        previewCandidate = null,
                        previewDocument = null,
                        previewLoading = false,
                        error = error.message ?: context.getString(R.string.betterlyrics_preview_failed),
                    )
                }
            }
        }
    }

    fun closePreview() {
        previewJob?.cancel()
        _state.update {
            it.copy(
                previewCandidate = null,
                previewDocument = null,
                previewLoading = false,
                previewTrack = PreviewTrack.ORIGINAL,
                previewKey = null,
            )
        }
    }

    fun selectPreviewTrack(track: PreviewTrack) {
        val current = _state.value
        when (track) {
            PreviewTrack.ORIGINAL -> Unit
            PreviewTrack.TRANSLATION -> if (!current.previewHasTranslation) return
            PreviewTrack.ROMANIZATION -> if (!current.previewHasRomanization) return
        }
        _state.update { it.copy(previewTrack = track) }
    }

    /** Persists the previewed document as this track's lyrics and notifies the lyrics page. */
    fun apply(media: ExternalMediaState?) {
        val current = _state.value
        val document = current.previewDocument
        if (media == null || document == null || current.applying) return

        // The preview is bound to the track it was opened for; never save it under a track that
        // started playing while the sheet was open.
        val mediaKey = manualLyricsStore.keyFor(media.title, media.artist, media.album)
        if (current.previewKey != null && current.previewKey != mediaKey) {
            _state.update {
                it.copy(
                    previewCandidate = null,
                    previewDocument = null,
                    previewKey = null,
                    error = context.getString(R.string.lyrics_manual_search_track_changed),
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(applying = true, error = null) }
            try {
                val stored = manualLyricsStore.save(
                    title = media.title,
                    artist = media.artist,
                    album = media.album,
                    selection = ManualLyricsSelection(
                        document = document,
                        mappedTitle = current.queryTitle.trim(),
                        mappedArtist = current.queryArtist.trim(),
                        mappedAlbum = current.queryAlbum.trim(),
                        providerLabel = current.previewCandidate?.providerLabel.orEmpty(),
                    ),
                )
                _state.update {
                    it.copy(
                        applying = false,
                        appliedCount = it.appliedCount + 1,
                        manualSelection = stored,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        applying = false,
                        error = error.message ?: context.getString(R.string.betterlyrics_apply_failed),
                    )
                }
            }
        }
    }

    /** Drops the manual choice so the normal source order takes over again. */
    fun clearManualSelection(media: ExternalMediaState?) {
        if (media == null) return
        viewModelScope.launch {
            manualLyricsStore.clear(media.title, media.artist, media.album)
            _state.update {
                it.copy(
                    manualSelection = null,
                    queryTitle = media.title,
                    queryArtist = media.artist,
                    queryAlbum = media.album,
                )
            }
        }
    }
}
