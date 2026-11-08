package io.github.jayfunc.carpelines.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor
import io.github.jayfunc.carpelines.data.external.ExternalPlaybackStore
import io.github.jayfunc.carpelines.data.external.ExternalPlayerCandidate
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.data.stats.PlaybackStatsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Which players CarpeLines may follow.
 *
 * The list mixes players the device has already played with (from [ExternalPlaybackStore]) and
 * players that are publishing a session right now (from [ExternalMediaMonitor]). The latter is
 * essential while the filter is on: an ignored player never records a play, so without the live
 * list it could never be selected again.
 */
@HiltViewModel
class PlayerFilterViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    store: ExternalPlaybackStore,
    private val playbackStatsRepository: PlaybackStatsRepository,
    private val listeningStatsTracker: ListeningStatsTracker,
) : ViewModel() {

    data class PlayerOption(
        val packageName: String,
        val appLabel: String,
        val isPlaying: Boolean,
    )

    data class UiState(
        val filterEnabled: Boolean = false,
        val allowedPackages: Set<String> = emptySet(),
        val players: List<PlayerOption> = emptyList(),
    ) {
        /** Enabled with nothing selected would follow no player at all; the screen warns about it. */
        val hasNoSelectionWarning: Boolean
            get() = filterEnabled && allowedPackages.isEmpty()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.externalPlayerFilterEnabledFlow,
                userPreferencesRepository.externalPlayerAllowedPackagesFlow,
                store.snapshot,
                ExternalMediaMonitor.playerCandidates,
            ) { enabled, allowed, snapshot, candidates ->
                UiState(
                    filterEnabled = enabled,
                    allowedPackages = allowed,
                    players = mergePlayers(snapshot.players, candidates),
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setFilterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // Turning the filter on with nothing selected would silently follow no player. Preselect
            // every player known at that moment; the user can switch off the ones to ignore.
            if (enabled && _uiState.value.allowedPackages.isEmpty()) {
                userPreferencesRepository.setExternalPlayerAllowedPackages(
                    _uiState.value.players.mapTo(HashSet()) { it.packageName }
                )
            }
            userPreferencesRepository.setExternalPlayerFilterEnabled(enabled)
            refreshPlaybackViews()
        }
    }

    fun setPlayerAllowed(packageName: String, allowed: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.allowedPackages
            userPreferencesRepository.setExternalPlayerAllowedPackages(
                if (allowed) current + packageName else current - packageName
            )
            refreshPlaybackViews()
        }
    }

    /** Back to following every player; the next enable starts from the players known then. */
    fun reset() {
        viewModelScope.launch {
            userPreferencesRepository.setExternalPlayerFilterEnabled(false)
            userPreferencesRepository.setExternalPlayerAllowedPackages(emptySet())
            refreshPlaybackViews()
        }
    }

    /** Stats and the home "recently played" strip were built from the previous selection. */
    private fun refreshPlaybackViews() {
        playbackStatsRepository.requestRefresh()
        listeningStatsTracker.reloadHistory()
    }

    private fun mergePlayers(
        records: List<ExternalPlaybackStore.PlayerRecord>,
        candidates: List<ExternalPlayerCandidate>,
    ): List<PlayerOption> {
        val merged = LinkedHashMap<String, PlayerOption>()
        records.forEach { record ->
            merged[record.packageName] = PlayerOption(
                packageName = record.packageName,
                appLabel = record.appLabel,
                isPlaying = false,
            )
        }
        candidates.forEach { candidate ->
            merged[candidate.packageName] = PlayerOption(
                packageName = candidate.packageName,
                appLabel = candidate.appLabel,
                isPlaying = candidate.isPlaying,
            )
        }
        return merged.values.sortedWith(
            compareBy<PlayerOption> { !it.isPlaying }.thenBy { it.appLabel.lowercase() }
        )
    }
}
