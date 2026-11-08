package io.github.jayfunc.carpelines.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsRepository
import io.github.jayfunc.carpelines.data.betterlyrics.LyricsSourceId
import io.github.jayfunc.carpelines.data.model.LyricsSourcePreference
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The only screen that edits lyrics source order.
 *
 * [BetterLyricsRepository.sourceOrder] is the single source of truth. The legacy priority preset
 * and the "online lyrics" switch still read by the playback pipeline are derived from it on every
 * change, so no other screen can silently disagree with what this page shows.
 */
@HiltViewModel
class LyricsSourceOrderViewModel @Inject constructor(
    private val repository: BetterLyricsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _order = MutableStateFlow(repository.sourceOrder)
    val order: StateFlow<List<LyricsSourceId>> = _order.asStateFlow()

    init {
        viewModelScope.launch {
            // Older builds had a separate "online lyrics" master switch and a three-way preset.
            // Fold both into the one editable order: a disabled switch removes every online source,
            // and the derived values are rewritten so upgrades (and restored backups) cannot leave
            // a hidden setting that disagrees with what this page shows.
            val onlineEnabled = userPreferencesRepository.externalLyricsEnabledFlow.first()
            val normalized = if (onlineEnabled) {
                _order.value
            } else {
                _order.value.filter { it.isLocal }.ifEmpty { listOf(LyricsSourceId.EMBEDDED) }
            }
            save(normalized)
        }
    }

    fun move(source: LyricsSourceId, delta: Int) {
        val current = _order.value.toMutableList()
        val index = current.indexOf(source)
        if (index < 0) return
        val target = index + delta
        if (target !in current.indices) return
        current[index] = current[target].also { current[target] = current[index] }
        save(current)
    }

    /** Places [source] at [targetIndex] of the enabled order, for drag-and-drop reordering. */
    fun moveTo(source: LyricsSourceId, targetIndex: Int) {
        val current = _order.value.toMutableList()
        val index = current.indexOf(source)
        if (index < 0 || current.isEmpty()) return
        val target = targetIndex.coerceIn(0, current.lastIndex)
        if (index == target) return
        current.add(target, current.removeAt(index))
        save(current)
    }

    fun setEnabled(source: LyricsSourceId, enabled: Boolean) {
        val current = _order.value.toMutableList()
        if (enabled) {
            if (!current.contains(source)) current += source
        } else {
            // Keeping one source enabled guarantees lyrics resolution never has an empty list.
            if (current.size <= 1 && current.contains(source)) return
            current.remove(source)
        }
        save(current)
    }

    fun reset() {
        save(LyricsSourceId.DefaultOrder)
    }

    private fun save(order: List<LyricsSourceId>) {
        if (order.isEmpty()) return
        repository.sourceOrder = order
        _order.value = order
        // The playback pipeline still reads the legacy preset and master switch; keep both as
        // derived values of this list so the single editable order stays authoritative.
        viewModelScope.launch {
            userPreferencesRepository.setLyricsSourcePreference(order.legacyPreference())
            userPreferencesRepository.setExternalLyricsEnabled(order.any { !it.isLocal })
        }
    }
}

/** Mirrors the three legacy presets so the pipeline follows the one editable source order. */
private fun List<LyricsSourceId>.legacyPreference(): LyricsSourcePreference = when {
    firstOrNull()?.isLocal == false -> LyricsSourcePreference.API_FIRST
    firstOrNull() == LyricsSourceId.LOCAL_LRC -> LyricsSourcePreference.LOCAL_FIRST
    else -> LyricsSourcePreference.EMBEDDED_FIRST
}
