package io.github.jayfunc.carpelines.presentation.components.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Visibility of everything that is not lyrics.
 *
 * The listening screen is meant to be the song, so the chrome shows up when the user asks for it
 * and then gets out of the way after the delay the user picked. [autoHideEnabled] is the opt-in
 * switch: when it is off the chrome stays until the user leaves.
 */
@Stable
class LyricsChromeState internal constructor(
    private val autoHideMillis: Long,
    initiallyVisible: Boolean = true,
) {

    var visible by mutableStateOf(initiallyVisible)
        private set

    /** Bumped by every interaction so the auto-hide timer restarts. */
    internal var interactionTicks by mutableIntStateOf(0)
        private set

    fun show() {
        visible = true
        interactionTicks++
    }

    fun hide() {
        visible = false
    }

    fun toggle() {
        if (visible) hide() else show()
    }

    /** Keeps the chrome up while the user is interacting with it. */
    fun hold() {
        if (visible) interactionTicks++
    }

    internal fun autoHide() {
        visible = false
    }

    internal fun autoHideDelayMillis(): Long = autoHideMillis
}

@Composable
fun rememberLyricsChromeState(
    autoHideEnabled: Boolean,
    autoHideMillis: Long = DEFAULT_AUTO_HIDE_MS,
): LyricsChromeState {
    // The face the user left on is saved with the destination, so coming back from the appearance
    // settings reopens the immersive face instead of falling back to the overview.
    var savedVisible by rememberSaveable { mutableStateOf(true) }
    // Re-created when the delay changes so the new value applies on the next timer run.
    val state = remember(autoHideMillis) { LyricsChromeState(autoHideMillis, savedVisible) }
    LaunchedEffect(state.visible) { savedVisible = state.visible }
    val ticks = state.interactionTicks
    val visible = state.visible
    LaunchedEffect(ticks, visible, autoHideEnabled) {
        if (!visible || !autoHideEnabled) return@LaunchedEffect
        delay(state.autoHideDelayMillis())
        state.autoHide()
    }
    return state
}

const val DEFAULT_AUTO_HIDE_MS = 3_500L
