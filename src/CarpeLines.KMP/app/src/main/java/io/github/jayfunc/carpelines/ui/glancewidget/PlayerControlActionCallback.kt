package io.github.jayfunc.carpelines.ui.glancewidget

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import io.github.jayfunc.carpelines.MainActivity
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Widget transport runs through the active system media session, the only playback CarpeLines
 * has. Actions are no-ops when no foreign session is connected.
 */
class PlayerControlActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        // A widget tap can be the first thing that starts the process after a reboot. Give the
        // notification listener a moment to attach before sending transport commands.
        ExternalMediaMonitor.ensureConnected(context)
        if (ExternalMediaMonitor.state.value == null) {
            withTimeoutOrNull(400L) {
                ExternalMediaMonitor.state.first { it != null }
            }
        }

        when (parameters[PlayerActions.key]) {
            PlayerActions.PLAY_PAUSE -> ExternalMediaMonitor.playPause()
            PlayerActions.PREVIOUS -> ExternalMediaMonitor.previous()
            PlayerActions.NEXT -> ExternalMediaMonitor.next()
        }
        WidgetMediaUpdater.refresh(context)
    }
}

object PlayerActions {
    val key = ActionParameters.Key<String>("playerActionKey_v2")
    const val PLAY_PAUSE = "io.github.jayfunc.carpelines.ACTION_WIDGET_PLAY_PAUSE"
    const val NEXT = "io.github.jayfunc.carpelines.ACTION_WIDGET_NEXT"
    const val PREVIOUS = "io.github.jayfunc.carpelines.ACTION_WIDGET_PREVIOUS"
}

class OpenLyricsActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = "carpelines://lyrics".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
}
