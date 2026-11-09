package io.github.jayfunc.carpelines.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jayfunc.carpelines.data.external.ExternalPlaybackStore
import io.github.jayfunc.carpelines.data.model.Song
import io.github.jayfunc.carpelines.data.repository.MusicRepository
import io.github.jayfunc.carpelines.data.stats.PlaybackPlayerFilterProvider
import io.github.jayfunc.carpelines.data.stats.PlaybackStatsRepository
import io.github.jayfunc.carpelines.data.stats.PlaybackStatsRepository.ListeningHeatmap
import io.github.jayfunc.carpelines.data.stats.PlaybackStatsRepository.PlaybackStatsSummary
import io.github.jayfunc.carpelines.data.stats.StatsTimeRange
import io.github.jayfunc.carpelines.di.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val playbackStatsRepository: PlaybackStatsRepository,
    private val musicRepository: MusicRepository,
    private val externalPlaybackStore: ExternalPlaybackStore,
    private val playerFilterProvider: PlaybackPlayerFilterProvider,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    data class StatsUiState(
        val selectedRange: StatsTimeRange = StatsTimeRange.WEEK,
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val summary: PlaybackStatsSummary? = null,
        val heatmap: ListeningHeatmap? = null,
        val availableRanges: ImmutableList<StatsTimeRange> = StatsTimeRange.entries.toImmutableList()
    )

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _weeklyOverview = MutableStateFlow<PlaybackStatsSummary?>(null)
    val weeklyOverview: StateFlow<PlaybackStatsSummary?> = _weeklyOverview.asStateFlow()

    private val _homeOverviewState =
        MutableStateFlow<HomeStatsOverviewState>(HomeStatsOverviewState.Loading)
    val homeOverviewState: StateFlow<HomeStatsOverviewState> = _homeOverviewState.asStateFlow()

    /** Only the latest home refresh may publish, so an older result never overwrites a newer one. */
    private var homeOverviewJob: Job? = null

    @Volatile
    private var cachedSongs: List<Song>? = null

    /** The wall does not depend on the selected range, so tab switches can reuse it. */
    private var cachedHeatmap: ListeningHeatmap? = null

    init {
        observeStatsRefreshFlow()
        refreshRange(
            range = StatsTimeRange.WEEK,
            showLoading = true,
            updateWeeklyOverview = true
        )
        refreshHomeOverview()
    }

    fun onRangeSelected(range: StatsTimeRange) {
        if (range == _uiState.value.selectedRange && !_uiState.value.isLoading) {
            return
        }
        refreshRange(
            range = range,
            showLoading = true,
            updateWeeklyOverview = range == StatsTimeRange.WEEK
        )
    }

    fun refreshWeeklyOverview() {
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) {
                    val songs = loadSongs()
                    playbackStatsRepository.loadSummary(
                        range = StatsTimeRange.WEEK,
                        songs = songs,
                        playerFilter = playerFilterProvider.current()
                    )
                }
            }.onSuccess { summary ->
                _weeklyOverview.value = summary
            }.onFailure { throwable ->
                Timber.e(throwable, "Failed to load weekly stats overview")
                _weeklyOverview.value = null
            }
        }
    }

    fun refreshHomeOverview() {
        homeOverviewJob?.cancel()
        homeOverviewJob = viewModelScope.launch {
            val summary = try {
                withContext(dispatchers.io) {
                    val songs = loadSongs()
                    val playerFilter = playerFilterProvider.current()
                    firstSummaryWithActivity(HomeOverviewRanges) { range ->
                        playbackStatsRepository.loadSummary(range, songs, playerFilter = playerFilter)
                    }
                }
            } catch (cancellation: CancellationException) {
                // A newer refresh replaced this one; it publishes the result instead.
                throw cancellation
            } catch (throwable: Exception) {
                Timber.e(throwable, "Failed to load home stats overview")
                // Keep showing the last good summary rather than flashing the empty state.
                if (_homeOverviewState.value !is HomeStatsOverviewState.Ready) {
                    _homeOverviewState.value = HomeStatsOverviewState.Empty
                }
                return@launch
            }
            _homeOverviewState.value = summary
                ?.let { HomeStatsOverviewState.Ready(it) }
                ?: HomeStatsOverviewState.Empty
        }
    }

    private fun refreshRange(
        range: StatsTimeRange,
        showLoading: Boolean = true,
        updateWeeklyOverview: Boolean = false,
        refreshHeatmap: Boolean = false
    ) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, isRefreshing = false, selectedRange = range) }
            } else {
                _uiState.update { it.copy(isRefreshing = true, selectedRange = range) }
            }
            val loaded = runCatching {
                withContext(dispatchers.io) {
                    val songs = loadSongs()
                    val playerFilter = playerFilterProvider.current()
                    val summary = playbackStatsRepository.loadSummary(
                        range = range,
                        songs = songs,
                        playerFilter = playerFilter
                    )
                    val cached = cachedHeatmap
                    val heatmap = if (refreshHeatmap || cached == null) {
                        playbackStatsRepository.loadListeningHeatmap(playerFilter)
                            .also { cachedHeatmap = it }
                    } else {
                        cached
                    }
                    summary to heatmap
                }
            }
            val summary = loaded.getOrNull()?.first
            val heatmap = loaded.getOrNull()?.second
            summary?.let { currentSummary ->
                if (updateWeeklyOverview) {
                    _weeklyOverview.value = currentSummary
                }
            }
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    isRefreshing = false,
                    summary = summary,
                    heatmap = heatmap ?: current.heatmap,
                    selectedRange = range
                )
            }
            loaded.exceptionOrNull()?.let { Timber.e(it, "Failed to load stats for range %s", range) }
        }
    }

    private fun observeStatsRefreshFlow() {
        viewModelScope.launch {
            playbackStatsRepository.refreshFlow
                .drop(1)
                .collectLatest {
                    val selectedRange = _uiState.value.selectedRange
                    refreshRange(
                        range = selectedRange,
                        showLoading = false,
                        updateWeeklyOverview = selectedRange == StatsTimeRange.WEEK,
                        refreshHeatmap = true
                    )
                    if (selectedRange != StatsTimeRange.WEEK) {
                        refreshWeeklyOverview()
                    }
                    refreshHomeOverview()
                }
        }
    }

    fun requestStatsRefresh() {
        playbackStatsRepository.requestRefresh()
    }

    fun forceRegenerateStats() {
        cachedSongs = null
        playbackStatsRepository.requestRefresh()
    }

    /**
     * Library songs, plus the tracks other players reported.
     *
     * The aggregation is keyed by song id and drops ids it cannot name, so external playback would
     * only ever have contributed anonymous totals. Merging the remembered external tracks gives
     * every play a title, an artist and a cover. Only the library scan is cached: the external
     * snapshot changes while the app runs.
     */
    private suspend fun loadSongs(): List<Song> {
        val library = cachedSongs ?: musicRepository.getAllSongsOnce().also { cachedSongs = it }
        val external = externalPlaybackStore.songs()
        if (external.isEmpty()) return library
        val knownIds = library.mapTo(HashSet(library.size)) { it.id }
        return library + external.filterNot { it.id in knownIds }
    }

    private companion object {
        val HomeOverviewRanges = listOf(
            StatsTimeRange.WEEK,
            StatsTimeRange.MONTH,
            StatsTimeRange.YEAR,
            StatsTimeRange.ALL
        )
    }
}

/**
 * Loads [ranges] in order and returns the first summary with listening activity, or null when
 * none has any. Ranges after the first match are never loaded.
 */
internal suspend fun firstSummaryWithActivity(
    ranges: List<StatsTimeRange>,
    load: suspend (StatsTimeRange) -> PlaybackStatsSummary
): PlaybackStatsSummary? {
    for (range in ranges) {
        val summary = load(range)
        if (summary.hasListeningActivity()) return summary
    }
    return null
}
