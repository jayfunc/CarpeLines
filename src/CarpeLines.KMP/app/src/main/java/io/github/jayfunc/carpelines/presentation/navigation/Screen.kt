package io.github.jayfunc.carpelines.presentation.navigation

import androidx.compose.runtime.Immutable


@Immutable
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object SettingsCategory : Screen("settings_category/{categoryId}?highlightKey={highlightKey}") {
        fun createRoute(categoryId: String, highlightKey: String? = null) =
            if (highlightKey != null) "settings_category/$categoryId?highlightKey=$highlightKey" else "settings_category/$categoryId"
    }
    object NowPlayingLyrics : Screen("now_playing_lyrics")
    object LyricsCard : Screen("lyrics_card/{initialIndex}") {
        fun createRoute(initialIndex: Int) = "lyrics_card/$initialIndex"
    }
    object LyricsManagement : Screen("lyrics_management?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "lyrics_management?highlightKey=$highlightKey" else "lyrics_management"
    }
    object CoverSources : Screen("cover_sources?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "cover_sources?highlightKey=$highlightKey" else "cover_sources"
    }
    object LyricsSettings : Screen("lyrics_settings?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "lyrics_settings?highlightKey=$highlightKey" else "lyrics_settings"
    }
    object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }

    object Stats : Screen("stats")
    object Recap : Screen("recap/{year}") {
        fun createRoute(year: Int) = "recap/$year"
    }
    object EditTransition : Screen("edit_transition?playlistId={playlistId}") {
        fun createRoute(playlistId: String?) =
            if (playlistId != null) "edit_transition?playlistId=$playlistId" else "edit_transition"
    }

    object About : Screen("about")
    object Licenses : Screen("licenses")
    object BetterLyricsSearch : Screen("betterlyrics_search")
    object Updates : Screen("updates")

}
