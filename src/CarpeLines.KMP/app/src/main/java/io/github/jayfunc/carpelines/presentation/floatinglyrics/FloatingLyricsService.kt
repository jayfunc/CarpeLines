package io.github.jayfunc.carpelines.presentation.floatinglyrics

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.jayfunc.carpelines.MainActivity
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.carpelines.ExternalLyricsResolver
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor
import io.github.jayfunc.carpelines.data.external.ExternalMediaState
import io.github.jayfunc.carpelines.data.external.decodeArtworkBitmap
import io.github.jayfunc.carpelines.data.model.Lyrics
import io.github.jayfunc.carpelines.data.model.hasTimelineContent
import io.github.jayfunc.carpelines.data.model.lineAt
import io.github.jayfunc.carpelines.data.preferences.AppThemeMode
import io.github.jayfunc.carpelines.data.preferences.ThemePreferencesRepository
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsOffsetStore
import io.github.jayfunc.carpelines.ui.theme.extractSeedColor
import io.github.jayfunc.carpelines.ui.theme.generateColorSchemeFromSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Keeps the floating lyric window alive while the desktop or lock-screen lyrics switch is on.
 *
 * The service follows the shared external media session; lyrics are resolved once per track by
 * [ExternalLyricsResolver] and the window only repaints when the active line changes. The same
 * window is reused for both modes: the lock-screen switch simply adds
 * [android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED] and lets the keyguard receiver
 * decide whether it may be shown.
 */
class FloatingLyricsService : Service() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FloatingLyricsEntryPoint {
        fun userPreferencesRepository(): UserPreferencesRepository
        fun themePreferencesRepository(): ThemePreferencesRepository
        fun externalLyricsResolver(): ExternalLyricsResolver
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var preferences: UserPreferencesRepository
    private lateinit var themePreferences: ThemePreferencesRepository
    private lateinit var lyricsResolver: ExternalLyricsResolver

    private var overlay: FloatingLyricsOverlay? = null
    private var observingStarted = false

    private var desktopEnabled = false
    private var lockScreenEnabled = false
    private var keyguardLocked = false
    private var screenOn = true

    private var currentMedia: ExternalMediaState? = null
    private var currentLyrics: Lyrics? = null
    private var lyricsResolved = false
    private var lyricsJob: Job? = null
    private var offsetJob: Job? = null
    private var syncOffsetMs = 0

    /** Theme state the floating palette is derived from: app mode plus per-track artwork. */
    private var appThemeMode = AppThemeMode.FOLLOW_SYSTEM
    private var paletteMediaIdentity: String? = null
    private var paletteArtworkKey = ""
    private var paletteDark = false
    private var paletteJob: Job? = null
    private var artworkPalette: FloatingLyricsColors? = null
    private var artworkPaletteDark = false

    private var latestPositionMs = 0L
    private var lastRenderedKey: String? = null
    private var notificationLine = ""
    private var notificationSecondary = ""
    private var lastNotificationKey: String? = null

    /**
     * Polls the two system facts the overlay depends on. MIUI/HyperOS does not reliably deliver
     * USER_PRESENT to the app after a password unlock, and SCREEN_ON arrives before the keyguard
     * has settled, so broadcast-driven state kept getting stuck on "locked". A 2 Hz poll is cheap
     * and cannot be missed; it also repairs an overlay the keyguard detached during the animation.
     */
    private val statePoll = object : Runnable {
        override fun run() {
            val interactive = isInteractive()
            val locked = if (interactive) isKeyguardLocked() else true
            if (interactive != screenOn || locked != keyguardLocked) {
                screenOn = interactive
                keyguardLocked = locked
                Timber.d("FloatingLyrics: poll screen=%s locked=%s", screenOn, keyguardLocked)
                syncWindow()
            } else if (interactive && !locked && desktopEnabled && currentMedia != null &&
                (overlay?.isAttached != true || overlay?.isShown != true)
            ) {
                // The window was hidden/detached by the keyguard animation; attach it again.
                syncWindow()
            }
            mainHandler.postDelayed(this, STATE_POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            FloatingLyricsEntryPoint::class.java,
        )
        preferences = entryPoint.userPreferencesRepository()
        themePreferences = entryPoint.themePreferencesRepository()
        lyricsResolver = entryPoint.externalLyricsResolver()

        overlay = FloatingLyricsOverlay(
            context = this,
            onOpenLyrics = ::openLyricsPage,
            onPrevious = { ExternalMediaMonitor.previous() },
            onPlayPause = { ExternalMediaMonitor.playPause() },
            onNext = { ExternalMediaMonitor.next() },
            onClose = ::disableFromOverlay,
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val foregroundStarted = runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        }.onFailure { Timber.w(it, "Floating lyrics could not become a foreground service") }.isSuccess
        if (!foregroundStarted) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_STOP) {
            disableEverywhere()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_PREVIOUS -> ExternalMediaMonitor.previous()
            ACTION_PLAY_PAUSE -> ExternalMediaMonitor.playPause()
            ACTION_NEXT -> ExternalMediaMonitor.next()
        }

        screenOn = isInteractive()
        keyguardLocked = if (screenOn) isKeyguardLocked() else true
        if (!observingStarted) {
            observingStarted = true
            startObserving()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        mainHandler.removeCallbacksAndMessages(null)
        overlay?.hide()
        overlay = null
        scope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startObserving() {
        mainHandler.post(statePoll)

        scope.launch {
            combine(
                preferences.floatingLyricsEnabledFlow,
                preferences.lockScreenLyricsEnabledFlow,
            ) { desktop, lockScreen -> desktop to lockScreen }
                .collectLatest { (desktop, lockScreen) ->
                    desktopEnabled = desktop
                    lockScreenEnabled = lockScreen
                    if (!desktop && !lockScreen) {
                        stopSelf()
                        return@collectLatest
                    }
                    syncWindow()
                    refreshNotification(force = true)
                }
        }

        scope.launch {
            combine(
                ExternalMediaMonitor.state,
                ExternalMediaMonitor.positionMs,
            ) { media, position -> media to position }
                .collectLatest { (media, position) ->
                    latestPositionMs = position
                    onMediaTick(media)
                }
        }

        // The window is tinted from the playing cover, so it must know whether the app theme is
        // currently dark and repaint when the user switches modes.
        scope.launch {
            themePreferences.appThemeModeFlow
                .distinctUntilChanged()
                .collect { mode ->
                    if (mode == appThemeMode) return@collect
                    appThemeMode = mode
                    currentMedia?.let(::updatePalette)
                }
        }
    }

    private fun onMediaTick(media: ExternalMediaState?) {
        if (media == null || !media.hasMetadata) {
            if (currentMedia != null) {
                currentMedia = null
                currentLyrics = null
                lyricsResolved = false
                lyricsJob?.cancel()
                offsetJob?.cancel()
                paletteJob?.cancel()
                paletteMediaIdentity = null
                paletteArtworkKey = ""
                artworkPalette = null
                lastRenderedKey = null
                notificationLine = ""
                notificationSecondary = ""
                overlay?.applyColors(null)
                overlay?.hide()
                refreshNotification(force = true)
            }
            return
        }

        val identityChanged = currentMedia?.identity != media.identity
        currentMedia = media
        if (identityChanged) {
            currentLyrics = lyricsResolver.peek(media)
            lyricsResolved = currentLyrics != null
            lastRenderedKey = null
            requestLyrics(media)
            startOffsetUpdates(media)
            notificationLine = ""
            notificationSecondary = ""
            refreshNotification(force = true)
            syncWindow()
        }
        updatePalette(media)
        render(media)
    }

    /**
     * Rebuilds the window palette when the track, the artwork availability or the light/dark
     * choice changes. Players often publish the title before the cover, so artwork arriving for
     * an already-playing track must trigger a repaint as well.
     */
    private fun updatePalette(media: ExternalMediaState) {
        val dark = isDarkTheme()
        // The URI or bitmap appearing later in the same track must repaint; a bitmap added on top
        // of an already published URI counts as a change because it is the more reliable source.
        val artworkKey = "${media.artworkUri.orEmpty()}|${media.artworkBitmap != null}"
        if (media.identity == paletteMediaIdentity &&
            artworkKey == paletteArtworkKey &&
            dark == paletteDark
        ) {
            return
        }
        paletteMediaIdentity = media.identity
        paletteArtworkKey = artworkKey
        paletteDark = dark
        paletteJob?.cancel()
        paletteJob = scope.launch {
            val colors = resolvePalette(media, dark)
            if (currentMedia?.identity != media.identity) return@launch
            when {
                colors != null -> {
                    artworkPalette = colors
                    artworkPaletteDark = dark
                    overlay?.applyColors(colors)
                }
                // Like the in-app theme, hold the previous cover palette while a track has no
                // artwork; only drop to the static app-theme colours when there is nothing to hold.
                artworkPalette != null && artworkPaletteDark == dark ->
                    overlay?.applyColors(artworkPalette)
                else -> overlay?.applyColors(null)
            }
        }
    }

    /**
     * Applies the same pipeline the in-app theme uses: the cover seeds an M3E tonal scheme whose
     * surface/content/primary roles tint the card. Without cover artwork the window keeps the
     * app-theme colours from colors.xml.
     */
    private suspend fun resolvePalette(media: ExternalMediaState, dark: Boolean): FloatingLyricsColors? {
        val bitmap = media.artworkBitmap ?: media.artworkUri?.let { uri ->
            withContext(Dispatchers.IO) { decodeArtworkBitmap(this@FloatingLyricsService, uri) }
        } ?: return null
        return withContext(Dispatchers.Default) {
            runCatching {
                val scheme = generateColorSchemeFromSeed(extractSeedColor(bitmap))
                (if (dark) scheme.dark else scheme.light).toWindowColors()
            }.getOrNull()
        }
    }

    private fun ColorScheme.toWindowColors(): FloatingLyricsColors = FloatingLyricsColors(
        container = surfaceContainerHigh.copy(alpha = WINDOW_CONTAINER_ALPHA).toArgb(),
        textPrimary = onSurface.toArgb(),
        textSecondary = onSurfaceVariant.copy(alpha = WINDOW_SECONDARY_ALPHA).toArgb(),
        accent = primary.toArgb(),
    )

    private fun isDarkTheme(): Boolean = when (appThemeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // A follow-system theme flips outside the app; repaint against the new night mode.
        currentMedia?.let(::updatePalette)
    }

    private fun requestLyrics(media: ExternalMediaState) {
        lyricsJob?.cancel()
        lyricsJob = scope.launch {
            val lyrics = withTimeoutOrNull(LYRICS_TIMEOUT_MS) {
                lyricsResolver.resolve(media)
            }
            if (currentMedia?.identity != media.identity) return@launch
            currentLyrics = lyrics
            lyricsResolved = true
            lastRenderedKey = null
            render(media)
        }
    }

    private fun startOffsetUpdates(media: ExternalMediaState) {
        offsetJob?.cancel()
        syncOffsetMs = 0
        val trackKey = LyricsOffsetStore.trackKey(media.title, media.artist)
        offsetJob = scope.launch {
            LyricsOffsetStore.offset(this@FloatingLyricsService, trackKey).collect { offset ->
                syncOffsetMs = offset
                lastRenderedKey = null
                currentMedia?.let(::render)
            }
        }
    }

    private fun render(media: ExternalMediaState) {
        val line = currentLyrics
            ?.takeIf { it.hasTimelineContent() }
            ?.lineAt(latestPositionMs + syncOffsetMs)

        val currentText = line?.text ?: media.title
        val translation = line?.translation
        val romanization = line?.romanization
        val nextText = line?.nextText
        val secondaryText = when {
            translation != null -> translation
            romanization != null -> romanization
            nextText != null -> nextText
            !lyricsResolved -> getString(R.string.floating_lyrics_searching)
            line != null -> media.artist.ifBlank { media.appLabel }
            else -> getString(R.string.floating_lyrics_not_found)
        }

        val renderKey = listOf(currentText, secondaryText, media.isPlaying).joinToString("\u0001")
        if (renderKey == lastRenderedKey) return
        lastRenderedKey = renderKey
        notificationLine = currentText
        notificationSecondary = secondaryText
        overlay?.update(
            currentLine = currentText,
            secondaryLine = secondaryText,
            isPlaying = media.isPlaying,
        )
        // The ongoing foreground notification doubles as the lock-screen lyrics surface: current
        // Android builds do not let app overlays or automatically launched activities sit above
        // the keyguard, but a PUBLIC notification does.
        if (lockScreenEnabled) refreshNotification()
    }

    /** Shows the desktop window only while the device is unlocked. */
    private fun syncWindow() {
        val media = currentMedia
        val showDesktopOverlay = media != null && desktopEnabled && !keyguardLocked &&
            Settings.canDrawOverlays(this)
        Timber.d(
            "FloatingLyrics: sync desktop=%s locked=%s media=%s attached=%s shown=%s",
            desktopEnabled,
            keyguardLocked,
            media != null,
            overlay?.isAttached == true,
            overlay?.isShown == true,
        )
        if (showDesktopOverlay) {
            val wasAttached = overlay?.isAttached == true
            val wasShown = overlay?.isShown == true
            // An attached-but-hidden window is the MIUI unlock-animation failure mode: drop it
            // and attach a fresh one instead of assuming it will come back by itself.
            if (wasAttached && !wasShown) overlay?.hide()
            overlay?.show()
            // A freshly attached window starts empty; drop the render dedupe key so the current
            // line is painted even when playback is paused and the position never ticks again.
            if (!wasAttached || !wasShown) lastRenderedKey = null
            media?.let(::render)
        } else {
            overlay?.hide()
        }
    }

    private fun openLyricsPage() {
        runCatching { startActivity(lyricsPageIntent()) }
    }

    private fun lyricsPageIntent(): Intent =
        Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(android.net.Uri.parse(LYRICS_DEEP_LINK))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private fun disableFromOverlay() {
        scope.launch {
            preferences.setFloatingLyricsEnabled(false)
            preferences.setLockScreenLyricsEnabled(false)
            stopSelf()
        }
    }

    private fun disableEverywhere() {
        scope.launch {
            preferences.setFloatingLyricsEnabled(false)
            preferences.setLockScreenLyricsEnabled(false)
            stopSelf()
        }
    }

    private fun isKeyguardLocked(): Boolean =
        getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true

    private fun isInteractive(): Boolean =
        getSystemService(PowerManager::class.java)?.isInteractive != false

    /**
     * Updates the ongoing notification. On the lock screen it is the lyrics surface itself,
     * showing the current line, the translation/next line and transport controls.
     */
    private fun refreshNotification(force: Boolean = false) {
        val media = currentMedia
        val key = buildString {
            append(lockScreenEnabled).append('\u0001')
            append(media?.identity.orEmpty()).append('\u0001')
            append(media?.isPlaying == true).append('\u0001')
            append(notificationLine).append('\u0001')
            append(notificationSecondary)
        }
        if (!force && key == lastNotificationKey) return
        lastNotificationKey = key
        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun buildNotification(): Notification {
        ensureChannel(getSystemService(NotificationManager::class.java))
        val media = currentMedia
        val hasMedia = media != null && media.hasMetadata
        val showLyrics = lockScreenEnabled && hasMedia
        val title = if (showLyrics) {
            notificationLine.ifBlank { media?.title.orEmpty() }
        } else {
            getString(R.string.floating_lyrics_notification_title)
        }
        val text = when {
            showLyrics && notificationSecondary.isNotBlank() -> notificationSecondary
            hasMedia -> getString(
                R.string.floating_lyrics_notification_text,
                media!!.title,
                media.artist,
            )
            else -> getString(R.string.floating_lyrics_status_showing)
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.monochrome_player)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openLyricsPendingIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(NOISY_ONCE_VIBRATION)

        if (showLyrics) {
            val subtitle = media?.let { "${it.title} · ${it.artist}" }
            if (!subtitle.isNullOrBlank()) builder.setSubText(subtitle)
            if (notificationSecondary.isNotBlank()) {
                builder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$notificationLine\n$notificationSecondary")
                )
            }
        }

        if (hasMedia) {
            builder.addAction(
                R.drawable.rounded_skip_previous_filled_24,
                getString(R.string.previous_track),
                serviceAction(ACTION_PREVIOUS, PREVIOUS_REQUEST_CODE),
            )
            builder.addAction(
                if (media!!.isPlaying) R.drawable.rounded_pause_filled_24
                else R.drawable.rounded_play_arrow_filled_24,
                getString(if (media.isPlaying) R.string.cd_pause else R.string.cd_play),
                serviceAction(ACTION_PLAY_PAUSE, PLAY_PAUSE_REQUEST_CODE),
            )
            builder.addAction(
                R.drawable.rounded_skip_next_filled_24,
                getString(R.string.next_track),
                serviceAction(ACTION_NEXT, NEXT_REQUEST_CODE),
            )
        }
        builder.addAction(
            R.drawable.rounded_close_24,
            getString(R.string.floating_lyrics_close),
            serviceAction(ACTION_STOP, STOP_REQUEST_CODE),
        )
        return builder.build()
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, FloatingLyricsService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openLyricsPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        OPEN_REQUEST_CODE,
        lyricsPageIntent(),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ensureChannel(manager: NotificationManager?) {
        manager ?: return
        // Channels from earlier builds of this feature are no longer used; drop them so the
        // notification settings do not accumulate orphaned entries.
        manager.deleteNotificationChannel("floating_lyrics")
        manager.deleteNotificationChannel("floating_lyrics_v2")
        manager.deleteNotificationChannel("floating_lyrics_v3")
        manager.deleteNotificationChannel("floating_lyrics_v4")
        manager.deleteNotificationChannel("floating_lyrics_v5")
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.floating_lyrics_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = NOISY_ONCE_VIBRATION
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = getString(R.string.floating_lyrics_status_showing)
            }
        )
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "floating_lyrics_lock"
        private const val NOTIFICATION_ID = 0x464C
        private const val OPEN_REQUEST_CODE = 0x464D
        private const val STOP_REQUEST_CODE = 0x464E
        private const val PREVIOUS_REQUEST_CODE = 0x464F
        private const val PLAY_PAUSE_REQUEST_CODE = 0x4650
        private const val NEXT_REQUEST_CODE = 0x4651
        private const val STATE_POLL_MS = 500L
        private const val LYRICS_TIMEOUT_MS = 12_000L
        private const val LYRICS_DEEP_LINK = "carpelines://lyrics"

        /** Matches the translucent card and muted secondary line of the static XML palette. */
        private const val WINDOW_CONTAINER_ALPHA = 0.95f
        private const val WINDOW_SECONDARY_ALPHA = 0.7f

        /**
         * A technically-noisy notification. Some ROMs (MIUI/HyperOS) hide silent notifications
         * from the lock screen; one 1 ms vibration at post marks this one as noisy without
         * producing a perceptible buzz, and ONLY_ALERT_ONCE keeps later line updates silent.
         */
        private val NOISY_ONCE_VIBRATION = longArrayOf(0, 1)

        const val ACTION_STOP = "io.github.jayfunc.carpelines.action.STOP_FLOATING_LYRICS"
        const val ACTION_PREVIOUS = "io.github.jayfunc.carpelines.action.FLOATING_PREVIOUS"
        const val ACTION_PLAY_PAUSE = "io.github.jayfunc.carpelines.action.FLOATING_PLAY_PAUSE"
        const val ACTION_NEXT = "io.github.jayfunc.carpelines.action.FLOATING_NEXT"

        @Volatile
        private var isRunning = false

        /** Starts the foreground overlay service; safe to call when it is already running. */
        fun start(context: Context) {
            if (isRunning) return
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, FloatingLyricsService::class.java),
                )
            }.onFailure {
                Timber.w(it, "Could not start floating lyrics")
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, FloatingLyricsService::class.java))
            }
        }
    }
}
