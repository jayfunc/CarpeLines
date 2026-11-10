package io.github.jayfunc.carpelines.presentation.navigation

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.jayfunc.carpelines.R
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import io.github.jayfunc.carpelines.data.preferences.LaunchTab
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.presentation.screens.BetterLyricsSearchScreen
import io.github.jayfunc.carpelines.presentation.screens.CoverSourceSettingsScreen
import io.github.jayfunc.carpelines.presentation.screens.NowPlayingLyricsScreen
import io.github.jayfunc.carpelines.presentation.screens.UpdatesScreen
import io.github.jayfunc.carpelines.presentation.model.SettingsCategory
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.jayfunc.carpelines.presentation.screens.LyricsCardRoute
import io.github.jayfunc.carpelines.presentation.screens.LyricsManagementSettingsScreen
import io.github.jayfunc.carpelines.presentation.screens.LyricsSettingsScreen
import io.github.jayfunc.carpelines.presentation.screens.HomeScreen

import io.github.jayfunc.carpelines.presentation.screens.AboutScreen
import io.github.jayfunc.carpelines.presentation.screens.LicensesScreen
import io.github.jayfunc.carpelines.presentation.screens.StatsScreen
import io.github.jayfunc.carpelines.presentation.screens.SettingsScreen
import io.github.jayfunc.carpelines.presentation.screens.SettingsCategoryScreen
import io.github.jayfunc.carpelines.presentation.viewmodel.PlayerViewModel
import io.github.jayfunc.carpelines.presentation.viewmodel.RecapViewModel
import io.github.jayfunc.carpelines.presentation.recap.RecapScreen
import kotlinx.coroutines.flow.first
import io.github.jayfunc.carpelines.presentation.components.ScreenWrapper
import io.github.jayfunc.carpelines.presentation.settings.search.ProvideSettingsHighlight
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(UnstableApi::class)
@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNavigation(
    playerViewModel: PlayerViewModel,
    navController: NavHostController,
    paddingValues: PaddingValues,
    userPreferencesRepository: UserPreferencesRepository
) {
    var launchRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        launchRoute = userPreferencesRepository.launchTabFlow
            .first()
            .toRoute()
    }

    launchRoute?.let { initialRoute ->
        // The default tab is a destination inside the app, not the root of the stack: it is opened
        // on top of Home so Back (and the navigation bar) can always return to the library. Using
        // it as NavHost's start destination would leave Home out of the stack entirely, and the
        // lyrics page hides every navigation affordance, trapping the user there.
        val opensOnDefaultTab = initialRoute != Screen.Home.route
        // The opening push is not a user gesture: Home must not flash first, and the tab
        // transition must not play over the launch fade. Cleared once the default tab is shown.
        val suppressOpeningTransition = remember(initialRoute) {
            mutableStateOf(opensOnDefaultTab)
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            composable(
                Screen.Home.route,
                enterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = enterTransition(),
                        suppressed = suppressOpeningTransition.value
                    )
                },
                exitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = exitTransition()
                    )
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popExitTransition()
                    )
                },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    HomeScreen(
                        navController = navController,
                        paddingValuesParent = paddingValues,
                        playerViewModel = playerViewModel
                    )
                }
            }
            composable(
                Screen.Settings.route,
                enterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = enterTransition(),
                        suppressed = suppressOpeningTransition.value
                    )
                },
                exitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = exitTransition()
                    )
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popExitTransition()
                    )
                },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    SettingsScreen(
                        navController = navController,
                        playerViewModel = playerViewModel,
                        paddingValues = paddingValues
                    )
                }
            }
            composable(
                route = Screen.SettingsCategory.route,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.StringType },
                    navArgument("highlightKey") { type = NavType.StringType; nullable = true; defaultValue = null }
                ),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    val categoryId = backStackEntry.arguments?.getString("categoryId")
                    val highlightKey = backStackEntry.arguments?.getString("highlightKey")
                    if (categoryId != null) {
                        ProvideSettingsHighlight(highlightKey) {
                            SettingsCategoryScreen(
                                categoryId = categoryId,
                                highlightKey = highlightKey,
                                navController = navController,
                                playerViewModel = playerViewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
            composable(
                route = Screen.LyricsSettings.route,
                arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true; defaultValue = null }),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val highlightKey = backStackEntry.arguments?.getString("highlightKey")
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    ProvideSettingsHighlight(highlightKey) {
                        LyricsSettingsScreen(
                            navController = navController,
                            highlightKey = highlightKey
                        )
                    }
                }
            }
            composable(
                route = Screen.LyricsManagement.route,
                arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true; defaultValue = null }),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val highlightKey = backStackEntry.arguments?.getString("highlightKey")
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    ProvideSettingsHighlight(highlightKey) {
                        LyricsManagementSettingsScreen(
                            navController = navController,
                            highlightKey = highlightKey
                        )
                    }
                }
            }
            composable(
                route = Screen.CoverSources.route,
                arguments = listOf(navArgument("highlightKey") { type = NavType.StringType; nullable = true; defaultValue = null }),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val highlightKey = backStackEntry.arguments?.getString("highlightKey")
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    ProvideSettingsHighlight(highlightKey) {
                        CoverSourceSettingsScreen(
                            navController = navController,
                            highlightKey = highlightKey
                        )
                    }
                }
            }
            composable(
                route = Screen.BetterLyricsSearch.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    BetterLyricsSearchScreen(
                        navController = navController
                    )
                }
            }
            composable(
                route = Screen.NowPlayingLyrics.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "carpelines://lyrics" },
                    navDeepLink { uriPattern = "carpelines://external_lyrics" },
                    navDeepLink { uriPattern = "betterlyrics://lyrics" },
                    navDeepLink { uriPattern = "betterlyrics://external_lyrics" },
                ),
                enterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = enterTransition(),
                        suppressed = suppressOpeningTransition.value
                    )
                },
                exitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = exitTransition()
                    )
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popExitTransition()
                    )
                },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    NowPlayingLyricsScreen(
                        paddingValues = paddingValues,
                        onOpenLyricsSettings = {
                            navController.navigate(Screen.LyricsSettings.createRoute())
                        },
                        onOpenLyricsManagement = {
                            navController.navigateSafely(Screen.LyricsManagement.route)
                        },
                        onOpenManualLyricsSearch = {
                            navController.navigate(Screen.BetterLyricsSearch.route)
                        },
                        onOpenShareCard = { initialIndex ->
                            navController.navigateSafely(
                                Screen.LyricsCard.createRoute(initialIndex)
                            )
                        },
                    )
                }
            }
            composable(
                route = Screen.LyricsCard.route,
                arguments = listOf(navArgument("initialIndex") { type = NavType.IntType }),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val initialIndex = backStackEntry.arguments?.getInt("initialIndex") ?: 0
                // The card is built from the track the lyrics page is already showing, so it reads
                // that page's view model instead of resolving a second copy of the same lyrics.
                val lyricsEntry = remember(backStackEntry) {
                    runCatching {
                        navController.getBackStackEntry(Screen.NowPlayingLyrics.route)
                    }.getOrNull()
                }
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    if (lyricsEntry != null) {
                        LyricsCardRoute(
                            initialIndex = initialIndex,
                            onBack = { navController.popBackStack() },
                            lyricsViewModel = hiltViewModel(lyricsEntry),
                        )
                    }
                }
            }
            composable(
                Screen.Stats.route,
                enterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = enterTransition(),
                        suppressed = suppressOpeningTransition.value
                    )
                },
                exitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = exitTransition()
                    )
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popExitTransition()
                    )
                },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    StatsScreen(
                        navController = navController,
                        paddingValues = paddingValues,
                        isRootDestination = true
                    )
                }
            }
            composable(
                route = Screen.Recap.route,
                arguments = listOf(navArgument(RecapViewModel.YEAR_ARG) { type = NavType.IntType }),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    RecapScreen(
                        navController = navController,
                        playerViewModel = playerViewModel
                    )
                }
            }
            composable(
                Screen.About.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    AboutScreen(
                        navController = navController,
                        viewModel = playerViewModel,
                        onNavigationIconClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                Screen.Licenses.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    LicensesScreen(
                        onNavigationIconClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                Screen.Updates.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    UpdatesScreen(
                        onBack = { navController.popBackStack() },
                        onOpenBackup = {
                            navController.navigateSafely(
                                Screen.SettingsCategory.createRoute(
                                    SettingsCategory.BACKUP_RESTORE.id,
                                    highlightKey = "item_backup_export"
                                )
                            )
                        }
                    )
                }
            }

        }

        if (opensOnDefaultTab) {
            val currentEntry by navController.currentBackStackEntryAsState()

            LaunchedEffect(navController, initialRoute) {
                // NavHost installs the graph from its own effect, so the first entry has to exist
                // before the default tab can be pushed on top of it.
                navController.currentBackStackEntryFlow.first()
                // Only an untouched Home page gets the default tab: a restored stack (process
                // death), a deep link or a pending-navigation request already picked a page.
                val stillOnHome = navController.previousBackStackEntry == null &&
                    navController.currentBackStackEntry?.destination?.route == Screen.Home.route
                if (!stillOnHome) {
                    suppressOpeningTransition.value = false
                    return@LaunchedEffect
                }
                navController.navigate(initialRoute) {
                    launchSingleTop = true
                }
            }

            LaunchedEffect(currentEntry?.id) {
                // The opening push has already captured the suppressed transition by the time
                // composition settles on the default tab; from here on, navigation animates.
                if (currentEntry?.destination?.route == initialRoute) {
                    suppressOpeningTransition.value = false
                }
            }
        }
    }
}

private fun String.toRoute(): String = when (this) {
    LaunchTab.LYRICS -> Screen.NowPlayingLyrics.route
    LaunchTab.STATS -> Screen.Stats.route
    else -> Screen.Home.route
}

private enum class MainRootDirection {
    FORWARD,
    BACKWARD
}

private val BottomNavEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * M3 "fade through" between peer destinations.
 *
 * Switching a bottom-navigation tab is not a hierarchy move, so it must not push the screen
 * sideways: the outgoing page fades out quickly, and the incoming one fades in over a slightly
 * scaled-up frame. Detail pushes keep the shared-axis slide in [enterTransition].
 */
private val MAIN_ROOT_FADE_OUT = tween<Float>(durationMillis = 100, easing = BottomNavEasing)

private val MAIN_ROOT_FADE_IN = tween<Float>(
    durationMillis = 220,
    delayMillis = 80,
    easing = BottomNavEasing
)

private val MAIN_ROOT_SCALE_IN = tween<Float>(
    durationMillis = 300,
    delayMillis = 80,
    easing = BottomNavEasing
)

private fun mainRootDirection(
    fromRoute: String?,
    toRoute: String?
): MainRootDirection? {
    val fromIndex = mainRootRouteIndex(fromRoute) ?: return null
    val toIndex = mainRootRouteIndex(toRoute) ?: return null
    if (fromIndex == toIndex) return null
    return if (toIndex > fromIndex) MainRootDirection.FORWARD else MainRootDirection.BACKWARD
}

private fun mainRootEnterTransition(
    fromRoute: String?,
    toRoute: String?,
    fallback: EnterTransition,
    suppressed: Boolean = false
): EnterTransition {
    if (suppressed) return EnterTransition.None
    return when (mainRootDirection(fromRoute, toRoute)) {
        null -> fallback
        else -> fadeIn(animationSpec = MAIN_ROOT_FADE_IN) +
            androidx.compose.animation.scaleIn(
                animationSpec = MAIN_ROOT_SCALE_IN,
                initialScale = 0.96f
            )
    }
}

private fun mainRootExitTransition(
    fromRoute: String?,
    toRoute: String?,
    fallback: ExitTransition
): ExitTransition = when (mainRootDirection(fromRoute, toRoute)) {
    null -> fallback
    else -> fadeOut(animationSpec = MAIN_ROOT_FADE_OUT)
}
