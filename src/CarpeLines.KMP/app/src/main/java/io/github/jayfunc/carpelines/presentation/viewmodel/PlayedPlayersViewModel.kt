package io.github.jayfunc.carpelines.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jayfunc.carpelines.data.external.ExternalPlaybackStore
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The players that have actually played something on this device.
 *
 * The home screen used to list every installed app CarpeLines can follow, which read as "these are
 * the only supported players" and said nothing about the user's own listening.
 */
@HiltViewModel
class PlayedPlayersViewModel @Inject constructor(
    store: ExternalPlaybackStore
) : ViewModel() {

    val players: StateFlow<List<ExternalPlaybackStore.PlayerRecord>> = store.snapshot
        .map { snapshot -> snapshot.players.sortedByDescending { it.lastPlayedAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Distinct tracks across every player, so two players of one song still count once. */
    val playedTrackCount: StateFlow<Int> = store.snapshot
        .map { snapshot -> snapshot.players.flatMap { it.trackIds }.distinct().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
