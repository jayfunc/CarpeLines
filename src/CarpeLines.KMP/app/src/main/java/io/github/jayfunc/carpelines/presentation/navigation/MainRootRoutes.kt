package io.github.jayfunc.carpelines.presentation.navigation

internal fun isMainRootRoute(route: String?): Boolean = when (route) {
    Screen.Home.route,
    Screen.NowPlayingLyrics.route,
    Screen.Stats.route,
    Screen.Settings.route -> true
    else -> false
}

internal fun mainRootRouteIndex(route: String?): Int? = when (route) {
    Screen.Home.route -> 0
    Screen.NowPlayingLyrics.route -> 1
    Screen.Stats.route -> 2
    Screen.Settings.route -> 3
    else -> null
}
