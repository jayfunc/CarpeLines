package io.github.jayfunc.carpelines

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import io.github.jayfunc.carpelines.data.diagnostics.AdvancedPerformanceDiagnosticsController
import io.github.jayfunc.carpelines.data.media.SongMetadataEditor
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.data.playlist.M3uSyncCoordinator
import io.github.jayfunc.carpelines.data.repository.ArtistImageRepository
import io.github.jayfunc.carpelines.presentation.viewmodel.LibraryStateHolder
import io.github.jayfunc.carpelines.presentation.viewmodel.ThemeStateHolder
import io.github.jayfunc.carpelines.utils.AlbumArtCacheManager
import io.github.jayfunc.carpelines.utils.FolderArtworkSettingsCoordinator
import io.github.jayfunc.carpelines.utils.AppLocaleManager
import io.github.jayfunc.carpelines.utils.CrashHandler
import io.github.jayfunc.carpelines.utils.MediaMetadataRetrieverPool
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CarpeLinesApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: dagger.Lazy<ImageLoader>

    @Inject
    lateinit var navidromeCoilFetcherFactory: dagger.Lazy<io.github.jayfunc.carpelines.data.image.NavidromeCoilFetcher.Factory>

    @Inject
    lateinit var jellyfinCoilFetcherFactory: dagger.Lazy<io.github.jayfunc.carpelines.data.image.JellyfinCoilFetcher.Factory>

    @Inject
    lateinit var localArtworkCoilFetcherFactory: dagger.Lazy<io.github.jayfunc.carpelines.data.image.LocalArtworkCoilFetcher.Factory>

    @Inject
    lateinit var themeStateHolder: dagger.Lazy<ThemeStateHolder>

    @Inject
    lateinit var artistImageRepository: dagger.Lazy<ArtistImageRepository>

    @Inject
    lateinit var libraryStateHolder: dagger.Lazy<LibraryStateHolder>

    @Inject
    lateinit var userPreferencesRepository: dagger.Lazy<UserPreferencesRepository>

    @Inject
    lateinit var folderArtworkSettingsCoordinator: dagger.Lazy<FolderArtworkSettingsCoordinator>

    @Inject
    lateinit var syncManager: dagger.Lazy<io.github.jayfunc.carpelines.data.worker.SyncManager>

    @Inject
    lateinit var advancedPerformanceDiagnosticsController: dagger.Lazy<AdvancedPerformanceDiagnosticsController>

    @Inject
    lateinit var m3uSyncCoordinator: dagger.Lazy<M3uSyncCoordinator>

    @Inject
    lateinit var updateCheckScheduler: dagger.Lazy<io.github.jayfunc.carpelines.data.update.UpdateCheckScheduler>

    @Inject
    lateinit var songMetadataEditor: dagger.Lazy<SongMetadataEditor>

    @Inject
    lateinit var externalPlaybackRecorder: dagger.Lazy<io.github.jayfunc.carpelines.data.external.ExternalPlaybackRecorder>

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val widgetUpdateScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "carpelines_music_channel"
    }

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            libraryStateHolder.get().restoreAfterTrimIfNeeded()
            m3uSyncCoordinator.get().onAppForeground()
            advancedPerformanceDiagnosticsController.get().onAppForeground()
            // Gives songs cut off by an interrupted tag save (a failed write-back, or the app
            // being killed mid-write) their edited audio back, and lists the ones the app can no
            // longer write to so the player can ask the user for access.
            startupScope.launch { songMetadataEditor.get().resumeInterruptedWriteBacks() }
        }

        override fun onResume(owner: LifecycleOwner) {
            // Image access can change while system settings is open, without restarting us.
            startupScope.launch { folderArtworkSettingsCoordinator.get().reconcile() }
            // The listener can also come back while the user was in the ROM's auto-start or
            // notification-access settings; ask for a rebind as soon as we are in front again.
            io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor.ensureConnected(
                this@CarpeLinesApplication
            )
            // A cold start from a background bind cannot start the overlay service (Android 12+
            // refuses background FGS starts), and granting the overlay permission later does not
            // change the preference. Both cases recover here, while the app is in front.
            startupScope.launch {
                val desktop = userPreferencesRepository.get().floatingLyricsEnabledFlow.first()
                val lockScreen = userPreferencesRepository.get().lockScreenLyricsEnabledFlow.first()
                if ((desktop || lockScreen) &&
                    android.provider.Settings.canDrawOverlays(this@CarpeLinesApplication)
                ) {
                    io.github.jayfunc.carpelines.presentation.floatinglyrics.FloatingLyricsService.start(
                        this@CarpeLinesApplication
                    )
                }
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            advancedPerformanceDiagnosticsController.get().onAppBackground()
            // Bank whatever the foreign session has accrued: the process may not survive to see
            // the next tick.
            externalPlaybackRecorder.get().flush()
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocaleManager.wrapContext(base))
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.BUILD_TYPE != "benchmark") {
            CrashHandler.install(this)
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        // Apply the saved player filter before/as the listener attaches. The filter can also
        // change while the app runs, and the monitor rebuilds its followed sessions on each change.
        startupScope.launch {
            combine(
                userPreferencesRepository.get().externalPlayerFilterEnabledFlow,
                userPreferencesRepository.get().externalPlayerAllowedPackagesFlow,
            ) { enabled, packages -> enabled to packages }
                .distinctUntilChanged()
                .collect { (enabled, packages) ->
                    io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor.setPlayerFilter(
                        enabled = enabled,
                        allowedPackages = packages,
                    )
                }
        }

        // Rebind the external media listener after a process restart when access was granted.
        io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor.ensureConnected(this)

        // Widgets follow the same foreign session as the home screen and lyrics page.
        widgetUpdateScope.launch {
            kotlinx.coroutines.flow.combine(
                io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor.state,
                io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor.positionMs,
            ) { media, position -> media to position }
                .sample(500L)
                .collect { (media, position) ->
                    io.github.jayfunc.carpelines.ui.glancewidget.WidgetMediaUpdater.onPlaybackChanged(
                        this@CarpeLinesApplication,
                        media,
                        position,
                    )
                }
        }

        // The floating desktop/lock-screen lyrics follow the same two switches. Starting from a
        // cold background process can be refused by Android 12+, so onResume re-asserts it as
        // well; the service itself is idempotent.
        startupScope.launch {
            kotlinx.coroutines.flow.combine(
                userPreferencesRepository.get().floatingLyricsEnabledFlow,
                userPreferencesRepository.get().lockScreenLyricsEnabledFlow,
            ) { desktop, lockScreen -> desktop to lockScreen }
                .distinctUntilChanged()
                .collect { (desktop, lockScreen) ->
                    if (desktop || lockScreen) {
                        io.github.jayfunc.carpelines.presentation.floatinglyrics.FloatingLyricsService.start(
                            this@CarpeLinesApplication
                        )
                    } else {
                        io.github.jayfunc.carpelines.presentation.floatinglyrics.FloatingLyricsService.stop(
                            this@CarpeLinesApplication
                        )
                    }
                }
        }

        // Listening stats and the home screen's player strip are fed by foreign media sessions,
        // which is the only playback this app has.
        externalPlaybackRecorder.get().start(startupScope)

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        syncManager.get().start()

        m3uSyncCoordinator.get().start()

        updateCheckScheduler.get().start()

        advancedPerformanceDiagnosticsController.get().start(startupScope)

        startupScope.launch {
            val savedLimit = runCatching {
                userPreferencesRepository.get().albumArtCacheLimitMbFlow.first()
            }.getOrNull()
            if (savedLimit != null) {
                AlbumArtCacheManager.configuredCacheLimitMb = savedLimit.toLong()
            }
        }

        // Collected rather than read once: the preference can also change underneath us, most
        // notably when a backup restore writes it straight into DataStore. A stale mirror would
        // leave the toggle reporting one thing while artwork resolution did another.
        startupScope.launch {
            userPreferencesRepository.get().useFolderAlbumArtFlow.collect {
                folderArtworkSettingsCoordinator.get().reconcile()
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return imageLoader.get().newBuilder()
            .components {
                add(io.github.jayfunc.carpelines.data.image.SharedCloudArtworkStringMapper(packageName))
                add(io.github.jayfunc.carpelines.data.image.SharedCloudArtworkUriMapper(packageName))
                add(localArtworkCoilFetcherFactory.get())
                add(navidromeCoilFetcherFactory.get())
                add(jellyfinCoilFetcherFactory.get())
            }
            .build()
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        imageLoader.get().memoryCache?.trimMemory(level)

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            themeStateHolder.get().trimMemory(level)
        }

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            artistImageRepository.get().clearCache()
            MediaMetadataRetrieverPool.clear()
        }

        libraryStateHolder.get().trimMemory(level)

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        ) {
            imageLoader.get().memoryCache?.clear()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

}
