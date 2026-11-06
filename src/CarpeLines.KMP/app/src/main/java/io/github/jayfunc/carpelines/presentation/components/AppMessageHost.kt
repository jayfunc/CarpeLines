package io.github.jayfunc.carpelines.presentation.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Transient feedback channel for the app shell.
 *
 * Material 3 carries transient, non-blocking feedback on a snackbar, not a platform toast: a
 * snackbar is part of the app's surface, can carry an undo/retry action, and is announced by
 * screen readers. Screens therefore talk to a [SnackbarHostState] that the enclosing scaffold
 * owns instead of reaching for `android.widget.Toast`.
 */
class AppMessageHost internal constructor(
    private val scope: CoroutineScope,
    val snackbarHostState: SnackbarHostState
) {

    /** Shows [message]. A later message replaces the current one rather than queueing behind it. */
    fun show(message: String) {
        if (message.isBlank()) return
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = false,
                duration = SnackbarDuration.Short
            )
        }
    }

    /** Shows [message] with a single action, used for undo/retry on reversible operations. */
    fun show(
        message: String,
        actionLabel: String,
        duration: SnackbarDuration = SnackbarDuration.Long,
        onAction: () -> Unit
    ) {
        if (message.isBlank()) return
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = false,
                duration = duration
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

/**
 * The host for the current screen tree. A screen that is not wrapped in a shell (a dialog, a
 * sheet, a preview) sees `null` and simply stays silent instead of crashing.
 */
val LocalAppMessageHost = staticCompositionLocalOf<AppMessageHost?> { null }

/** Creates a host backed by [snackbarHostState] and a scope tied to the current composition. */
@Composable
fun rememberAppMessageHost(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): AppMessageHost {
    val scope = rememberCoroutineScope()
    return remember(snackbarHostState, scope) { AppMessageHost(scope, snackbarHostState) }
}

/**
 * Shorthand for the common case: a screen only needs to post a message.
 *
 * ```kotlin
 * val showMessage = rememberAppMessage()
 * Button(onClick = { showMessage(context.getString(R.string.saved)) })
 * ```
 */
@Composable
fun rememberAppMessage(): (String) -> Unit {
    val host = LocalAppMessageHost.current
    return remember(host) {
        { message: String -> host?.show(message) }
    }
}
