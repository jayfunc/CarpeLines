package io.github.jayfunc.carpelines.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jayfunc.carpelines.data.model.CoverArtSource
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The only screen that edits cover source order. The stored order doubles as the enabled set,
 * exactly like the lyrics source order, so this view model owns both reordering and toggling.
 */
@HiltViewModel
class CoverArtSourceOrderViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _order = MutableStateFlow(CoverArtSource.DefaultOrder)
    val order: StateFlow<List<CoverArtSource>> = _order.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.coverArtSourceOrderFlow.collect { stored ->
                _order.value = stored
            }
        }
    }

    /** Places [source] at [targetIndex] of the enabled order, for drag-and-drop reordering. */
    fun moveTo(source: CoverArtSource, targetIndex: Int) {
        val current = _order.value.toMutableList()
        val index = current.indexOf(source)
        if (index < 0 || current.isEmpty()) return
        val target = targetIndex.coerceIn(0, current.lastIndex)
        if (index == target) return
        current.add(target, current.removeAt(index))
        save(current)
    }

    fun setEnabled(source: CoverArtSource, enabled: Boolean) {
        val current = _order.value.toMutableList()
        if (enabled) {
            if (!current.contains(source)) current += source
        } else {
            // Keeping one source enabled guarantees the cover chain never has an empty list.
            if (current.size <= 1 && current.contains(source)) return
            current.remove(source)
        }
        save(current)
    }

    fun reset() {
        save(CoverArtSource.DefaultOrder)
    }

    private fun save(order: List<CoverArtSource>) {
        if (order.isEmpty()) return
        _order.value = order
        viewModelScope.launch {
            userPreferencesRepository.setCoverArtSourceOrder(order)
        }
    }
}
