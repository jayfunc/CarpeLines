package io.github.jayfunc.carpelines.presentation.viewmodel

import androidx.lifecycle.ViewModel
import io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsRepository
import io.github.jayfunc.carpelines.data.betterlyrics.LyricsSourceId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class LyricsSourceOrderViewModel @Inject constructor(
    private val repository: BetterLyricsRepository,
) : ViewModel() {

    private val _order = MutableStateFlow(repository.sourceOrder)
    val order: StateFlow<List<LyricsSourceId>> = _order.asStateFlow()

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
    }
}
