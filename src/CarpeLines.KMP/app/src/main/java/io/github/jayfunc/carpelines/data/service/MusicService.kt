package io.github.jayfunc.carpelines.data.service

import android.app.BackgroundServiceStartNotAllowedException
import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import io.github.jayfunc.carpelines.CarpeLinesApplication
import io.github.jayfunc.carpelines.MainActivity
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.diagnostics.AdvancedPerformanceDiagnostics
import io.github.jayfunc.carpelines.data.model.PlaybackQueueItemSnapshot
import io.github.jayfunc.carpelines.data.model.PlaybackQueueSnapshot
import io.github.jayfunc.carpelines.data.model.Song
import io.github.jayfunc.carpelines.data.preferences.PreviousButtonBehavior
import io.github.jayfunc.carpelines.data.preferences.ThemePreferencesRepository
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.data.repository.MusicRepository
import io.github.jayfunc.carpelines.data.service.player.AudioDecoderPolicy
import io.github.jayfunc.carpelines.data.service.player.DualPlayerEngine
import io.github.jayfunc.carpelines.data.service.player.FadingPlayer
import io.github.jayfunc.carpelines.data.service.player.MappingPlayer
import io.github.jayfunc.carpelines.data.service.player.PreviousButtonPlayer
import io.github.jayfunc.carpelines.data.service.player.TransitionController
import io.github.jayfunc.carpelines.data.service.player.selectCanonicalCloudPlaybackUri
import io.github.jayfunc.carpelines.utils.AlbumArtUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import io.github.jayfunc.carpelines.data.equalizer.ExternalAudioEffectSession
import io.github.jayfunc.carpelines.data.preferences.AlbumArtColorAccuracy
import io.github.jayfunc.carpelines.data.preferences.ThemePreference
import io.github.jayfunc.carpelines.utils.ArtworkTransportSanitizer
import io.github.jayfunc.carpelines.utils.MediaItemBuilder
import io.github.jayfunc.carpelines.data.navidrome.NavidromeRepository
import io.github.jayfunc.carpelines.di.AppScope
import io.github.jayfunc.carpelines.presentation.viewmodel.ListeningStatsTracker
import kotlin.math.abs
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision

import javax.inject.Inject
import androidx.core.net.toUri

/**
 * Task removal must never tear down active playback when the user has opted into
 * background playback; in every other case the service should shut down cleanly
 * instead of lingering half-alive until the system reclaims it.
 */
internal fun shouldContinuePlaybackAfterTaskRemoved(
    hasForegroundPlaybackIntent: Boolean,
    keepPlayingInBackground: Boolean
): Boolean {
    return hasForegroundPlaybackIntent && keepPlayingInBackground
}

/**
 * A start only needs the temporary foreground placeholder when Android demands one:
 * an explicit startForegroundService() fallback (forced extra or a marked pending
 * start), or a media-button start that onCreate has not already promoted while
 * playback is not foreground yet. Plain startService() widget actions never do, so
 * they neither flash the placeholder nor stop the service afterwards.
 */
internal fun requiresTemporaryForegroundForStart(
    forcedForegroundStart: Boolean,
    pendingTemporaryForegroundStart: Boolean,
    isMediaButtonIntent: Boolean,
    startedTemporaryForegroundInOnCreate: Boolean,
    serviceAlreadyForeground: Boolean
): Boolean {
    return forcedForegroundStart ||
        pendingTemporaryForegroundStart ||
        (isMediaButtonIntent &&
            !startedTemporaryForegroundInOnCreate &&
            !serviceAlreadyForeground)
}

@androidx.annotation.OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var engine: DualPlayerEngine
    @Inject
    lateinit var controller: TransitionController
    @Inject
    lateinit var musicRepository: MusicRepository
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository
    @Inject
    lateinit var externalAudioEffectSession: ExternalAudioEffectSession
    @Inject
    lateinit var replayGainManager: io.github.jayfunc.carpelines.data.media.ReplayGainManager
    @Inject
    lateinit var navidromeRepository: NavidromeRepository
    @Inject
    lateinit var listeningStatsTracker: ListeningStatsTracker
    @Inject
    lateinit var sleepTimerSession: SleepTimerSession
    @Inject
    @AppScope
    lateinit var appScope: CoroutineScope

    private var userPlaybackSpeed = 1f

    // ReplayGain volume-normalization state + logic, extracted to a standalone
    // component. Lazily built so the Hilt-injected engine/replayGainManager and the
    // service scope are ready before first use (first playback event).
    private val replayGainProcessor by lazy {
        ReplayGainProcessor(
            engine = engine,
            replayGainManager = replayGainManager,
            scope = serviceScope,
            currentSessionMediaItem = { mediaSession?.player?.currentMediaItem },
        )
    }

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var keepPlayingInBackground = true
    private var isManualShuffleEnabled = false
    private var persistentShuffleEnabled = false
    private var previousButtonBehavior = PreviousButtonBehavior.DEFAULT
    private var previousMainThreadExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private val sleepTimerController by lazy {
        SleepTimerController(
            engine = engine,
            session = sleepTimerSession,
            scope = serviceScope,
            alarmScheduler = AlarmManagerSleepTimerScheduler(this),
            sessionPlayer = { mediaSession?.player ?: engine.masterPlayer },
        )
    }
    private var playbackSnapshotPersistJob: Job? = null
    private var followUpMediaSessionUiRefreshJob: Job? = null
    private var mediaSessionButtonRefreshJob: Job? = null
    private var lastAppliedMediaButtonSignature: String? = null
    private var playbackSnapshotUnloadWriteJob: Job? = null
    private val playbackSnapshotItemCache =
        PlaybackSnapshotItemCache<PlaybackQueueItemSnapshot>()
    private var isRestoringPlaybackSnapshot = false
    private var isPlaybackUnloadInProgress = false
    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private var headsetReconnectCallback: AudioDeviceCallback? = null
    private var shouldResumeAfterHeadsetReconnect = false
    private var lastNoisyPauseRealtimeMs = 0L
    private var resumeOnHeadsetReconnectEnabled = false
    private var temporaryForegroundStartedInOnCreate = false
    private var temporaryForegroundNotificationVisible = false

    companion object {
        private const val TAG = "MusicService_CarpeLines"
        const val NOTIFICATION_ID = 101
        const val ACTION_SLEEP_TIMER_EXPIRED = "io.github.jayfunc.carpelines.ACTION_SLEEP_TIMER_EXPIRED"
        const val EXTRA_FORCE_FOREGROUND_ON_START =
            "io.github.jayfunc.carpelines.extra.FORCE_FOREGROUND_ON_START"
        private const val PLAYBACK_SNAPSHOT_DEBOUNCE_MS = 1500L
        private const val MEDIA_SESSION_BUTTON_DEBOUNCE_MS = 250L
        private const val DEFERRED_SERVICE_STARTUP_WORK_DELAY_MS = 1_000L
        private const val PAUSED_RESTORE_PREPARE_QUEUE_LIMIT = 50
        private val pendingTemporaryForegroundStarts = AtomicInteger(0)

        private const val APP_PACKAGE_PREFIX = "io.github.jayfunc.carpelines"
        private const val DEFAULT_STREAM_BUFFER_SIZE = 8 * 1024
        private const val HEADSET_RECONNECT_RESUME_WINDOW_MS = 15_000L

        fun markPendingTemporaryForegroundStart() {
            pendingTemporaryForegroundStarts.incrementAndGet()
        }

        fun unmarkPendingTemporaryForegroundStart() {
            while (true) {
                val currentCount = pendingTemporaryForegroundStarts.get()
                if (currentCount <= 0) return
                if (pendingTemporaryForegroundStarts.compareAndSet(currentCount, currentCount - 1)) {
                    return
                }
            }
        }

        private fun consumePendingTemporaryForegroundStart(): Boolean {
            while (true) {
                val currentCount = pendingTemporaryForegroundStarts.get()
                if (currentCount <= 0) return false
                if (pendingTemporaryForegroundStarts.compareAndSet(currentCount, currentCount - 1)) {
                    return true
                }
            }
        }
    }

    private val playerSwapListener: (Player) -> Unit = { newPlayer ->
        publishMediaSessionPlayer(newPlayer, "Swapped MediaSession player to new instance.")
        replayGainProcessor.prepareForTransition(newPlayer)
        applyPlaybackSpeed(newPlayer)
    }

    private val transitionDisplayPlayerListener: (Player) -> Unit = { displayPlayer ->
        publishMediaSessionPlayer(
            displayPlayer,
            "Published incoming crossfade player to MediaSession."
        )
        replayGainProcessor.prepareForTransition(displayPlayer)
        applyPlaybackSpeed(displayPlayer)
    }

    private val transitionFinishedListener: () -> Unit = {
        replayGainProcessor.onTransitionFinished()
    }

    private fun Player.unwrapPreviousButtonPlayer(): Player {
        return (this as? PreviousButtonPlayer)?.innerPlayer ?: this
    }

    private fun Player.unwrapMappingPlayer(): Player {
        return (this as? MappingPlayer)?.innerPlayer ?: this
    }

    private fun Player.unwrapFadingPlayer(): Player {
        return (this as? FadingPlayer)?.innerPlayer ?: this
    }

    /** Wraps an engine player for the session; [unwrapSessionPlayer] is its inverse. */
    private fun wrapSessionPlayer(player: Player): Player {
        val fadingPlayer = FadingPlayer(
            innerPlayer = player,
            scope = appScope
        )
        val mappingPlayer = MappingPlayer(
            innerPlayer = fadingPlayer,
            context = this
        )
        return PreviousButtonPlayer(
            innerPlayer = mappingPlayer,
            behaviorProvider = { previousButtonBehavior }
        )
    }

    private fun Player.unwrapSessionPlayer(): Player =
        unwrapPreviousButtonPlayer().unwrapMappingPlayer().unwrapFadingPlayer()

    private fun publishMediaSessionPlayer(player: Player, logMessage: String) {
        val session = mediaSession ?: return
        val oldPlayer = session.player
        val unwrappedOld = oldPlayer.unwrapSessionPlayer()
        if (unwrappedOld !== player) {
            oldPlayer.removeListener(playerListener)
            val wrappedPlayer = wrapSessionPlayer(player)
            session.player = wrappedPlayer
            wrappedPlayer.addListener(playerListener)
        }

        Timber.tag("MusicService").d(logMessage)
        syncLocalListeningStatsFromPlayer(player)
        refreshMediaSessionUi(session)
    }

    private fun syncLocalListeningStatsFromPlayer(
        player: Player = engine.masterPlayer,
        forceNewSession: Boolean = false
    ) {
        val mediaItem = player.currentMediaItem
        val songId = mediaItem?.mediaId?.takeIf { it.isNotBlank() }
        if (songId == null) {
            if (
                player.mediaItemCount == 0 ||
                player.playbackState == Player.STATE_IDLE ||
                player.playbackState == Player.STATE_ENDED
            ) {
                listeningStatsTracker.onPlaybackStopped()
            }
            return
        }

        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val durationMs = player.duration
        val fallbackDurationMs = mediaItem.mediaMetadata.extras
            ?.getLong(MediaItemBuilder.EXTERNAL_EXTRA_DURATION, 0L)
            ?: 0L

        if (forceNewSession) {
            listeningStatsTracker.onTrackChanged(
                songId = songId,
                positionMs = positionMs,
                durationMs = durationMs,
                fallbackDurationMs = fallbackDurationMs,
                isPlaying = player.isPlaying
            )
        } else {
            listeningStatsTracker.ensureSession(
                songId = songId,
                positionMs = positionMs,
                durationMs = durationMs,
                fallbackDurationMs = fallbackDurationMs,
                isPlaying = player.isPlaying
            )
        }
    }

    override fun onCreate() {
        val existingHandler = Thread.currentThread().uncaughtExceptionHandler
        previousMainThreadExceptionHandler = existingHandler
        Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                throwable is ForegroundServiceStartNotAllowedException
            ) {
                Timber.tag(TAG).w(throwable, "Suppressed ForegroundServiceStartNotAllowedException from Media3 internal path")
            } else {
                existingHandler?.uncaughtException(thread, throwable)
            }
        }

        // Marked foreground-service starts (media buttons, and the widget's
        // startForegroundService() fallback) are promoted here, before super.onCreate(),
        // so Hilt injection and MediaSession setup cannot use up Android's FGS deadline.
        // Plain binds and plain startService() starts are never marked, so opening the
        // app does not flash the temporary placeholder notification.
        temporaryForegroundStartedInOnCreate = consumePendingTemporaryForegroundStart()
        if (temporaryForegroundStartedInOnCreate) {
            startTemporaryForegroundForCommand()
        }

        super.onCreate()
        listeningStatsTracker.initialize(appScope)
        
        engine.initialize()
        replayGainProcessor.captureUserVolume(engine.masterPlayer.volume)
        syncLocalListeningStatsFromPlayer(engine.masterPlayer)

        engine.masterPlayer.addListener(playerListener)

        engine.setOnPlayerAboutToBeReleasedListener { oldPlayer ->
            oldPlayer.removeListener(playerListener)
        }
        engine.addPlayerSwapListener(playerSwapListener)
        engine.addTransitionDisplayPlayerListener(transitionDisplayPlayerListener)
        engine.addTransitionFinishedListener(transitionFinishedListener)

        controller.initialize()
        sleepTimerController.attach()
        registerHeadsetReconnectMonitor()

        serviceScope.launch {
            engine.activeAudioSessionId.collect { sessionId ->
                externalAudioEffectSession.open(sessionId)
            }
        }

        serviceScope.launch {
            userPreferencesRepository.keepPlayingInBackgroundFlow.collect { enabled ->
                keepPlayingInBackground = enabled
            }
        }

        serviceScope.launch {
            userPreferencesRepository.audioOutputModeFlow.collect { mode ->
                engine.setAudioOutputMode(mode)
            }
        }

        serviceScope.launch {
            userPreferencesRepository.resumeOnHeadsetReconnectFlow.collect { enabled ->
                resumeOnHeadsetReconnectEnabled = enabled
                if (!enabled) {
                    clearHeadsetReconnectResume()
                }
            }
        }

        serviceScope.launch {
            userPreferencesRepository.persistentShuffleEnabledFlow.collect { enabled ->
                persistentShuffleEnabled = enabled
            }
        }

        serviceScope.launch {
            userPreferencesRepository.previousButtonBehaviorFlow.collect { behavior ->
                previousButtonBehavior = behavior
            }
        }

        serviceScope.launch {
            userPreferencesRepository.replayGainEnabledFlow.collect { enabled ->
                replayGainProcessor.setEnabled(enabled)
                replayGainProcessor.apply(mediaSession?.player?.currentMediaItem)
            }
        }
        serviceScope.launch {
            userPreferencesRepository.replayGainUseAlbumGainFlow.collect { useAlbum ->
                replayGainProcessor.setUseAlbumGain(useAlbum)
                replayGainProcessor.apply(mediaSession?.player?.currentMediaItem)
            }
        }

        serviceScope.launch {
            userPreferencesRepository.playbackSpeedFlow.collect { speed ->
                userPlaybackSpeed = speed
                applyPlaybackSpeed(mediaSession?.player ?: engine.masterPlayer)
            }
        }

        serviceScope.launch {
            val persistent = userPreferencesRepository.persistentShuffleEnabledFlow.first()
            if (persistent) {
                isManualShuffleEnabled = userPreferencesRepository.isShuffleOnFlow.first()
                mediaSession?.let { refreshMediaSessionUi(it) }
            }
        }

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val controllerPackage = controller.packageName
                val hintKeys = controller.connectionHints.keySet().joinToString(",")
                Timber.tag(TAG).d(
                    "onConnect from package=%s uid=%s trusted=%s version=%s hints=[%s]",
                    controllerPackage,
                    controller.uid,
                    controller.isTrusted,
                    controller.controllerVersion,
                    hintKeys
                )
                val defaultResult = super.onConnect(session, controller)
                val sessionCommandsBuilder = SessionCommands.Builder()
                    .addSessionCommands(defaultResult.availableSessionCommands.commands)
                if (isPrivilegedController(controller)) {
                    val customCommands = listOf(
                        MusicNotificationProvider.CUSTOM_COMMAND_CLOSE_PLAYER,
                        MusicNotificationProvider.CUSTOM_COMMAND_TOGGLE_SHUFFLE,
                        MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_ON,
                        MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_OFF,
                        MusicNotificationProvider.CUSTOM_COMMAND_SET_SHUFFLE_STATE,
                        MusicNotificationProvider.CUSTOM_COMMAND_CYCLE_REPEAT_MODE,
                        MusicNotificationProvider.CUSTOM_COMMAND_COUNTED_PLAY,
                        MusicNotificationProvider.CUSTOM_COMMAND_CANCEL_COUNTED_PLAY,
                        MusicNotificationProvider.CUSTOM_COMMAND_SET_SLEEP_TIMER_DURATION,
                        MusicNotificationProvider.CUSTOM_COMMAND_SET_SLEEP_TIMER_END_OF_TRACK,
                        MusicNotificationProvider.CUSTOM_COMMAND_CANCEL_SLEEP_TIMER,
                    ).map { SessionCommand(it, Bundle.EMPTY) }
                    customCommands.forEach { sessionCommandsBuilder.add(it) }
                }
                grantArtworkUriPermissions(
                    controller.packageName,
                    listOfNotNull(session.player.currentMediaItem)
                )

                return MediaSession.ConnectionResult.accept(
                    sessionCommandsBuilder.build(),
                    defaultResult.availablePlayerCommands
                )
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                Timber.tag("MusicService")
                    .d("onCustomCommand received: ${customCommand.customAction}")
                if (!isPrivilegedController(controller)) {
                    Timber.tag(TAG).w(
                        "Rejected custom command %s from untrusted package=%s",
                        customCommand.customAction,
                        controller.packageName
                    )
                    return Futures.immediateFuture(
                        SessionResult(SessionError.ERROR_PERMISSION_DENIED)
                    )
                }
                when (customCommand.customAction) {
                    MusicNotificationProvider.CUSTOM_COMMAND_CLOSE_PLAYER -> {
                        closeNotificationPlayer()
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_COUNTED_PLAY -> {
                        val count = args.getInt("count", 1)
                        sleepTimerController.startCountedPlay(count)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_CANCEL_COUNTED_PLAY -> {
                        sleepTimerController.stopCountedPlay()
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_SET_SLEEP_TIMER_DURATION -> {
                        val minutes = args.getInt(
                            MusicNotificationProvider.EXTRA_SLEEP_TIMER_MINUTES,
                            0
                        )
                        sleepTimerController.setDurationTimer(minutes)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_SET_SLEEP_TIMER_END_OF_TRACK -> {
                        val enabled = args.getBoolean(
                            MusicNotificationProvider.EXTRA_END_OF_TRACK_ENABLED,
                            true
                        )
                        sleepTimerController.setEndOfTrackTimer(enabled)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_CANCEL_SLEEP_TIMER -> {
                        sleepTimerController.cancelTimer()
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_TOGGLE_SHUFFLE -> {
                        val enabled = !isManualShuffleEnabled
                        updateManualShuffleState(session, enabled = enabled, broadcast = true)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_ON -> {
                        Timber.tag("MusicService")
                            .d("Executing SHUFFLE_ON. Current shuffleMode: ${session.player.shuffleModeEnabled}")
                        updateManualShuffleState(session, enabled = true, broadcast = true)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_OFF -> {
                        Timber.tag("MusicService")
                            .d("Executing SHUFFLE_OFF. Current shuffleMode: ${session.player.shuffleModeEnabled}")
                        updateManualShuffleState(session, enabled = false, broadcast = true)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_SET_SHUFFLE_STATE -> {
                        val enabled = args.getBoolean(
                            MusicNotificationProvider.EXTRA_SHUFFLE_ENABLED,
                            false
                        )
                        updateManualShuffleState(session, enabled = enabled, broadcast = false)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_CYCLE_REPEAT_MODE -> {
                        val currentMode = session.player.repeatMode
                        val newMode = when (currentMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                            else -> Player.REPEAT_MODE_OFF
                        }
                        session.player.repeatMode = newMode
                        refreshMediaSessionUi(session)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                return serviceScope.future {
                    resolveMediaItemsByIds(
                        requestedItems = mediaItems,
                        exposeInternalArtwork = controller.packageName == packageName
                    ).also { resolvedItems ->
                        grantArtworkUriPermissions(
                            controller.packageName,
                            resolvedItems.trustedArtworkGrantItems
                        )
                    }.mediaItems
                }
            }

            override fun onSetMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                return serviceScope.future {
                    val resolvedItems = resolveMediaItemsByIds(
                        requestedItems = mediaItems,
                        exposeInternalArtwork = controller.packageName == packageName
                    )
                    grantArtworkUriPermissions(
                        controller.packageName,
                        resolvedItems.trustedArtworkGrantItems
                    )
                    val safeStartIndex = startIndex.coerceIn(
                        0,
                        (resolvedItems.mediaItems.size - 1).coerceAtLeast(0)
                    )
                    MediaSession.MediaItemsWithStartPosition(
                        resolvedItems.mediaItems,
                        safeStartIndex,
                        startPositionMs
                    )
                }
            }
        }

        mediaSession = MediaSession.Builder(this, wrapSessionPlayer(engine.masterPlayer))
            .setCallback(callback)
            .setSessionActivity(getOpenAppPendingIntent())
            .setBitmapLoader(CoilBitmapLoader(this, serviceScope))
            .build()

        val localOnlyProvider = LocalOnlyMediaNotificationProvider(this).also {
            it.setSmallIcon(R.drawable.monochrome_player)
        }
        setMediaNotificationProvider(localOnlyProvider)
        if (temporaryForegroundStartedInOnCreate) {
            serviceScope.launch {
                delay(2_000L)
                if (mediaSession?.player?.hasForegroundPlaybackIntent() != true) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    clearTemporaryForegroundNotification()
                }
            }
        }
        serviceScope.launch {
            restorePlaybackQueueSnapshotIfNeeded()
            mediaSession?.let { refreshMediaSessionUi(it) }
        }

    }

    private fun startTemporaryForegroundForCommand() {
        val notification = NotificationCompat.Builder(
            this,
            CarpeLinesApplication.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.monochrome_player)
            .setContentTitle(getString(R.string.app_name))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(getOpenAppPendingIntent())
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .build()
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
            temporaryForegroundNotificationVisible = true
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to promote service to foreground for external command")
        }
    }

    /**
     * Media3's notification provider posts under its own id, so promoting the real
     * media notification does not replace the temporary placeholder — without an
     * explicit cancel the placeholder stays behind forever once the session
     * notification takes over the foreground.
     */
    private fun clearTemporaryForegroundNotification() {
        if (!temporaryForegroundNotificationVisible) return
        temporaryForegroundNotificationVisible = false
        runCatching {
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        }.onFailure { e ->
            Timber.tag(TAG).w(e, "Failed to cancel temporary foreground notification")
        }
    }

    private fun isServiceAlreadyForeground(): Boolean {
        val player = mediaSession?.player ?: return false
        return player.hasForegroundPlaybackIntent()
    }

    private fun Player.hasForegroundPlaybackIntent(): Boolean {
        return playWhenReady &&
            mediaItemCount > 0 &&
            playbackState != Player.STATE_IDLE &&
            playbackState != Player.STATE_ENDED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startedTemporaryForegroundInOnCreate = temporaryForegroundStartedInOnCreate
        temporaryForegroundStartedInOnCreate = false
        val pendingTemporaryForegroundStart = consumePendingTemporaryForegroundStart()
        val forcedForegroundStart =
            intent?.getBooleanExtra(EXTRA_FORCE_FOREGROUND_ON_START, false) == true
        val isMediaButtonIntent = intent?.action == Intent.ACTION_MEDIA_BUTTON
        val needsTemporaryForeground = requiresTemporaryForegroundForStart(
            forcedForegroundStart = forcedForegroundStart,
            pendingTemporaryForegroundStart = pendingTemporaryForegroundStart,
            isMediaButtonIntent = isMediaButtonIntent,
            startedTemporaryForegroundInOnCreate = startedTemporaryForegroundInOnCreate,
            serviceAlreadyForeground = isServiceAlreadyForeground()
        )
        if (needsTemporaryForeground && !startedTemporaryForegroundInOnCreate) {
            startTemporaryForegroundForCommand()
        }

        intent?.action?.let { action ->
            Timber.tag(TAG).d("onStartCommand widget action: %s", action)
            val player = mediaSession?.player ?: engine.masterPlayer
            when (action) {
                ACTION_SLEEP_TIMER_EXPIRED -> {
                    Timber.tag(TAG).d("Sleep timer backstop alarm received.")
                    sleepTimerController.onDurationExpired()
                }
            }
        }
        val startCommandResult = try {
            super.onStartCommand(intent, flags, startId)
        } catch (e: IllegalStateException) {
            // Media3 promotes to foreground before stopping itself on a stale self-start
            // intent (e.g. after the queue empties); Android may refuse that from background.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                e !is ForegroundServiceStartNotAllowedException
            ) throw e
            Timber.tag(TAG).w(e, "Media3 start command could not go foreground; stopping")
            clearTemporaryForegroundNotification()
            stopSelf()
            return START_NOT_STICKY
        }
        if (needsTemporaryForeground || startedTemporaryForegroundInOnCreate) {
            if (mediaSession?.player?.hasForegroundPlaybackIntent() != true) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                clearTemporaryForegroundNotification()
                if (needsTemporaryForeground) {
                    stopSelfResult(startId)
                }
            }
        }
        return startCommandResult
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val session = mediaSession
        val continuePlayback = shouldContinuePlaybackAfterTaskRemoved(
            hasForegroundPlaybackIntent = session?.player?.hasForegroundPlaybackIntent() == true,
            keepPlayingInBackground = keepPlayingInBackground
        )
        if (continuePlayback && session != null) {
            // Media3's default onTaskRemoved pauses all players and stops the service
            // whenever it is not in foreground state, and OEMs freeze cached processes
            // that have no foreground service — either way playback dies with the task.
            if (!isPlaybackOngoing()) {
                Timber.tag(TAG).w(
                    "Task removed while playing without foreground state; re-promoting."
                )
                onUpdateNotification(session, startInForegroundRequired = true)
            }
            return
        }
        stopPlaybackAndUnload(reason = "task_removed")
    }

    override fun onDestroy() {
        PlaybackActivityTracker.setPlaybackActive(false)
        io.github.jayfunc.carpelines.data.provider.SharedArtworkContentProvider.clearPublishedSessionArtwork()
        listeningStatsTracker.finalizeCurrentSession(forceSynchronousPersistence = true)
        reportNavidromePlayback("stopped")
        stopNavidromePlaybackReporting()
        flushPendingPlaybackSnapshotOnDestroy()
        playbackSnapshotPersistJob?.cancel()
        mediaSessionButtonRefreshJob?.cancel()
        followUpMediaSessionUiRefreshJob?.cancel()
        unregisterHeadsetReconnectMonitor()
        replayGainProcessor.cancel()

        sleepTimerController.release()
        engine.removePlayerSwapListener(playerSwapListener)
        engine.removeTransitionDisplayPlayerListener(transitionDisplayPlayerListener)
        engine.removeTransitionFinishedListener(transitionFinishedListener)
        engine.setOnPlayerAboutToBeReleasedListener {}
        mediaSession?.player?.removeListener(playerListener)
        engine.masterPlayer.removeListener(playerListener)

        mediaSession?.run {
            release()
            mediaSession = null
        }
        externalAudioEffectSession.close()
        engine.release()
        controller.release()
        serviceScope.cancel()
        Thread.currentThread().setUncaughtExceptionHandler(previousMainThreadExceptionHandler)
        previousMainThreadExceptionHandler = null
        super.onDestroy()
    }

    private fun getNavidromeId(mediaItem: MediaItem?): String? {
        if (mediaItem == null) return null
        return mediaItem.mediaMetadata.extras?.getString(MediaItemBuilder.EXTERNAL_EXTRA_NAVIDROME_ID)
            ?: mediaItem.mediaId.let { if (it.startsWith("navidrome_")) it.substringAfter("navidrome_") else null }
            ?: mediaItem.mediaMetadata.extras?.getString(MediaItemBuilder.EXTERNAL_EXTRA_CONTENT_URI)?.let {
                if (it.startsWith("navidrome://")) it.substringAfter("navidrome://") else null
            }
    }

    private fun isNavidromeMediaItem(mediaItem: MediaItem?): Boolean {
        return getNavidromeId(mediaItem) != null
    }

    private fun reportNavidromePlayback(state: String, mediaItem: MediaItem? = engine.masterPlayer.currentMediaItem) {
        val player = engine.masterPlayer
        val targetItem = mediaItem ?: return
        val navidromeId = getNavidromeId(targetItem) ?: return

        val positionMs = if (targetItem === player.currentMediaItem) {
            player.currentPosition
        } else {
            targetItem.mediaMetadata.extras?.getLong(MediaItemBuilder.EXTERNAL_EXTRA_DURATION) ?: 0L
        }
        val playbackRate = player.playbackParameters.speed

        appScope.launch(Dispatchers.IO) {
            navidromeRepository.reportPlayback(
                navidromeId = navidromeId,
                positionMs = positionMs,
                state = state,
                playbackRate = playbackRate
            )
        }
    }

    private var navidromePlaybackReportJob: Job? = null

    private fun startNavidromePlaybackReporting() {
        navidromePlaybackReportJob?.cancel()
        navidromePlaybackReportJob = serviceScope.launch {
            while (true) {
                delay(30_000)
                val player = engine.masterPlayer
                if (player.isPlaying && isNavidromeMediaItem(player.currentMediaItem)) {
                    reportNavidromePlayback("playing")
                }
            }
        }
    }

    private fun stopNavidromePlaybackReporting() {
        navidromePlaybackReportJob?.cancel()
        navidromePlaybackReportJob = null
    }

    private var consecutivePlaybackErrors = 0
    private val maxConsecutivePlaybackErrors = 5

    private val playerListener = object : Player.Listener {
        override fun onVolumeChanged(volume: Float) {
            replayGainProcessor.onPlayerVolumeChanged(volume)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val player = mediaSession?.player ?: engine.masterPlayer
            Timber.tag(TAG).d("onIsPlayingChanged: $isPlaying. Duration: ${player.duration}, Seekable: ${player.isCurrentMediaItemSeekable}")
            PlaybackActivityTracker.setPlaybackActive(isPlaying)
            syncLocalListeningStatsFromPlayer(player)

            if (isPlaying) {
                reportNavidromePlayback("playing")
                startNavidromePlaybackReporting()
            } else {
                val state = if (player.playbackState == Player.STATE_ENDED) "stopped" else "paused"
                reportNavidromePlayback(state)
                stopNavidromePlaybackReporting()
            }

            if (isPlaying) {
                replayGainProcessor.reapplyLastAppliedVolume(player)
            }
            mediaSession?.let { refreshMediaSessionUi(it) }
            schedulePlaybackSnapshotPersist()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            when {
                playWhenReady -> clearHeadsetReconnectResume()
                !resumeOnHeadsetReconnectEnabled -> clearHeadsetReconnectResume()
                reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> {
                    shouldResumeAfterHeadsetReconnect = true
                    lastNoisyPauseRealtimeMs = SystemClock.elapsedRealtime()
                    Timber.tag(TAG).d("Marked playback for headset reconnect resume")
                }
                else -> clearHeadsetReconnectResume()
            }
            mediaSession?.let { refreshMediaSessionUi(it) }
            schedulePlaybackSnapshotPersist()
        }

        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
            val canSeek = availableCommands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            val player = engine.masterPlayer
            Timber.tag(TAG).w("onAvailableCommandsChanged. Can Seek Command? $canSeek. IsSeekable? ${player.isCurrentMediaItemSeekable}. Duration: ${player.duration}")
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            Timber.tag(TAG).d("Playback state changed: $playbackState")
            if (playbackState == Player.STATE_READY) {
                consecutivePlaybackErrors = 0
            }
            if (playbackState == Player.STATE_ENDED) {
                listeningStatsTracker.finalizeCurrentSession()
                val mediaItem = (mediaSession?.player ?: engine.masterPlayer).currentMediaItem
                getNavidromeId(mediaItem)?.let { navidromeId ->
                    appScope.launch(Dispatchers.IO) {
                        navidromeRepository.scrobble(navidromeId, submission = true)
                    }
                }

                sleepTimerController.onPlaybackEnded()
                reportNavidromePlayback("stopped")
                stopNavidromePlaybackReporting()
            } else {
                syncLocalListeningStatsFromPlayer(mediaSession?.player ?: engine.masterPlayer)
            }
            mediaSession?.let { refreshMediaSessionUi(it) }
            schedulePlaybackSnapshotPersist(immediate = playbackState == Player.STATE_IDLE)
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            // Source timeline updates (for example, a newly prepared next item) do not change
            // the queue metadata persisted here. Rebuilding hundreds of items for those updates
            // puts the same work back on the next/previous transition hot path.
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                playbackSnapshotItemCache.invalidate()
            }
            schedulePlaybackSnapshotPersist(immediate = timeline.isEmpty)
            val player = engine.masterPlayer
            val nextIndex = player.nextMediaItemIndex
            if (nextIndex != androidx.media3.common.C.INDEX_UNSET) {
                runCatching { replayGainProcessor.prefetch(player.getMediaItemAt(nextIndex)) }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                val state = if (engine.masterPlayer.isPlaying) "playing" else "paused"
                reportNavidromePlayback(state)
            }

            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                val finishedItem = oldPosition.mediaItem
                if (isNavidromeMediaItem(finishedItem)) {
                    val prevId = getNavidromeId(finishedItem)
                    reportNavidromePlayback("stopped", finishedItem)
                    if (prevId != null) {
                        appScope.launch(Dispatchers.IO) {
                            navidromeRepository.scrobble(prevId, submission = true)
                        }
                    }
                }
            }

            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION ||
                reason == Player.DISCONTINUITY_REASON_SEEK
            ) {
                val currentItem = mediaSession?.player?.currentMediaItem
                val oldMediaId = oldPosition.mediaItem?.mediaId
                val newMediaId = newPosition.mediaItem?.mediaId
                if (oldMediaId != null && oldMediaId == newMediaId) {
                    replayGainProcessor.reapplyLastAppliedVolume(engine.masterPlayer)
                } else {
                    replayGainProcessor.apply(currentItem)
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            unsupportedFormatWarnedMediaId = null
            AdvancedPerformanceDiagnostics.recordEventIfEnabled(
                type = AdvancedPerformanceDiagnostics.EventTypes.PLAYBACK,
                name = "media_item_transition",
            ) {
                mapOf("reason" to reason.toString())
            }
            mediaItem?.let(::grantArtworkUriPermissionsToConnectedControllers)
            syncLocalListeningStatsFromPlayer(mediaSession?.player ?: engine.masterPlayer, forceNewSession = true)
            if (isNavidromeMediaItem(mediaItem)) {
                reportNavidromePlayback("starting")
                if (engine.masterPlayer.isPlaying) {
                    startNavidromePlaybackReporting()
                }
            } else {
                stopNavidromePlaybackReporting()
            }

            sleepTimerController.onMediaItemTransition(mediaItem, reason)
            replayGainProcessor.apply(mediaSession?.player?.currentMediaItem)
            val player = engine.masterPlayer
            val nextIndex = player.nextMediaItemIndex
            if (nextIndex != androidx.media3.common.C.INDEX_UNSET) {
                runCatching { replayGainProcessor.prefetch(player.getMediaItemAt(nextIndex)) }
            }
            mediaSession?.let { refreshMediaSessionUi(it) }
            schedulePlaybackSnapshotPersist()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            mediaSession?.let { refreshMediaSessionUiWithFollowUp(it) }
            val activePlayer = mediaSession?.player ?: engine.masterPlayer
            replayGainProcessor.onMediaMetadataChanged(activePlayer.currentMediaItem)
        }

        override fun onTracksChanged(tracks: Tracks) {
            warnIfNoDecodableAudioTrack(tracks)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            Timber.tag("MusicService")
                .d("playerListener.onShuffleModeEnabledChanged: $shuffleModeEnabled")
            mediaSession?.let { refreshMediaSessionUi(it) }
            schedulePlaybackSnapshotPersist()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            sleepTimerController.onRepeatModeChanged(repeatMode)
            mediaSession?.let { refreshMediaSessionUi(it) }
            schedulePlaybackSnapshotPersist()
        }

        override fun onPlayerError(error: PlaybackException) {
            val player = mediaSession?.player ?: engine.masterPlayer
            Timber.tag(TAG).e(error, "Player error on item %s", player.currentMediaItem?.mediaId)
            AdvancedPerformanceDiagnostics.recordEventIfEnabled(
                type = AdvancedPerformanceDiagnostics.EventTypes.PLAYBACK,
                name = "player_error"
            ) {
                mapOf(
                    "code" to error.errorCodeName,
                    "message" to (error.message ?: error.javaClass.simpleName)
                )
            }
            if (player.hasNextMediaItem() && consecutivePlaybackErrors < maxConsecutivePlaybackErrors) {
                consecutivePlaybackErrors++
                player.seekToNextMediaItem()
                player.prepare()
            } else {
                consecutivePlaybackErrors = 0
            }
        }
    }

    /** MediaId already reported by [warnIfNoDecodableAudioTrack], to avoid repeat toasts. */
    private var unsupportedFormatWarnedMediaId: String? = null

    /**
     * Tells the user when no renderer (platform or FFmpeg) can decode the current item's audio.
     * The engine's [io.github.jayfunc.carpelines.data.service.player.UnsupportedAudioListener]
     * pauses the player so ExoPlayer does not advance a silent clock.
     */
    private fun warnIfNoDecodableAudioTrack(tracks: Tracks) {
        if (!AudioDecoderPolicy.hasUnsupportedAudio(tracks)) return
        val mediaItem = (mediaSession?.player ?: engine.masterPlayer).currentMediaItem ?: return
        // onTracksChanged fires repeatedly for the same item; warn once per track.
        if (unsupportedFormatWarnedMediaId == mediaItem.mediaId) return
        unsupportedFormatWarnedMediaId = mediaItem.mediaId

        val format = tracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO }
            .firstNotNullOfOrNull { group ->
                (0 until group.length).firstNotNullOfOrNull { group.getTrackFormat(it).sampleMimeType }
            }
        val trackTitle = mediaItem.mediaMetadata.title?.toString()?.takeIf(String::isNotBlank)
            ?: getString(R.string.unknown_song_title)
        Timber.tag(TAG).e(
            "No renderer can decode the audio track of '%s' (format=%s); playback paused",
            trackTitle, format
        )
        Toast.makeText(
            this,
            getString(
                R.string.player_unsupported_format,
                trackTitle,
                format ?: getString(R.string.player_unsupported_format_unknown)
            ),
            Toast.LENGTH_LONG
        ).show()
    }

    /** Applies the user's playback speed to [player], preserving pitch (time-stretch). */
    private fun applyPlaybackSpeed(player: Player) {
        if (abs(player.playbackParameters.speed - userPlaybackSpeed) > 0.001f) {
            player.setPlaybackSpeed(userPlaybackSpeed)
        }
    }

    private fun registerHeadsetReconnectMonitor() {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                if (!addedDevices.any(::isReconnectableHeadsetOutput)) return
                maybeResumeAfterHeadsetReconnect()
            }
        }

        audioManager.registerAudioDeviceCallback(callback, null)
        headsetReconnectCallback = callback
    }

    private fun unregisterHeadsetReconnectMonitor() {
        headsetReconnectCallback?.let { callback ->
            runCatching { audioManager.unregisterAudioDeviceCallback(callback) }
        }
        headsetReconnectCallback = null
        clearHeadsetReconnectResume()
    }

    private fun maybeResumeAfterHeadsetReconnect() {
        if (!resumeOnHeadsetReconnectEnabled || !shouldResumeAfterHeadsetReconnect) return

        val elapsedSinceNoisyPause = SystemClock.elapsedRealtime() - lastNoisyPauseRealtimeMs
        if (elapsedSinceNoisyPause > HEADSET_RECONNECT_RESUME_WINDOW_MS) {
            clearHeadsetReconnectResume()
            return
        }

        if (!hasReconnectableHeadsetOutput()) {
            return
        }

        val player = engine.masterPlayer
        if (
            player.currentMediaItem == null ||
            player.playWhenReady ||
            player.playbackState == Player.STATE_IDLE ||
            player.playbackState == Player.STATE_ENDED
        ) {
            clearHeadsetReconnectResume()
            return
        }

        Timber.tag(TAG).d("Resuming playback after headset reconnect")
        clearHeadsetReconnectResume()
        player.play()
    }

    private fun hasReconnectableHeadsetOutput(): Boolean {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any(::isReconnectableHeadsetOutput)
    }

    private fun isReconnectableHeadsetOutput(device: AudioDeviceInfo): Boolean {
        return when (device.type) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> true
            else -> false
        }
    }

    private fun clearHeadsetReconnectResume() {
        shouldResumeAfterHeadsetReconnect = false
        lastNoisyPauseRealtimeMs = 0L
    }

    private fun schedulePlaybackSnapshotPersist(immediate: Boolean = false) {
        if (isPlaybackUnloadInProgress) {
            return
        }
        playbackSnapshotPersistJob?.cancel()
        playbackSnapshotPersistJob = serviceScope.launch {
            if (!immediate) {
                delay(PLAYBACK_SNAPSHOT_DEBOUNCE_MS)
            }
            persistPlaybackSnapshot()
        }
    }

    private suspend fun persistPlaybackSnapshot(playWhenReadyOverride: Boolean? = null) {
        if (isRestoringPlaybackSnapshot) return
        val snapshot = capturePlaybackSnapshot(playWhenReadyOverride)
        runCatching {
            userPreferencesRepository.setPlaybackQueueSnapshot(snapshot)
        }.onFailure { e ->
            Timber.tag(TAG).w(e, "Failed to persist playback snapshot")
        }
    }

    private suspend fun capturePlaybackSnapshot(playWhenReadyOverride: Boolean? = null): PlaybackQueueSnapshot? =
        withContext(Dispatchers.Main.immediate) {
            capturePlaybackSnapshotFromPlayer(playWhenReadyOverride)
        }

    private fun capturePlaybackSnapshotFromPlayer(
        playWhenReadyOverride: Boolean? = null
    ): PlaybackQueueSnapshot? {
        val player = engine.masterPlayer
        // A crossfade prepares the incoming player with only a window of the queue. If we
        // persist while that player is master, its timeline is not the queue; persisting it
        // would shrink the restored queue to the window. Use the engine's full queue instead.
        val isWindowed = engine.isUsingWindowedQueue()
        val fullQueue = if (isWindowed) engine.getFullQueue() else null
        val mediaItemCount = fullQueue?.size ?: player.mediaItemCount
        if (mediaItemCount <= 0) {
            return null
        }

        val snapshotItems = playbackSnapshotItemCache.getOrBuild {
            buildPlaybackSnapshotItems(mediaItemCount) { index ->
                fullQueue?.get(index) ?: player.getMediaItemAt(index)
            }
        }

        if (snapshotItems.isEmpty()) {
            return null
        }

        val currentMediaId = player.currentMediaItem?.mediaId
        val indexFromMediaId = currentMediaId
            ?.let { id -> snapshotItems.indexOfFirst { it.mediaId == id } }
            ?.takeIf { it >= 0 }

        // player.currentMediaItemIndex is window-relative while a crossfade window is in
        // place, so only trust it when the timeline really is the whole queue.
        val playerIndex = if (isWindowed) engine.getCurrentAbsoluteIndex() else player.currentMediaItemIndex
        val safeCurrentIndex = when {
            indexFromMediaId != null -> indexFromMediaId
            playerIndex in snapshotItems.indices -> playerIndex
            else -> 0
        }

        val safeRepeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF,
            Player.REPEAT_MODE_ONE,
            Player.REPEAT_MODE_ALL -> player.repeatMode
            else -> Player.REPEAT_MODE_OFF
        }

        return PlaybackQueueSnapshot(
            items = snapshotItems,
            currentMediaId = currentMediaId,
            currentIndex = safeCurrentIndex,
            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
            playWhenReady = playWhenReadyOverride ?: player.playWhenReady,
            repeatMode = safeRepeatMode,
            shuffleEnabled = isManualShuffleEnabled,
        )
    }

    private fun buildPlaybackSnapshotItems(
        mediaItemCount: Int,
        mediaItemAt: (Int) -> MediaItem,
    ): List<PlaybackQueueItemSnapshot> {
        val snapshotItems = ArrayList<PlaybackQueueItemSnapshot>(mediaItemCount)
        for (index in 0 until mediaItemCount) {
            val mediaItem = mediaItemAt(index)
            val metadata = mediaItem.mediaMetadata
            val playerUri = mediaItem.localConfiguration?.uri?.toString()
            val originalContentUri = metadata.extras
                ?.getString(MediaItemBuilder.EXTERNAL_EXTRA_CONTENT_URI)
            // Persist the provider URI for cloud items. Proxy ports die with the process and an
            // app-private offline file can be removed while this item remains in the queue.
            val uri = selectCanonicalCloudPlaybackUri(playerUri, originalContentUri)
            if (mediaItem.mediaId.isBlank() || uri.isNullOrBlank()) continue

            snapshotItems += PlaybackQueueItemSnapshot(
                mediaId = mediaItem.mediaId,
                uri = uri,
                title = metadata.title?.toString(),
                artist = metadata.artist?.toString(),
                albumTitle = metadata.albumTitle?.toString(),
                artworkUri = resolveStoredArtworkUriString(metadata),
                durationMs = metadata.extras
                    ?.getLong(MediaItemBuilder.EXTERNAL_EXTRA_DURATION)
                    ?.takeIf { it > 0L },
            )
        }
        return snapshotItems
    }

    private suspend fun restorePlaybackQueueSnapshotIfNeeded() {
        val alreadyHasQueue = withContext(Dispatchers.Main.immediate) {
            engine.masterPlayer.mediaItemCount > 0
        }
        if (alreadyHasQueue) return

        val snapshot = runCatching {
            userPreferencesRepository.getPlaybackQueueSnapshotOnce()
        }.getOrNull() ?: return

        if (snapshot.items.isEmpty()) {
            return
        }

        val allowBackgroundPlayback = runCatching {
            userPreferencesRepository.keepPlayingInBackgroundFlow.first()
        }.getOrDefault(keepPlayingInBackground)
        val shouldRestorePlaying = snapshot.playWhenReady && allowBackgroundPlayback

        val restoredItems = snapshot.items.mapNotNull(::buildMediaItemFromSnapshot)
        if (restoredItems.isEmpty()) {
            userPreferencesRepository.setPlaybackQueueSnapshot(null)
            return
        }

        val resolvedIndex = when {
            snapshot.currentIndex in restoredItems.indices -> snapshot.currentIndex
            !snapshot.currentMediaId.isNullOrBlank() -> {
                restoredItems.indexOfFirst { it.mediaId == snapshot.currentMediaId }
                    .takeIf { it >= 0 } ?: 0
            }
            else -> 0
        }

        val preparedItems = restoredItems.toMutableList()
        preparedItems.getOrNull(resolvedIndex)?.let { currentItem ->
            val resolvedCurrentItem = runCatching { engine.resolveMediaItem(currentItem) }.getOrNull()
            if (resolvedCurrentItem != null && resolvedCurrentItem != currentItem) {
                preparedItems[resolvedIndex] = resolvedCurrentItem
            }
        }

        withContext(Dispatchers.Main.immediate) {
            val player = engine.masterPlayer
            if (player.mediaItemCount > 0) {
                return@withContext
            }

            val safeRepeatMode = when (snapshot.repeatMode) {
                Player.REPEAT_MODE_OFF,
                Player.REPEAT_MODE_ONE,
                Player.REPEAT_MODE_ALL -> snapshot.repeatMode
                else -> Player.REPEAT_MODE_OFF
            }

            isRestoringPlaybackSnapshot = true
            try {
                player.setMediaItems(
                    preparedItems,
                    resolvedIndex,
                    snapshot.currentPositionMs.coerceAtLeast(0L)
                )
                if (shouldRestorePlaying || preparedItems.size <= PAUSED_RESTORE_PREPARE_QUEUE_LIMIT) {
                    player.prepare()
                }
                player.repeatMode = safeRepeatMode
                player.shuffleModeEnabled = false
                isManualShuffleEnabled = snapshot.shuffleEnabled
                if (shouldRestorePlaying) {
                    player.playWhenReady = true
                } else {
                    player.playWhenReady = false
                }
            } finally {
                isRestoringPlaybackSnapshot = false
            }
        }

        Timber.tag(TAG).i(
            "Restored playback snapshot: items=%d index=%d playWhenReady=%s",
            restoredItems.size,
            snapshot.currentIndex,
            shouldRestorePlaying
        )
        schedulePlaybackSnapshotPersist(immediate = true)
    }

    private fun buildMediaItemFromSnapshot(snapshotItem: PlaybackQueueItemSnapshot): MediaItem? {
        if (snapshotItem.mediaId.isBlank() || snapshotItem.uri.isBlank()) {
            return null
        }

        val metadataBuilder = MediaMetadata.Builder()
        snapshotItem.title?.takeIf { it.isNotBlank() }?.let { metadataBuilder.setTitle(it) }
        snapshotItem.artist?.takeIf { it.isNotBlank() }?.let { metadataBuilder.setArtist(it) }
        snapshotItem.albumTitle?.takeIf { it.isNotBlank() }?.let { metadataBuilder.setAlbumTitle(it) }
        val restoredArtworkUri = MediaItemBuilder.externalControllerArtworkUri(
            context = this,
            rawArtworkUri = snapshotItem.artworkUri
        )
        restoredArtworkUri?.let { metadataBuilder.setArtworkUri(it) }

        val extras = Bundle().apply {
            putBoolean(
                MediaItemBuilder.EXTERNAL_EXTRA_FLAG,
                snapshotItem.mediaId.startsWith("external:")
            )
            putString(MediaItemBuilder.EXTERNAL_EXTRA_CONTENT_URI, snapshotItem.uri)
            snapshotItem.albumTitle?.takeIf { it.isNotBlank() }?.let {
                putString(MediaItemBuilder.EXTERNAL_EXTRA_ALBUM, it)
            }
            (restoredArtworkUri?.toString() ?: snapshotItem.artworkUri)
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    putString(MediaItemBuilder.EXTERNAL_EXTRA_ALBUM_ART, it)
                }
            snapshotItem.durationMs?.takeIf { it > 0L }?.let {
                putLong(MediaItemBuilder.EXTERNAL_EXTRA_DURATION, it)
            }
        }
        metadataBuilder.setExtras(extras)

        return MediaItem.Builder()
            .setMediaId(snapshotItem.mediaId)
            .setUri(MediaItemBuilder.playbackUri(snapshotItem.uri))
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun getOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            setPackage(packageName)
            action = "io.github.jayfunc.carpelines.action.OPEN_PLAYER"
            addCategory(Intent.CATEGORY_DEFAULT)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ACTION_SHOW_PLAYER", true)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun resolveStoredArtworkUriString(metadata: MediaMetadata?): String? {
        metadata ?: return null
        return metadata.extras
            ?.getString(MediaItemBuilder.EXTERNAL_EXTRA_ALBUM_ART)
            ?.takeIf { it.isNotBlank() }
            ?: metadata.artworkUri
                ?.toString()
                ?.takeIf { it.isNotBlank() }
    }

    private fun parseArtworkUriString(rawArtworkUri: String?): Uri? {
        if (rawArtworkUri.isNullOrBlank()) return null
        return MediaItemBuilder.artworkUri(rawArtworkUri)
            ?: if (rawArtworkUri.startsWith("/")) {
                Uri.fromFile(java.io.File(rawArtworkUri))
            } else {
                runCatching { rawArtworkUri.toUri() }.getOrNull()
            }
    }

    private fun resolveArtworkUri(metadata: MediaMetadata?): Uri? {
        metadata ?: return null
        metadata.artworkUri?.let { return it }
        val extrasUri = metadata.extras
            ?.getString(MediaItemBuilder.EXTERNAL_EXTRA_ALBUM_ART)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return parseArtworkUriString(extrasUri)
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        val hasPlaybackIntent = session.player.hasForegroundPlaybackIntent()

        val shouldStartInForeground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startInForegroundRequired || hasPlaybackIntent
        } else {
            startInForegroundRequired
        }

        try {
            super.onUpdateNotification(session, shouldStartInForeground)
            if (session.player.mediaItemCount > 0) {
                clearTemporaryForegroundNotification()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "onUpdateNotification suppressed: ${e.message}")
        }
    }

    override fun startForegroundService(serviceIntent: Intent?): ComponentName? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return try {
                super.startForegroundService(serviceIntent)
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Timber.tag(TAG).w(
                    e,
                    "startForegroundService not allowed; ignoring redundant self-start request"
                )
                serviceIntent?.component ?: ComponentName(this, javaClass)
            } catch (e: BackgroundServiceStartNotAllowedException) {
                Timber.tag(TAG).w(
                    e,
                    "startForegroundService blocked (app in background); ignoring self-start request"
                )
                serviceIntent?.component ?: ComponentName(this, javaClass)
            }
        }
        return super.startForegroundService(serviceIntent)
    }

    private fun refreshMediaSessionUi(session: MediaSession, force: Boolean = false) {
        val pendingSignature = buildMediaButtonPreferencesSignature(session)
        if (!force && pendingSignature == lastAppliedMediaButtonSignature) {
            return
        }

        mediaSessionButtonRefreshJob?.cancel()
        mediaSessionButtonRefreshJob = serviceScope.launch {
            if (!force) {
                delay(MEDIA_SESSION_BUTTON_DEBOUNCE_MS)
            }
            if (mediaSession !== session) {
                return@launch
            }

            val latestSignature = buildMediaButtonPreferencesSignature(session)
            if (latestSignature == lastAppliedMediaButtonSignature) {
                return@launch
            }

            val buttons = buildMediaButtonPreferences(session)
            session.setMediaButtonPreferences(buttons)
            lastAppliedMediaButtonSignature = latestSignature
        }
    }

    private fun closeNotificationPlayer() {
        stopPlaybackAndUnload(
            reason = "notification_close_button",
            preservePlaybackSnapshot = false
        )
    }

    private fun stopPlaybackAndUnload(
        reason: String,
        preservePlaybackSnapshot: Boolean = true,
    ) {
        Timber.tag(TAG).d(
            "Stopping playback and unloading service. reason=%s",
            reason
        )
        isPlaybackUnloadInProgress = true
        followUpMediaSessionUiRefreshJob?.cancel()
        mediaSessionButtonRefreshJob?.cancel()
        playbackSnapshotPersistJob?.cancel()

        val sessionToRelease = mediaSession
        val player = sessionToRelease?.player ?: engine.masterPlayer

        clearHeadsetReconnectResume()
        sleepTimerController.cancelTimer()
        sleepTimerController.stopCountedPlay(restoreRepeatMode = false)

        if (preservePlaybackSnapshot) {
            persistPlaybackSnapshotOnUnload()
        } else {
            clearPlaybackSnapshotOnUnload()
        }

        listeningStatsTracker.finalizeCurrentSession(forceSynchronousPersistence = true)

        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()

        stopForeground(STOP_FOREGROUND_REMOVE)
        clearTemporaryForegroundNotification()

        stopSelf()
    }

    private fun persistPlaybackSnapshotOnUnload() {
        val snapshot = capturePlaybackSnapshotFromPlayer(playWhenReadyOverride = false)
        writePlaybackSnapshotOnUnload(snapshot)
    }

    private fun clearPlaybackSnapshotOnUnload() {
        writePlaybackSnapshotOnUnload(null)
    }

    private fun writePlaybackSnapshotOnUnload(snapshot: PlaybackQueueSnapshot?) {
        playbackSnapshotUnloadWriteJob?.cancel()
        playbackSnapshotUnloadWriteJob = appScope.launch {
            runCatching {
                userPreferencesRepository.setPlaybackQueueSnapshot(snapshot)
            }.onFailure { e ->
                Timber.tag(TAG).w(e, "Failed to persist playback snapshot during unload")
            }
        }
    }

    /**
     * A debounced snapshot persist that is still pending at [onDestroy] would be
     * silently cancelled, leaving a stale "playing" snapshot behind — reopening the
     * app would then fake-resume playback that the system already tore down. Flush
     * it now, forced to a paused state, so the next restore comes back paused at
     * the correct position.
     */
    private fun flushPendingPlaybackSnapshotOnDestroy() {
        if (isPlaybackUnloadInProgress || isRestoringPlaybackSnapshot) return
        if (playbackSnapshotPersistJob?.isActive != true) return
        playbackSnapshotPersistJob?.cancel()
        val snapshot = capturePlaybackSnapshotFromPlayer(playWhenReadyOverride = false)
            ?: return
        writePlaybackSnapshotOnUnload(snapshot)
    }

    private fun refreshMediaSessionUiWithFollowUp(
        session: MediaSession,
        delayMs: Long = 250L
    ) {
        refreshMediaSessionUi(session, force = true)
        followUpMediaSessionUiRefreshJob?.cancel()
        followUpMediaSessionUiRefreshJob = serviceScope.launch {
            delay(delayMs)
            if (mediaSession === session) {
                refreshMediaSessionUi(session)
            }
        }
    }

    private fun updateManualShuffleState(
        session: MediaSession,
        enabled: Boolean,
        broadcast: Boolean
    ) {
        val changed = isManualShuffleEnabled != enabled
        isManualShuffleEnabled = enabled
        session.player.shuffleModeEnabled = enabled
        
        if (persistentShuffleEnabled) {
            serviceScope.launch {
                userPreferencesRepository.setShuffleOn(enabled)
            }
        }

        if (broadcast && changed) {
            val args = Bundle().apply {
                putBoolean(MusicNotificationProvider.EXTRA_SHUFFLE_ENABLED, enabled)
            }
            session.broadcastCustomCommand(
                SessionCommand(MusicNotificationProvider.CUSTOM_COMMAND_SET_SHUFFLE_STATE, Bundle.EMPTY),
                args
            )
        }
        refreshMediaSessionUi(session)
    }

    /**
     * @param exposeInternalArtwork the request comes from this app's own controller. Its items
     *   also keep their playlist tag: the rebuilt items come from the library and would
     *   otherwise drop it, turning off the playlist's transitions. Only the app itself is
     *   trusted to set it.
     */
    private suspend fun resolveMediaItemsByIds(
        requestedItems: List<MediaItem>,
        exposeInternalArtwork: Boolean = false,
    ): TrustedMediaItemsResolution {
        val songIds = requestedItems.map { it.mediaId }
        val songs = musicRepository.getSongsByIds(songIds).first()
        val songMap = songs.associateBy { it.id }

        val resolution = resolveMediaItemsWithTrustedArtworkGrants(requestedItems) { mediaId ->
            songMap[mediaId]?.let { song ->
                if (exposeInternalArtwork) {
                    MediaItemBuilder.build(song)
                } else {
                    MediaItemBuilder.buildForExternalController(this, song)
                }
            }
        }
        return resolution
    }

    /**
     * Custom session commands mutate app state, so they are limited to our own controllers
     * (in-app UI, media notification) and controllers the system marks as trusted
     * (System UI and trusted notification listeners).
     */
    private fun isPrivilegedController(controller: MediaSession.ControllerInfo): Boolean {
        return controller.packageName == packageName || controller.isTrusted
    }

    /**
     * Controllers connected before the current item changed need a fresh, narrowly scoped read
     * grant for each new artwork URI. The provider enforces these grants when opening a file.
     */
    private fun grantArtworkUriPermissionsToConnectedControllers(mediaItem: MediaItem) {
        val session = mediaSession ?: return
        session.connectedControllers
            .asSequence()
            .map { it.packageName }
            .distinct()
            .filter { it.isNotBlank() && it != packageName }
            .forEach { grantArtworkUriPermissions(it, listOf(mediaItem)) }
    }

    private fun grantArtworkUriPermissions(
        targetPackage: String,
        mediaItems: List<MediaItem>
    ) {
        if (targetPackage.isBlank()) return

        val providerAuthority = "$packageName.provider"
        val artworkAuthority = "$packageName.artwork"
        mediaItems.forEach { mediaItem ->
            val storedArtworkUri = resolveStoredArtworkUriString(mediaItem.mediaMetadata)
            val artworkUri = MediaItemBuilder.externalControllerArtworkUri(
                context = this,
                rawArtworkUri = storedArtworkUri
            ) ?: resolveArtworkUri(mediaItem.mediaMetadata) ?: return@forEach
            val authority = artworkUri.authority
            if (artworkUri.scheme?.lowercase() != "content" ||
                (authority != providerAuthority && authority != artworkAuthority)
            ) {
                return@forEach
            }

            runCatching {
                grantUriPermission(targetPackage, artworkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.onFailure { error ->
                Timber.tag(TAG).w(
                    error,
                    "Failed to grant artwork URI permission to package=%s uri=%s",
                    targetPackage,
                    artworkUri
                )
            }
        }
    }

    private fun buildMediaButtonPreferencesSignature(session: MediaSession): String {
        val player = session.player
        return buildString {
            append(player.currentMediaItem?.mediaId.orEmpty())
            append('|')
            append(isManualShuffleEnabled)
            append('|')
            append(player.repeatMode)
        }
    }

    private fun buildMediaButtonPreferences(session: MediaSession): List<CommandButton> {
        val player = session.player
        val shuffleOn = isManualShuffleEnabled
        val shuffleCommandAction = if (shuffleOn) {
            MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_OFF
        } else {
            MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_ON
        }
        val shuffleButton = CommandButton.Builder(
            if (shuffleOn) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF
        )
            .setDisplayName("Shuffle")
            .setSessionCommand(SessionCommand(shuffleCommandAction, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()

        val repeatButton = CommandButton.Builder(
            when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
                Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
                else -> CommandButton.ICON_REPEAT_OFF
            }
        )
            .setDisplayName("Repeat")
            .setSessionCommand(SessionCommand(MusicNotificationProvider.CUSTOM_COMMAND_CYCLE_REPEAT_MODE, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()

        val closeButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(R.drawable.rounded_close_24)
            .setDisplayName(getString(R.string.close_notification_player))
            .setSessionCommand(SessionCommand(MusicNotificationProvider.CUSTOM_COMMAND_CLOSE_PLAYER, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()

        return listOf(closeButton, shuffleButton, repeatButton)
    }

    /**
     * Bridges a suspend block into a [ListenableFuture] for Media3 callback methods.
     */
    private fun <T> CoroutineScope.future(block: suspend () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        launch(Dispatchers.IO) {
            try {
                future.set(block())
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }
}
