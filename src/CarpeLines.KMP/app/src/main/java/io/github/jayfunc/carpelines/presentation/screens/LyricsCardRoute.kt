package io.github.jayfunc.carpelines.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor
import io.github.jayfunc.carpelines.presentation.components.lyrics.card.LyricsCardScreen
import io.github.jayfunc.carpelines.presentation.components.lyrics.rememberArtworkBitmap
import io.github.jayfunc.carpelines.presentation.components.lyrics.rememberLyricsDisplaySettings
import io.github.jayfunc.carpelines.presentation.viewmodel.NowPlayingLyricsViewModel

/**
 * The lyric card page's data layer.
 *
 * The card is built from the very same track the lyrics page is showing, so this route reads the
 * media session and the lyrics view model rather than accepting a title and a lyric list as route
 * arguments: the card would otherwise be a snapshot that goes stale while it is open.
 */
@Composable
fun LyricsCardRoute(
    initialIndex: Int,
    onBack: () -> Unit,
    lyricsViewModel: NowPlayingLyricsViewModel,
) {
    val lyricsUi by lyricsViewModel.state.collectAsStateWithLifecycle()
    val externalMedia by ExternalMediaMonitor.state.collectAsStateWithLifecycle()
    val settings = rememberLyricsDisplaySettings()
    val header = resolveHeader(externalMedia, lyricsUi.localArtworkModel)
    val artwork = rememberArtworkBitmap(header?.artworkBitmap, header?.artworkUri)

    LyricsCardScreen(
        title = header?.title.orEmpty(),
        artist = header?.artist.orEmpty(),
        artwork = artwork,
        // The page's accent is the scheme's own primary, exactly what the lyrics page calls its
        // accent, so a template's highlight cannot drift from the rest of the app.
        accent = MaterialTheme.colorScheme.primary,
        lyrics = lyricsUi.lyrics,
        initialIndex = initialIndex,
        showBackground = settings.showBackgroundVocals,
        showTranslation = settings.showTranslation,
        showRomanization = settings.showRomanization,
        onBack = onBack,
    )
}
