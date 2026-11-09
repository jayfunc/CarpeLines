package io.github.jayfunc.carpelines.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jayfunc.carpelines.data.model.Song
import io.github.jayfunc.carpelines.data.repository.MusicRepository
import io.github.jayfunc.carpelines.data.stats.ListeningRecap
import io.github.jayfunc.carpelines.data.stats.PlaybackPlayerFilterProvider
import io.github.jayfunc.carpelines.data.stats.PlaybackStatsRepository
import io.github.jayfunc.carpelines.di.DispatcherProvider
import io.github.jayfunc.carpelines.presentation.recap.RecapSchedule
import io.github.jayfunc.carpelines.presentation.recap.RecapUiModel
import io.github.jayfunc.carpelines.presentation.recap.RecapUiState
import io.github.jayfunc.carpelines.presentation.recap.buildRecapSlides
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Builds the recap story for the year in the `year` navigation argument. The story is a
 * snapshot: plays recorded while it is open do not reshuffle the slides under the reader.
 */
@HiltViewModel
class RecapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playbackStatsRepository: PlaybackStatsRepository,
    private val musicRepository: MusicRepository,
    private val playerFilterProvider: PlaybackPlayerFilterProvider,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    val year: Int = savedStateHandle.get<Int>(YEAR_ARG) ?: LocalDate.now().year

    private val _uiState = MutableStateFlow<RecapUiState>(RecapUiState.Loading)
    val uiState: StateFlow<RecapUiState> = _uiState.asStateFlow()

    /** The year's most played songs still in the library, most played first. */
    @Volatile
    var topSongsQueue: List<Song> = emptyList()
        private set

    init {
        viewModelScope.launch {
            try {
                load()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Exception) {
                Timber.e(throwable, "Failed to build the %d recap", year)
                _uiState.value = RecapUiState.NotEnoughData(year)
            }
        }
    }

    private suspend fun load() {
        val songs = withContext(dispatchers.io) { musicRepository.getAllSongsOnce() }
        val recap = playbackStatsRepository.loadRecap(
            year = year,
            songs = songs,
            playerFilter = playerFilterProvider.current()
        )
        if (recap == null || recap.summary.totalDurationMs < RecapSchedule.MIN_RECAP_LISTENING_MS) {
            _uiState.value = RecapUiState.NotEnoughData(year)
            return
        }

        val songsById = songs.associateBy { it.id }
        topSongsQueue = recap.summary.songs.mapNotNull { songsById[it.songId] }

        // The recap ends today while the year is still running.
        val lastDay = LocalDate.ofYearDay(year, recap.daysInRange)
        val isSoFar = RecapSchedule.isSoFar(year, lastDay)
        val covers = recap.summary.topArtists
            .mapNotNull { artist -> recap.mostPlayedCoverOf(artist.artist, songsById)?.let { artist.artist to it } }
            .toMap()
        _uiState.value = RecapUiState.Ready(buildModel(recap, isSoFar, covers))
    }

    private suspend fun buildModel(
        recap: ListeningRecap,
        isSoFar: Boolean,
        artistCovers: Map<String, String>
    ): RecapUiModel {
        val slides = withContext(dispatchers.default) {
            buildRecapSlides(
                recap = recap,
                isSoFar = isSoFar,
                artistCovers = artistCovers,
                canPlayTopSongs = topSongsQueue.isNotEmpty()
            )
        }
        return RecapUiModel(year = year, isSoFar = isSoFar, slides = slides.toImmutableList())
    }

    companion object {
        /** Navigation argument holding the recap's calendar year. */
        const val YEAR_ARG = "year"
    }
}

/**
 * Cover of the most played track by [artistName], used as the artist's picture in the recap.
 * Matches the way stats split multi-artist songs into their separate artists.
 */
private fun ListeningRecap.mostPlayedCoverOf(artistName: String, songsById: Map<String, Song>): String? {
    val key = artistName.trim().lowercase(Locale.ROOT)
    return summary.songs.firstNotNullOfOrNull { played ->
        val cover = played.albumArtUri?.takeIf { it.isNotBlank() } ?: return@firstNotNullOfOrNull null
        val song = songsById[played.songId] ?: return@firstNotNullOfOrNull null
        val songArtists = song.artists.map { it.name }.ifEmpty { listOf(song.displayArtist) }
        cover.takeIf { songArtists.any { it.trim().lowercase(Locale.ROOT) == key } }
    }
}
