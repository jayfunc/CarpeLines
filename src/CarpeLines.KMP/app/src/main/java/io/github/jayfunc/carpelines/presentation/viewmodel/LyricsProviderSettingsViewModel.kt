package io.github.jayfunc.carpelines.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State behind the lyrics provider settings page (Apple Music token, LibreTranslate fallback and
 * the translation/transliteration track toggles), persisted by [BetterLyricsRepository].
 *
 * The page has no save button: every edit is applied immediately, and text fields are written
 * after a short pause so typing does not hit the (encrypted) preference file per keystroke.
 */
@HiltViewModel
class LyricsProviderSettingsViewModel @Inject constructor(
    private val repository: BetterLyricsRepository,
) : ViewModel() {

    data class UiState(
        val appleMusicToken: String = "",
        val libreTranslateServer: String = "",
        val translationEnabled: Boolean = true,
        val romanizationEnabled: Boolean = true,
    )

    private val _state = MutableStateFlow(
        UiState(
            appleMusicToken = repository.appleMusicToken.orEmpty(),
            libreTranslateServer = repository.libreTranslateServer.orEmpty(),
            translationEnabled = repository.translationEnabled,
            romanizationEnabled = repository.romanizationEnabled,
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var tokenSaveJob: Job? = null
    private var serverSaveJob: Job? = null

    fun setAppleMusicToken(value: String) {
        _state.update { it.copy(appleMusicToken = value) }
        tokenSaveJob?.cancel()
        tokenSaveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            repository.appleMusicToken = value
        }
    }

    fun setLibreTranslateServer(value: String) {
        _state.update { it.copy(libreTranslateServer = value) }
        serverSaveJob?.cancel()
        serverSaveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            repository.libreTranslateServer = value
        }
    }

    fun setTranslationEnabled(enabled: Boolean) {
        _state.update { it.copy(translationEnabled = enabled) }
        repository.translationEnabled = enabled
    }

    fun setRomanizationEnabled(enabled: Boolean) {
        _state.update { it.copy(romanizationEnabled = enabled) }
        repository.romanizationEnabled = enabled
    }

    override fun onCleared() {
        // Leaving the page cancels the pending debounce: flush the last keystrokes instead.
        val current = _state.value
        if (repository.appleMusicToken.orEmpty() != current.appleMusicToken) {
            repository.appleMusicToken = current.appleMusicToken
        }
        if (repository.libreTranslateServer.orEmpty() != current.libreTranslateServer) {
            repository.libreTranslateServer = current.libreTranslateServer
        }
        super.onCleared()
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 400L
    }
}
