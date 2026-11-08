package io.github.jayfunc.carpelines.data.external

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Collects the audio session ids that external players publish for audio-effect apps.
 *
 * Android has no public API that exposes another app's audio session id, but a player that
 * supports external effects broadcasts [AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION]
 * with the id it just opened. System-wide equalizers attach to those ids; the spectrum analyzer
 * uses the same handshake to attach a [android.media.audiofx.Visualizer].
 *
 * The receiver must live in a running component: the action is not on the implicit-broadcast
 * exemption list, so a manifest receiver would not receive it on Android 8+. It is therefore
 * registered by [io.github.jayfunc.carpelines.data.external.service.ExternalMediaListenerService],
 * which the system keeps bound while notification access is granted.
 */
object ExternalAudioSessionRegistry {

    private const val TAG = "ExternalAudioSession"

    private val _sessions = MutableStateFlow<Map<String, Int>>(emptyMap())

    /** Package name to the audio session id that package last opened. */
    val sessions: StateFlow<Map<String, Int>> = _sessions.asStateFlow()

    private var receiver: BroadcastReceiver? = null
    private var appContext: Context? = null

    @Synchronized
    fun start(context: Context) {
        if (receiver != null) return

        val application = context.applicationContext
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        val newReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent ?: return
                val packageName = intent
                    .getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
                    ?.takeIf { it.isNotBlank() }
                    ?: return
                val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
                when (intent.action) {
                    AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                        if (sessionId <= 0) return
                        _sessions.update { it + (packageName to sessionId) }
                        Log.d(TAG, "Session opened: $packageName -> $sessionId")
                    }

                    AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                        // A player can close an old session after it has already opened a new one;
                        // only drop the entry when the id matches the one we kept.
                        if (sessionId > 0 && _sessions.value[packageName] != sessionId) return
                        _sessions.update { it - packageName }
                        Log.d(TAG, "Session closed: $packageName")
                    }
                }
            }
        }

        runCatching {
            ContextCompat.registerReceiver(
                application,
                newReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )
            receiver = newReceiver
            appContext = application
        }.onFailure { Log.w(TAG, "Unable to register audio session receiver", it) }
    }

    @Synchronized
    fun stop() {
        val current = receiver ?: return
        runCatching { appContext?.unregisterReceiver(current) }
            .onFailure { Log.w(TAG, "Unable to unregister audio session receiver", it) }
        receiver = null
        appContext = null
        Log.d(TAG, "Receiver stopped")
    }

    /** Session id last announced by [packageName], or null when it never opened one. */
    fun sessionIdFor(packageName: String?): Int? =
        packageName
            ?.takeIf { it.isNotBlank() }
            ?.let { _sessions.value[it] }
}
