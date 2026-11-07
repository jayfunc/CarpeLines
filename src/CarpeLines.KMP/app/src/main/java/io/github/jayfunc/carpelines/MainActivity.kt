package io.github.jayfunc.carpelines

import io.github.jayfunc.carpelines.presentation.navigation.navigateSafely

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import io.github.jayfunc.carpelines.utils.traceSection
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import io.github.jayfunc.carpelines.presentation.viewmodel.PlayerSheetState

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor
import io.github.jayfunc.carpelines.data.external.decodeArtworkBitmap
import io.github.jayfunc.carpelines.data.preferences.AppThemeMode
import io.github.jayfunc.carpelines.data.preferences.ThemePreferencesRepository
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.data.worker.SyncManager
import io.github.jayfunc.carpelines.data.worker.SyncProgress
import io.github.jayfunc.carpelines.presentation.components.AllFilesAccessDialog
import io.github.jayfunc.carpelines.presentation.components.CrashReportDialog
import io.github.jayfunc.carpelines.presentation.components.LocalAppMessageHost
import io.github.jayfunc.carpelines.presentation.components.rememberAppMessageHost
import io.github.jayfunc.carpelines.presentation.components.NavBarOuterBottomPadding
import io.github.jayfunc.carpelines.presentation.components.NavBarOuterHorizontalPadding
import io.github.jayfunc.carpelines.presentation.components.NavBarOuterTopPadding
import io.github.jayfunc.carpelines.presentation.components.PlayerInternalNavigationBar
import io.github.jayfunc.carpelines.presentation.components.PlayerNavigationRail
import io.github.jayfunc.carpelines.presentation.components.sanitizeNavigationBarBottomInset
import io.github.jayfunc.carpelines.presentation.navigation.AppNavigation
import io.github.jayfunc.carpelines.presentation.navigation.Screen
import io.github.jayfunc.carpelines.presentation.screens.SetupScreen
import io.github.jayfunc.carpelines.presentation.viewmodel.MainViewModel
import io.github.jayfunc.carpelines.presentation.viewmodel.PlayerViewModel
import io.github.jayfunc.carpelines.presentation.splash.CarpeLinesSplash
import io.github.jayfunc.carpelines.ui.m3e.M3ELoadingState
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import io.github.jayfunc.carpelines.ui.theme.CarpeLinesTheme
import io.github.jayfunc.carpelines.ui.theme.LocalShowScrollbar
import io.github.jayfunc.carpelines.ui.theme.rememberM3ELayoutMetrics
import io.github.jayfunc.carpelines.ui.theme.extractSeedColor
import io.github.jayfunc.carpelines.ui.theme.generateColorSchemeFromSeed
import io.github.jayfunc.carpelines.utils.AppLocaleManager
import io.github.jayfunc.carpelines.utils.CrashHandler
import io.github.jayfunc.carpelines.utils.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import io.github.jayfunc.carpelines.presentation.utils.AppHapticsConfig
import io.github.jayfunc.carpelines.presentation.utils.LocalAppHapticsConfig
import io.github.jayfunc.carpelines.presentation.utils.NoOpHapticFeedback
import io.github.jayfunc.carpelines.utils.CrashLogData
import javax.annotation.concurrent.Immutable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


@Immutable
data class BottomNavItem(
    /** Shown as the tab label and content description, so it follows the app language. */
    @StringRes val labelResId: Int,
    @DrawableRes val iconResId: Int,
    @DrawableRes val selectedIconResId: Int? = null,
    val screen: Screen
)

/** Top-level destinations shared by the bottom navigation bar and the navigation rail. */
internal val MainNavItems: ImmutableList<BottomNavItem> = persistentListOf(
    BottomNavItem(R.string.tab_home, R.drawable.rounded_home_24, R.drawable.home_24_rounded_filled, Screen.Home),
    BottomNavItem(R.string.tab_lyrics, R.drawable.rounded_lyrics_24, R.drawable.rounded_lyrics_24, Screen.NowPlayingLyrics),
    BottomNavItem(R.string.tab_stats, R.drawable.rounded_monitoring_24, R.drawable.rounded_monitoring_24, Screen.Stats),
    BottomNavItem(R.string.tab_settings, R.drawable.rounded_settings_24, R.drawable.rounded_settings_24, Screen.Settings)
)


private const val SYSTEM_SPLASH_HANDOFF_MS = 150L


@androidx.annotation.OptIn(UnstableApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private var isUIVisiblyReady = false
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository
    @Inject
    lateinit var syncManager: SyncManager
    private val _pendingUpdatesNavigation = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val _pendingDeepLinkIntent = kotlinx.coroutines.flow.MutableStateFlow<Intent?>(null)

    /** Flips when the system splash view is removed; CarpeLinesSplash starts animating then. */
    private val systemSplashGone = mutableStateOf(false)

    private val requestAllFilesAccessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
    }

    /**
     * Builds the app-wide palette from the system media session's artwork: the bitmap when the
     * session publishes one, otherwise the content URI it pointed at. Returns null when there is
     * no session, so the app falls back to the user's theme preset.
     */
    private fun resolveExternalArtworkColorScheme(
        artworkBitmap: Bitmap?,
        artworkUri: String?,
    ): io.github.jayfunc.carpelines.presentation.viewmodel.ColorSchemePair? {
        val bitmap = artworkBitmap ?: artworkUri?.let { decodeArtworkBitmap(this, it) } ?: return null
        val seed = extractSeedColor(bitmap)
        return generateColorSchemeFromSeed(seed)
    }

    @CallSuper
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LogUtils.d(this, "onCreate")
        val splashScreen = installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { false }

        val isBenchmarkMode = intent.getBooleanExtra("is_benchmark", false)

        // The animated splash plays on fresh launches only (not on recreation), and never
        // when the user has turned animations off or a benchmark is timing startup.
        val playAnimatedSplash = savedInstanceState == null &&
            !isBenchmarkMode &&
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        if (playAnimatedSplash) {
            // CarpeLinesSplash's first frame is the system splash's last one, so the system view
            // only needs a short fade to cover device-specific icon rendering differences.
            splashScreen.setOnExitAnimationListener { provider ->
                provider.view.animate()
                    .alpha(0f)
                    .setDuration(SYSTEM_SPLASH_HANDOFF_MS)
                    .withEndAction {
                        provider.remove()
                        systemSplashGone.value = true
                    }
                    .start()
            }
        }
        val shouldBenchmarkRebuildDatabase =
            isBenchmarkMode && intent.getBooleanExtra("benchmark_rebuild_database", false)
        Timber.tag("CarpeLinesBenchmark").i(
            "onCreate benchmark=$isBenchmarkMode rebuildDatabase=$shouldBenchmarkRebuildDatabase"
        )
        if (shouldBenchmarkRebuildDatabase) {
            lifecycleScope.launch {
                userPreferencesRepository.setInitialSetupDone(true)
                Timber.tag("CarpeLinesBenchmark").i("Enqueueing benchmark database rebuild")
                syncManager.rebuildDatabase()
            }
        }

        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            val appThemeMode by themePreferencesRepository.appThemeModeFlow.collectAsStateWithLifecycle(initialValue = AppThemeMode.FOLLOW_SYSTEM)
            val useDarkTheme = when (appThemeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                else -> systemDarkTheme
            }
            // The app-wide palette always follows the playing song: the old opt-in switch and
            // the in-app player pipeline never fired (CarpeLines has no local playback), so the
            // colours now come straight from the system media session's artwork.
            val externalMediaForTheme by ExternalMediaMonitor.state.collectAsStateWithLifecycle()
            var appWideNowPlayingScheme by remember {
                mutableStateOf<io.github.jayfunc.carpelines.presentation.viewmodel.ColorSchemePair?>(null)
            }
            LaunchedEffect(
                externalMediaForTheme?.identity,
                externalMediaForTheme?.artworkBitmap,
                externalMediaForTheme?.artworkUri,
            ) {
                val media = externalMediaForTheme
                if (media == null) {
                    appWideNowPlayingScheme = null
                    return@LaunchedEffect
                }
                // Players usually publish the new track's metadata before its cover, so this
                // state briefly carries neither a bitmap nor a URI. Rebuilding the palette then
                // fell back to the system colours for a second before snapping back to the
                // cover — the white/black flash seen when switching songs. Hold the previous
                // cover palette until the new artwork can actually be read.
                if (media.artworkBitmap == null && media.artworkUri == null) {
                    return@LaunchedEffect
                }
                val scheme = withContext(Dispatchers.Default) {
                    resolveExternalArtworkColorScheme(
                        artworkBitmap = media.artworkBitmap,
                        artworkUri = media.artworkUri,
                    )
                }
                // An unreadable URI must not erase the palette either; the next metadata
                // update retries.
                if (scheme != null) appWideNowPlayingScheme = scheme
            }
            val isSetupComplete by mainViewModel.isSetupComplete.collectAsStateWithLifecycle()
            
            var showCrashReportDialog by remember { mutableStateOf(false) }
            var crashLogData by remember { mutableStateOf<CrashLogData?>(null) }
            
            // Media access is optional (it only enables local tag/.lrc lyrics), so setup must not
            // be shown again just because the user skipped it. Notification access is checked by
            // the setup flow itself.
            val showSetupScreen = remember(isSetupComplete, isBenchmarkMode) {
                when {
                    isBenchmarkMode -> false
                    isSetupComplete == null -> null
                    else -> !isSetupComplete!!
                }
            }

            LaunchedEffect(showSetupScreen) {
                if (showSetupScreen == false) {
                    LogUtils.i(this, "Setup complete. Starting sync.")
                    mainViewModel.startSync()
                }
            }

            LaunchedEffect(Unit) {
                if (!isBenchmarkMode && CrashHandler.hasCrashLog()) {
                    crashLogData = CrashHandler.getCrashLog()
                    showCrashReportDialog = true
                }
            }

            CarpeLinesTheme(
                darkTheme = useDarkTheme,
                colorSchemePairOverride = appWideNowPlayingScheme
            ) {
                var contentVisible by remember { mutableStateOf(false) }
                val contentAlpha by animateFloatAsState(
                    targetValue = if (contentVisible) 1f else 0f,
                    animationSpec = tween(600, easing = LinearOutSlowInEasing),
                    label = "AppContentAlpha"
                )

                LaunchedEffect(Unit) {
                    delay(100)
                    contentVisible = true
                }

                var showAnimatedSplash by remember { mutableStateOf(playAnimatedSplash) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha },
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (showSetupScreen == null) {
                            SetupGateLoadingScreen()
                        } else {
                            AnimatedContent(
                                targetState = showSetupScreen,
                                transitionSpec = {
                                    if (targetState) {
                                        fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                                    } else {
                                        scaleIn(initialScale = 0.95f, animationSpec = tween(450)) + fadeIn(animationSpec = tween(450)) togetherWith
                                                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(450)) + fadeOut(animationSpec = tween(450))
                                    }
                                },
                                label = "SetupTransition"
                            ) { shouldShowSetup ->
                                if (shouldShowSetup) {
                                    SetupScreen(onSetupComplete = {
                                    })
                                } else {
                                    MainAppContent(playerViewModel, mainViewModel)
                                }
                            }
                        }

                        if (showCrashReportDialog && crashLogData != null) {
                            CrashReportDialog(
                                crashLog = crashLogData!!,
                                onDismiss = {
                                    CrashHandler.clearCrashLog()
                                    crashLogData = null
                                    showCrashReportDialog = false
                                }
                            )
                        }
                    }

                    if (showAnimatedSplash) {
                        CarpeLinesSplash(
                            systemSplashGone = systemSplashGone.value,
                            onFinished = { showAnimatedSplash = false }
                        )
                    }
                }
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when {
            intent.action == MainActivityIntentContract.ACTION_OPEN_UPDATES -> {
                _pendingUpdatesNavigation.value = true
                intent.action = null
            }

            intent.action == android.content.Intent.ACTION_VIEW &&
                intent.data?.scheme in setOf("carpelines", "betterlyrics") -> {
                _pendingDeepLinkIntent.value = intent
                intent.action = null
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun SetupGateLoadingScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(M3ESpacing.extraExtraLarge),
            contentAlignment = Alignment.Center
        ) {
            M3ELoadingState(label = stringResource(R.string.setup_gate_preparing))
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @Composable
    private fun MainAppContent(
        playerViewModel: PlayerViewModel,
        mainViewModel: MainViewModel
    ) = traceSection("MainActivity.MainAppContent") {
        val navController = rememberNavController()
        val pendingDeepLinkIntent by _pendingDeepLinkIntent.collectAsStateWithLifecycle()
        LaunchedEffect(pendingDeepLinkIntent) {
            val intent = pendingDeepLinkIntent ?: return@LaunchedEffect
            // The NavHost is composed after the initial sync gate, so wait for its graph.
            navController.currentBackStackEntryFlow.first()
            if (navController.handleDeepLink(intent)) {
                _pendingDeepLinkIntent.value = null
            }
        }
        val isSyncing by mainViewModel.isSyncing.collectAsStateWithLifecycle()
        val hasCompletedInitialSync by mainViewModel.hasCompletedInitialSync.collectAsStateWithLifecycle()
        val syncProgress by mainViewModel.syncProgress.collectAsStateWithLifecycle()

        val context = LocalContext.current
        // One snackbar host owns every transient message in the shell: sync failures, player
        // results and per-screen confirmations all flow through it instead of a platform toast.
        val appMessageHost = rememberAppMessageHost()
        LaunchedEffect(appMessageHost, context) {
            mainViewModel.syncFailed.collect {
                appMessageHost.show(context.getString(R.string.sync_failed_toast))
            }
        }
        LaunchedEffect(appMessageHost, playerViewModel) {
            // Player results used to be emitted into a flow nobody collected.
            playerViewModel.toastEvents.collect { message -> appMessageHost.show(message) }
        }
        // Dismissing the mini player is reversible, so it gets the M3 undo snackbar instead of a
        // bespoke bar: the action stays reachable by screen readers and the timeout is the
        // platform's, not a hand-rolled countdown.
        val playerUiState by playerViewModel.playerUiState.collectAsStateWithLifecycle()
        LaunchedEffect(appMessageHost, playerUiState.showDismissUndoBar) {
            if (!playerUiState.showDismissUndoBar) return@LaunchedEffect
            val result = appMessageHost.snackbarHostState.showSnackbar(
                message = context.getString(R.string.playlist_dismissed_message),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                playerViewModel.undoDismissPlaylist()
            }
        }

        val pendingUpdatesNav by _pendingUpdatesNavigation.collectAsStateWithLifecycle()
        LaunchedEffect(pendingUpdatesNav) {
            if (!pendingUpdatesNav) return@LaunchedEffect
            // The NavHost may not be attached yet when the activity is cold-started from the notification.
            repeat(50) {
                if (navController.navigateSafely(Screen.Updates.route)) {
                    _pendingUpdatesNavigation.value = false
                    return@LaunchedEffect
                }
                delay(100)
            }
            _pendingUpdatesNavigation.value = false
        }

        var canShowLoadingIndicator by remember { mutableStateOf(false) }
        var loadingShownTimestamp by remember { mutableStateOf(0L) }
        val minimumDisplayDuration = 1500L

        val shouldPotentiallyShowLoading = isSyncing && !hasCompletedInitialSync

        LaunchedEffect(shouldPotentiallyShowLoading) {
            if (shouldPotentiallyShowLoading) {
                delay(300L)
                if (mainViewModel.isSyncing.value && !mainViewModel.hasCompletedInitialSync.value) {
                    canShowLoadingIndicator = true
                    loadingShownTimestamp = System.currentTimeMillis()
                }
            } else {
                if (canShowLoadingIndicator && loadingShownTimestamp > 0) {
                    val elapsed = System.currentTimeMillis() - loadingShownTimestamp
                    val remaining = minimumDisplayDuration - elapsed
                    if (remaining > 0) {
                        delay(remaining)
                    }
                }
                canShowLoadingIndicator = false
                loadingShownTimestamp = 0L
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalAppMessageHost provides appMessageHost) {
                MainUI(playerViewModel, navController, appMessageHost.snackbarHostState)
            }

            if (canShowLoadingIndicator) {
                LoadingOverlay(syncProgress)
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @Composable
    private fun MainUI(
        playerViewModel: PlayerViewModel,
        navController: NavHostController,
        snackbarHostState: androidx.compose.material3.SnackbarHostState
    ) = traceSection("MainActivity.MainUI") {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        // One source of truth for the window class: the same token set every screen uses for its
        // page margins and readable content width.
        val layoutMetrics = rememberM3ELayoutMetrics()
        val useNavigationRail = layoutMetrics.useNavigationRail

        val routesWithHiddenNavigationBar = remember {
            setOf(
                // The lyrics page is a reading surface: its chrome owns the whole window, so the
                // bar stays out of the way in the overview and in immersive mode alike. Pushed
                // detail pages do the same; the rail stays on large screens.
                Screen.NowPlayingLyrics.route,
                Screen.PlaylistDetail.route,
                Screen.EditTransition.route,
                Screen.About.route,
                Screen.Licenses.route,
                Screen.Recap.route,
                Screen.LyricsSettings.route,
                Screen.LyricsProviderSettings.route,
                Screen.LyricsSourceOrder.route,
                Screen.BetterLyricsSearch.route,
                Screen.SettingsCategory.route,
                Screen.Updates.route
            )
        }
        val shouldHideNavigationBar by remember(currentRoute) {
            derivedStateOf {
                currentRoute?.let { route ->
                    routesWithHiddenNavigationBar.any { hiddenRoute ->
                        // Declared routes keep their arguments in "{...}" form, while the back
                        // stack entry reports either that same pattern or a concrete route
                        // ("lyrics_settings?highlightKey=font", "settings_category/appearance").
                        // Strip the dynamic segment, then the optional query, so every flavour of
                        // the same destination collapses to one comparable prefix.
                        val optionalPrefix = hiddenRoute.substringBefore("{").trimEnd('?', '/')
                        val pathPrefix = optionalPrefix.substringBefore('?').trimEnd('/')
                        route == hiddenRoute ||
                            route == optionalPrefix ||
                            route.startsWith("$optionalPrefix?") ||
                            route.startsWith("$optionalPrefix/") ||
                            route == pathPrefix ||
                            route.startsWith("$pathPrefix?") ||
                            route.startsWith("$pathPrefix/")
                    }
                } ?: false
            }
        }
        // The lyrics page and the recap story give up the rail as well as the bar: both own the
        // whole window on every size class. Pushed detail pages keep the rail, since that is
        // their only visible way back on a wide window.
        val recapRoutePrefix = Screen.Recap.route.substringBefore("{")
        val shouldHideNavigationRail = currentRoute == Screen.NowPlayingLyrics.route ||
            currentRoute == Screen.Recap.route ||
            currentRoute?.startsWith(recapRoutePrefix) == true

        val hapticsEnabled by playerViewModel.hapticsEnabled.collectAsStateWithLifecycle()
        val rootView = LocalView.current
        val platformHapticFeedback = LocalHapticFeedback.current
        val appHapticsConfig = remember(hapticsEnabled) {
            AppHapticsConfig(enabled = hapticsEnabled)
        }
        val scopedHapticFeedback = remember(platformHapticFeedback, appHapticsConfig.enabled) {
            if (appHapticsConfig.enabled) platformHapticFeedback else NoOpHapticFeedback
        }

        LaunchedEffect(hapticsEnabled, rootView) {
            rootView.isHapticFeedbackEnabled = hapticsEnabled
            rootView.rootView?.isHapticFeedbackEnabled = hapticsEnabled
        }

        LaunchedEffect(userPreferencesRepository) {
            userPreferencesRepository.clearDeprecatedPlayerSheetPreference()
        }

        val showScrollbar by userPreferencesRepository.showScrollbarFlow.collectAsStateWithLifecycle(initialValue = true)

        CompositionLocalProvider(
            LocalAppHapticsConfig provides appHapticsConfig,
            LocalHapticFeedback provides scopedHapticFeedback,
            LocalShowScrollbar provides showScrollbar
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = useNavigationRail && !shouldHideNavigationRail,
                    enter = fadeIn(animationSpec = tween(220)) +
                        expandHorizontally(
                            animationSpec = tween(
                                durationMillis = 320,
                                easing = FastOutSlowInEasing
                            ),
                            expandFrom = Alignment.Start
                        ),
                    exit = fadeOut(animationSpec = tween(180)) +
                        shrinkHorizontally(
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutLinearInEasing
                            ),
                            shrinkTowards = Alignment.Start
                        )
                ) {
                    PlayerNavigationRail(
                        navController = navController,
                        navItems = MainNavItems,
                        currentRoute = currentRoute
                    )
                }

                Scaffold(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        // The bar collapses instead of vanishing: detail screens used to make it
                        // pop out of existence the moment the destination changed, which read as a
                        // layout error rather than a navigation.
                        AnimatedVisibility(
                            visible = !useNavigationRail && !shouldHideNavigationBar,
                            enter = slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 320,
                                    easing = FastOutSlowInEasing
                                ),
                                initialOffsetY = { it / 2 }
                            ) + expandVertically(
                                animationSpec = tween(
                                    durationMillis = 320,
                                    easing = FastOutSlowInEasing
                                ),
                                expandFrom = Alignment.Bottom
                            ) + fadeIn(animationSpec = tween(220)),
                            exit = slideOutVertically(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = FastOutLinearInEasing
                                ),
                                targetOffsetY = { it / 2 }
                            ) + shrinkVertically(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = FastOutLinearInEasing
                                ),
                                shrinkTowards = Alignment.Bottom
                            ) + fadeOut(animationSpec = tween(180))
                        ) {
                            FixedNavigationBar(
                                navController = navController,
                                currentRoute = currentRoute
                            )
                        }
                    }
                ) { innerPadding ->
                    AppNavigation(
                        playerViewModel = playerViewModel,
                        navController = navController,
                        paddingValues = innerPadding,
                        userPreferencesRepository = userPreferencesRepository
                    )
                }
            }
        }
    }

    @Composable
    private fun FixedNavigationBar(
        navController: NavHostController,
        currentRoute: String?
    ) {
        val systemNavBarInset = sanitizeNavigationBarBottomInset(
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        )
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = NavBarOuterHorizontalPadding,
                        end = NavBarOuterHorizontalPadding,
                        top = NavBarOuterTopPadding,
                        bottom = NavBarOuterBottomPadding + systemNavBarInset
                    ),
                contentAlignment = Alignment.Center
            ) {
                // The pill stays compact and grows around the active destination's label, so the
                // shell only margins it and lets the bar measure itself.
                PlayerInternalNavigationBar(
                    navController = navController,
                    navItems = MainNavItems,
                    currentRoute = currentRoute,
                    compactMode = false,
                    bottomBarPadding = systemNavBarInset
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun LoadingOverlay(syncProgress: SyncProgress) {
        val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
            targetValue = syncProgress.progress,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            ),
            label = "SyncProgressAnimation"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                .clickable(enabled = false, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            // The loading state announces itself as a live region and reports progress to screen
            // readers, which a bare indicator plus a label does not.
            M3ELoadingState(
                label = if (syncProgress.hasProgress) {
                    stringResource(
                        R.string.sync_scanned_count,
                        syncProgress.currentCount,
                        syncProgress.totalCount
                    )
                } else {
                    stringResource(R.string.sync_preparing_library)
                },
                progress = if (syncProgress.hasProgress) animatedProgress else null,
                modifier = Modifier.padding(horizontal = M3ESpacing.extraExtraLarge)
            )
        }
    }


    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        LogUtils.d(this, "onStart")
        playerViewModel.onMainActivityStart()
    }

    override fun onStop() {
        super.onStop()
        LogUtils.d(this, "onStop")
    }

    override fun onResume() {
        super.onResume()
    }

}
