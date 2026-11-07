package io.github.jayfunc.carpelines.data.external

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import io.github.jayfunc.carpelines.data.external.service.ExternalMediaListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Listens to the media sessions of every other player on the device, which is the Android
 * equivalent of BetterLyrics' SMTC (System Media Transport Controls) monitoring on Windows.
 *
 * The listener service is bound by the system once the user grants notification access; this
 * object exposes the active track, extracted playback position and transport controls as flows.
 */
object ExternalMediaMonitor {

    private const val TAG = "ExternalMediaMonitor"
    private const val POSITION_TICK_MS = 250L

    /**
     * Rebind attempts after a process start, spaced out rather than fired once: MIUI/HyperOS
     * gates the system's rebind behind its auto-start control and can refuse the first requests,
     * so a single attempt at Application.onCreate is not enough to recover from a force-stop.
     */
    private val REBIND_RETRY_DELAYS_MS = longArrayOf(0L, 1_000L, 3_000L, 6_000L, 12_000L)

    /** A bind/unbind loop must not turn into a rebind loop. */
    private const val SYSTEM_UNBOUND_REBIND_INTERVAL_MS = 15_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val controllers = mutableListOf<MediaController>()
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private var activeController: MediaController? = null
    private var appContext: Context? = null
    private var listenerComponent: ComponentName? = null
    private var sessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var tickerJob: Job? = null
    private var rebindJob: Job? = null
    private var lastSystemUnboundRebindAt = 0L
    private var selectedMediaId: String? = null

    private val _state = MutableStateFlow<ExternalMediaState?>(null)
    val state: StateFlow<ExternalMediaState?> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    fun attach(context: Context, component: ComponentName) {
        val application = context.applicationContext
        appContext = application
        Log.d(TAG, "attach($component)")
        if (listenerComponent == component && sessionsListener != null) {
            _connected.value = true
            rebindJob?.cancel()
            refreshSessions()
            return
        }
        detachInternal(clearState = false)

        val manager = application.getSystemService(MediaSessionManager::class.java)
        if (manager == null) {
            Log.w(TAG, "MediaSessionManager is unavailable")
            return
        }

        listenerComponent = component
        appContext = application
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
            onSessionsChanged(sessions.orEmpty())
        }
        val registered = runCatching {
            manager.addOnActiveSessionsChangedListener(listener, component, mainHandler)
        }.onFailure {
            Log.w(TAG, "Could not register active session listener", it)
        }.isSuccess
        if (!registered) {
            // Reporting connected here would hide the "connecting / reconnect" state from the
            // lyrics page and make ensureConnected() skip the retry that could still recover.
            listenerComponent = null
            _connected.value = false
            return
        }
        sessionsListener = listener
        _connected.value = true
        rebindJob?.cancel()
        refreshSessions()
        startTicker()
    }

    fun detach() {
        detachInternal(clearState = true)
    }

    /**
     * True when the user granted notification access to this app's listener service.
     *
     * Delegates the `enabled_notification_listeners` parsing to AndroidX and matches on the
     * package level: parsing the stored string by hand misreads entries whose class name is
     * written in short form, which some ROMs do.
     */
    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun openNotificationAccessSettings(context: Context) {
        val component = ComponentName(context, ExternalMediaListenerService::class.java)
        val detailIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                component.flattenToString(),
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = runCatching { context.startActivity(detailIntent) }.isSuccess
        if (!opened) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    /**
     * Asks the system to rebind the listener after the user grants access or a process restart.
     *
     * A single request is not reliable: MIUI/HyperOS rejects the rebind through its auto-start
     * control for a while after a force-stop, so the request is repeated with backoff until the
     * listener attaches or access disappears.
     */
    fun ensureConnected(context: Context) {
        if (!hasNotificationAccess(context)) return
        if (_connected.value) return
        scheduleRebind(context.applicationContext)
    }

    /** Explicit reconnect requested from the lyrics page. */
    fun reconnect(context: Context) {
        if (!hasNotificationAccess(context)) return
        requestRebindInternal(context)
    }

    /**
     * Called by the listener service when the system unbound it. Without this the listener stays
     * unbound after a force-stop, because nothing else asks the system to bind it again.
     */
    fun onSystemUnbound(context: Context) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSystemUnboundRebindAt < SYSTEM_UNBOUND_REBIND_INTERVAL_MS) return
        lastSystemUnboundRebindAt = now
        if (!hasNotificationAccess(context)) return
        scheduleRebind(context.applicationContext)
    }

    private fun scheduleRebind(context: Context) {
        if (rebindJob?.isActive == true) return
        rebindJob = scope.launch {
            for (delayMs in REBIND_RETRY_DELAYS_MS) {
                if (delayMs > 0L) delay(delayMs)
                if (_connected.value) return@launch
                if (!hasNotificationAccess(context)) return@launch
                requestRebindInternal(context)
            }
        }
    }

    private fun requestRebindInternal(context: Context) {
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(context, ExternalMediaListenerService::class.java),
            )
        }.onFailure {
            Log.w(TAG, "Could not request a listener rebind", it)
        }
    }

    fun playPause() {
        val controller = activeController ?: return
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun next() {
        activeController?.transportControls?.skipToNext()
    }

    fun previous() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        activeController?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    }

    private fun refreshSessions() {
        val manager = appContext?.getSystemService(MediaSessionManager::class.java) ?: return
        val component = listenerComponent ?: return
        val sessions = runCatching { manager.getActiveSessions(component) }.getOrElse {
            Log.w(TAG, "Could not read active media sessions", it)
            emptyList()
        }
        onSessionsChanged(sessions)
    }

    private fun onSessionsChanged(sessions: List<MediaController>) {
        val application = appContext ?: return
        Log.d(TAG, "onSessionsChanged: ${sessions.size} session(s)")
        unregisterCallbacks()

        controllers.clear()
        // Keep every foreign session, even when its metadata is still empty. Several players
        // publish the session first and fill in title/artist on a later metadata callback; filtering
        // those sessions out here meant that callback was never registered and the lyrics page
        // stayed on "No media playing".
        controllers += sessions.filter { controller ->
            controller.packageName != application.packageName
        }

        controllers.forEach { controller ->
            val callback = controllerCallback(controller)
            callbacks[controller] = callback
            runCatching { controller.registerCallback(callback, mainHandler) }
        }

        selectActiveController()
    }

    private fun controllerCallback(controller: MediaController): MediaController.Callback {
        return object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                refreshActiveSelection(controller)
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                refreshActiveSelection(controller)
            }

            override fun onSessionDestroyed() {
                refreshSessions()
            }

            private fun refreshActiveSelection(changed: MediaController) {
                val changedIsPlaying = changed.playbackState?.state == PlaybackState.STATE_PLAYING
                if (changedIsPlaying) {
                    activeController = changed
                    publishState()
                } else if (changed == activeController) {
                    selectActiveController()
                } else if (activeController == null) {
                    selectActiveController()
                }
            }
        }
    }

    private fun selectActiveController() {
        val previous = activeController
        val playing = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        activeController = playing
            ?: controllers.firstOrNull { it == previous }
            ?: controllers.firstOrNull { it.metadata?.let(::hasUsefulMetadata) == true }
            ?: controllers.firstOrNull()

        publishState()
    }

    private fun publishState() {
        val controller = activeController ?: run {
            selectedMediaId = null
            _state.value = null
            _positionMs.value = 0L
            return
        }
        val metadata = controller.metadata
        val playback = controller.playbackState
        val application = appContext ?: return
        val packageName = controller.packageName

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: ""
        val mediaId = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
            ?.takeIf { it > 0L }
        val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val artworkUri = metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)

        // Some players only expose a MediaStore id (for example when launched with a raw
        // content:// URI); resolve the real title/artist/album from the media database.
        val enriched = enrichFromMediaStore(application, mediaId, title, artist, album)
        val resolvedTitle = enriched?.first?.takeIf { it.isNotBlank() } ?: title
        val resolvedArtist = enriched?.second?.takeIf { it.isNotBlank() } ?: artist
        val resolvedAlbum = enriched?.third?.takeIf { it.isNotBlank() } ?: album

        if (resolvedTitle.isBlank() && resolvedArtist.isBlank()) {
            selectedMediaId = null
            _state.value = null
            _positionMs.value = 0L
            return
        }

        selectedMediaId = mediaId ?: controller.sessionToken.toString()

        _state.value = ExternalMediaState(
            packageName = packageName,
            appLabel = resolveAppLabel(application, packageName),
            title = resolvedTitle,
            artist = resolvedArtist,
            album = resolvedAlbum,
            durationMs = duration,
            artworkUri = artworkUri,
            artworkBitmap = artwork,
            isPlaying = playback?.state == PlaybackState.STATE_PLAYING,
            playbackSpeed = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            mediaId = selectedMediaId,
        )
        _positionMs.value = resolvePosition(controller, SystemClock.elapsedRealtime())
        Log.d(TAG, "External media: $packageName - $resolvedTitle / $resolvedArtist ($duration ms)")
    }

    /**
     * Resolves MediaStore metadata when a player published only a numeric media id.
     * Returns null when the id is missing, the metadata is already useful, or the row is unknown.
     */
    private fun enrichFromMediaStore(
        context: Context,
        mediaId: String?,
        title: String,
        artist: String,
        album: String,
    ): Triple<String, String, String>? {
        val numericId = mediaId?.toLongOrNull()
            ?: title.takeIf { it.all(Char::isDigit) }?.toLongOrNull()
            ?: return null
        if (artist.isNotBlank() && title.isNotBlank() && !title.all(Char::isDigit)) return null
        return runCatching {
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                numericId,
            )
            context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                Triple(
                    cursor.getString(0).orEmpty(),
                    cursor.getString(1).orEmpty(),
                    cursor.getString(2).orEmpty(),
                )
            }
        }.getOrNull()
    }

    private fun resolvePosition(controller: MediaController, nowElapsedRealtime: Long): Long {
        val playback = controller.playbackState ?: return 0L
        val base = playback.position.coerceAtLeast(0L)
        if (playback.state != PlaybackState.STATE_PLAYING) return base
        if (playback.lastPositionUpdateTime <= 0L) return base
        val sinceUpdate = (nowElapsedRealtime - playback.lastPositionUpdateTime).coerceAtLeast(0L)
        val elapsed = (sinceUpdate * playback.playbackSpeed).toLong()
        val position = base + elapsed
        val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        return if (duration > 0L) position.coerceAtMost(duration) else position
    }

    private fun resolveAppLabel(context: Context, packageName: String): String {
        return runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                val controller = activeController
                if (controller != null && _state.value?.isPlaying == true) {
                    _positionMs.value = resolvePosition(controller, SystemClock.elapsedRealtime())
                }
                delay(POSITION_TICK_MS)
            }
        }
    }

    private fun detachInternal(clearState: Boolean) {
        tickerJob?.cancel()
        tickerJob = null

        val manager = appContext?.getSystemService(MediaSessionManager::class.java)
        sessionsListener?.let { listener ->
            runCatching { manager?.removeOnActiveSessionsChangedListener(listener) }
        }
        sessionsListener = null

        unregisterCallbacks()
        controllers.clear()
        activeController = null
        listenerComponent = null
        _connected.value = false

        if (clearState) {
            _state.value = null
            _positionMs.value = 0L
        }
    }

    private fun unregisterCallbacks() {
        callbacks.forEach { (controller, callback) ->
            runCatching { controller.unregisterCallback(callback) }
        }
        callbacks.clear()
    }

    private fun hasUsefulMetadata(metadata: MediaMetadata): Boolean =
        !metadata.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank() ||
            !metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).isNullOrBlank() ||
            !metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).isNullOrBlank() ||
            !metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).isNullOrBlank() ||
            metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null ||
            metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON) != null
}
