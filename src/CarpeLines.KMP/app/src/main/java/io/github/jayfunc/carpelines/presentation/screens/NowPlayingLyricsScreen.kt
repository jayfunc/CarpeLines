package io.github.jayfunc.carpelines.presentation.screens

import android.content.ClipData
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor
import io.github.jayfunc.carpelines.data.external.ExternalMediaState
import io.github.jayfunc.carpelines.data.model.Lyrics
import io.github.jayfunc.carpelines.data.model.SyncedLine
import io.github.jayfunc.carpelines.presentation.components.AutoScrollingText
import io.github.jayfunc.carpelines.presentation.components.LyricsClipboardText
import io.github.jayfunc.carpelines.presentation.components.SmartImage
import io.github.jayfunc.carpelines.presentation.components.SyncedLyricsList
import io.github.jayfunc.carpelines.presentation.components.WavySliderExpressive
import io.github.jayfunc.carpelines.presentation.components.betterlyrics.BetterLyricsAmbientBackground
import io.github.jayfunc.carpelines.presentation.components.lyrics.KeepLyricsScreenOn
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsAlignment
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsLineActionsSheet
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsOffsetStore
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsSystemBarsAppearance
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsTextColorMode
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsTextColors
import io.github.jayfunc.carpelines.presentation.components.lyrics.card.LyricsShareSheet
import io.github.jayfunc.carpelines.presentation.components.lyrics.rememberArtworkPalette
import io.github.jayfunc.carpelines.presentation.components.lyrics.rememberLyricsChromeState
import io.github.jayfunc.carpelines.presentation.components.lyrics.rememberLyricsDisplaySettings
import io.github.jayfunc.carpelines.presentation.components.lyrics.resolveLyricsTextColors
import io.github.jayfunc.carpelines.presentation.components.lyrics.scaledForLyrics
import io.github.jayfunc.carpelines.presentation.components.player.AnimatedPlaybackControls
import io.github.jayfunc.carpelines.presentation.viewmodel.NowPlayingLyricsViewModel
import io.github.jayfunc.carpelines.presentation.viewmodel.hasContent
import io.github.jayfunc.carpelines.ui.m3e.M3EButtonGroup
import io.github.jayfunc.carpelines.ui.m3e.M3EEmptyState
import io.github.jayfunc.carpelines.ui.m3e.M3EErrorState
import io.github.jayfunc.carpelines.ui.m3e.M3ELoadingState
import io.github.jayfunc.carpelines.ui.m3e.M3ESecondaryButton
import io.github.jayfunc.carpelines.ui.m3e.M3ETonalButton
import io.github.jayfunc.carpelines.ui.theme.LocalReducedMotion
import io.github.jayfunc.carpelines.ui.theme.M3EMotion
import io.github.jayfunc.carpelines.ui.theme.emphasized
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * The page's single side margin. The identity (title, artist, source pill), the seek line and the
 * transport all sit on it, and the lyrics are lined up to it rather than given a margin of their
 * own — one left edge for the whole page, taken from the track title.
 */
private val LyricsPageInset = 24.dp

/**
 * The karaoke view insets every line by 16 dp on its own, so the lyrics band backs exactly that
 * out of [LyricsPageInset]: the rendered words then start where the title starts. Plain lyrics are
 * drawn by this file and use [LyricLineInset] as their own margin instead.
 */
private val LyricLineInset = 16.dp

private data class LyricsHeader(
    /** Stable per-track identity, so a pause/play or artwork refresh does not replay the transition. */
    val key: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkModel: Any?,
    val artworkBitmap: Bitmap?,
)

/**
 * The lyrics page. Its default face is the overview: the lyrics run from the top of the page down
 * to a bottom stack of the track identity — a small cover beside the title and artist — the
 * progress line and the transport controls. The immersive timer, when it is switched on, dismisses
 * everything but the lyrics; tapping the lyrics brings the overview back.
 */
@Composable
fun NowPlayingLyricsScreen(
    paddingValues: PaddingValues = PaddingValues(),
    lyricsViewModel: NowPlayingLyricsViewModel = hiltViewModel(),
    onOpenLyricsSettings: () -> Unit = {},
    onOpenManualLyricsSearch: () -> Unit = {},
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val externalMedia by ExternalMediaMonitor.state.collectAsStateWithLifecycle()
    val monitorConnected by ExternalMediaMonitor.connected.collectAsStateWithLifecycle()
    // The raw sample is handed to the overview separately from the value used for the progress
    // bar: the overview line needs a per-frame position, the progress bar only a per-second one.
    val sampledPosition = ExternalMediaMonitor.positionMs.collectAsStateWithLifecycle()
    val positionMs by sampledPosition
    val lyricsUi by lyricsViewModel.state.collectAsStateWithLifecycle()
    val sourcePreference by lyricsViewModel.sourcePreference.collectAsStateWithLifecycle()
    val settings = rememberLyricsDisplaySettings()

    val header = resolveHeader(externalMedia, lyricsUi.localArtworkModel)
    val lyrics: Lyrics? = lyricsUi.lyrics
    val isLoading = lyricsUi.loading
    val isPlaying = externalMedia?.isPlaying == true
    val playbackSpeed = externalMedia?.playbackSpeed ?: 1f
    val durationMs = externalMedia?.durationMs ?: 0L

    // The immersive-lyrics preference is an explicit request to get the chrome out of the way:
    // once it is on, the overview leaves by itself after the delay the user picked. It does not
    // depend on the media session's playback state — some players publish paused/none while audio
    // is running — so the timer always starts once the lyrics are actually there.
    val hasLyricsContent = lyrics?.hasContent() == true
    val chrome = rememberLyricsChromeState(
        autoHideEnabled = hasLyricsContent && settings.immersiveLyricsEnabled,
        autoHideMillis = settings.immersiveLyricsTimeoutMs,
    )

    LaunchedEffect(externalMedia?.identity, externalMedia?.durationMs, lyricsViewModel.orderKey) {
        lyricsViewModel.resolveExternal(externalMedia)
    }

    var hasNotificationAccess by remember {
        mutableStateOf(ExternalMediaMonitor.hasNotificationAccess(context))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasNotificationAccess = ExternalMediaMonitor.hasNotificationAccess(context)
        if (hasNotificationAccess) ExternalMediaMonitor.ensureConnected(context)
    }

    // The listener can stay unbound after an app update, force-stop or ROM restriction even
    // though notification access is still granted. Retry once when the screen settles.
    LaunchedEffect(hasNotificationAccess, monitorConnected) {
        if (hasNotificationAccess && !monitorConnected) {
            delay(1_200)
            if (!ExternalMediaMonitor.connected.value) {
                ExternalMediaMonitor.reconnect(context)
            }
        }
    }

    // Per-track calibration, for playback CarpeLines does not own.
    val trackKey = remember(header?.title, header?.artist) {
        LyricsOffsetStore.trackKey(header?.title.orEmpty(), header?.artist.orEmpty())
    }
    var syncOffsetMs by remember(trackKey) { mutableStateOf(0) }
    LaunchedEffect(trackKey) {
        LyricsOffsetStore.offset(context, trackKey).collectLatest { syncOffsetMs = it }
    }

    // The last cover's palette is kept while the next one loads, but the no-media states must
    // not be tinted by a song that has already ended.
    val palette = rememberArtworkPalette(header?.artworkBitmap)?.takeIf { header != null }
    val colors = resolveLyricsTextColors(palette, MaterialTheme.colorScheme)
    // `KaraokeLyricsView.textColor` is a single colour for every lyric layer, so the override is
    // resolved once here instead of per line.
    val lyricContentColor = when (settings.visual.textColorMode) {
        LyricsTextColorMode.ARTWORK -> colors.played
        LyricsTextColorMode.THEME -> MaterialTheme.colorScheme.onSurface
        LyricsTextColorMode.LIGHT -> Color.White
        LyricsTextColorMode.DARK -> Color(0xFF101014)
    }
    // The bar goes away for the whole page, not just the lyrics-only face: the lyrics reach the top
    // of the page, and system icons over them read as dirt on the words.
    LyricsSystemBarsAppearance(isLightBackground = colors.isLightBackground)

    // The lyrics-only face sits one step inside the page, so Back steps out of it instead of
    // leaving the lyrics page.
    BackHandler(enabled = header != null && !chrome.visible) {
        chrome.show()
    }

    val progress by animateFloatAsState(
        targetValue = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
        label = "lyricsProgress",
    )

    val baseLyricTypography = MaterialTheme.typography.titleLarge
    // No text shadow: at the dim opacity an unplayed line uses, a 14 px blur behind every glyph
    // reads as a grey halo around the words — the "dirty" page. Contrast comes from the scrim
    // below and from the type colour itself, which is how the WinUI page reads too.
    val lyricStyle = remember(
        settings.fontSizeSp,
        settings.visual.lineHeightScale,
        baseLyricTypography,
        settings.visual.fontWeight,
        settings.visual.fontFamily,
    ) {
        baseLyricTypography
            .scaledForLyrics(settings.fontSizeSp, settings.visual.lineHeightScale)
            .copy(
                fontWeight = settings.visual.fontWeight.weight,
                fontFamily = settings.visual.fontFamily.resolve(baseLyricTypography.fontFamily),
            )
    }

    var actionLine by remember { mutableStateOf<SyncedLine?>(null) }
    var shareInitialIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (settings.ambientEnabled) {
            BetterLyricsAmbientBackground(
                style = settings.ambientStyle,
                albumArtModel = header?.artworkModel,
                colorScheme = MaterialTheme.colorScheme,
                palette = palette,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Switched off in settings: fall back to the plain theme surface, no gradient at all.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            )
        }

        // A fixed scrim under the lyrics, not a per-line shadow: it darkens the whole column evenly,
        // so the words keep their edge on a busy cover and the page keeps one flat tone.
        LyricsScrim(colors = colors, modifier = Modifier.fillMaxSize())

        // No track, no lyrics page: the permission / reconnect / empty states take the screen.
        if (header == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = 24.dp,
                        bottom = paddingValues.calculateBottomPadding()
                    )
            ) {
                when {
                    !hasNotificationAccess -> M3EEmptyState(
                        icon = Icons.Rounded.NotificationsActive,
                        title = stringResource(R.string.external_lyrics_permission_title),
                        description = stringResource(R.string.external_lyrics_permission_body),
                        action = {
                            M3ETonalButton(
                                label = stringResource(R.string.external_lyrics_grant_access),
                                onClick = {
                                    ExternalMediaMonitor.openNotificationAccessSettings(context)
                                },
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                    )

                    !monitorConnected -> M3EErrorState(
                        icon = Icons.Rounded.LinkOff,
                        title = stringResource(R.string.external_lyrics_connecting_media_title),
                        description = stringResource(R.string.external_lyrics_connecting_media_body),
                        action = {
                            // A rebind is not always enough: after an app update some ROMs keep the
                            // listener unbound until the permission is toggled, so the settings page
                            // has to be one tap away instead of being described in the copy.
                            M3EButtonGroup {
                                M3ETonalButton(
                                    label = stringResource(R.string.external_lyrics_reconnect),
                                    onClick = {
                                        hasNotificationAccess =
                                            ExternalMediaMonitor.hasNotificationAccess(context)
                                        ExternalMediaMonitor.reconnect(context)
                                    },
                                )
                                M3ESecondaryButton(
                                    label = stringResource(R.string.external_lyrics_open_access_settings),
                                    onClick = {
                                        ExternalMediaMonitor.openNotificationAccessSettings(context)
                                    },
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                    )

                    else -> M3EEmptyState(
                        icon = Icons.Rounded.MusicOff,
                        title = stringResource(R.string.external_lyrics_no_media_title),
                        description = stringResource(R.string.external_lyrics_no_media_body),
                        action = {
                            M3ETonalButton(
                                label = stringResource(R.string.external_lyrics_no_media_check_again),
                                onClick = {
                                    hasNotificationAccess =
                                        ExternalMediaMonitor.hasNotificationAccess(context)
                                    ExternalMediaMonitor.reconnect(context)
                                },
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                    )
                }
            }
        } else {
            LyricsOverviewPanel(
                header = header,
                chromeVisible = chrome.visible,
                progress = progress,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                elapsedLabel = formatPlaybackTime(positionMs),
                durationLabel = formatPlaybackTime(durationMs),
                durationMs = durationMs,
                colors = colors,
                onShowChrome = chrome::show,
                // The hold on play/pause is the second way into the lyrics-only face, next to
                // the auto-hide timer.
                onHideChrome = { chrome.hide() },
                onPrevious = {
                    ExternalMediaMonitor.previous()
                    chrome.hold()
                },
                onPlayPause = {
                    ExternalMediaMonitor.playPause()
                    chrome.hold()
                },
                onNext = {
                    ExternalMediaMonitor.next()
                    chrome.hold()
                },
                onSeek = { targetMs ->
                    ExternalMediaMonitor.seekTo(targetMs)
                    chrome.hold()
                },
                // The lyrics are one view in both faces: the overview shows them in the middle
                // band of the page, and hiding the chrome simply gives them the whole height.
                lyricsContent = { lyricsModifier ->
                    when {
                        lyrics?.synced?.isNotEmpty() == true -> {
                            val synced = remember(lyrics) { lyrics.synced.orEmpty().toImmutableList() }
                            SyncedLyricsList(
                                lines = synced,
                                playbackPositionFlow = ExternalMediaMonitor.positionMs,
                                isPlaying = isPlaying,
                                playbackSpeed = playbackSpeed,
                                lyricsSyncOffset = syncOffsetMs,
                                contentColor = lyricContentColor,
                                textStyle = lyricStyle,
                                accentColor = colors.accent,
                                onLineClick = { line ->
                                    ExternalMediaMonitor.seekTo(
                                        io.github.jayfunc.carpelines.presentation.components.resolveSeekPositionMs(
                                            lineTimeMs = line.time.toLong(),
                                            lyricsSyncOffsetMs = syncOffsetMs,
                                        )
                                    )
                                    chrome.hold()
                                },
                                onLineLongClick = { line ->
                                    actionLine = line
                                    chrome.hold()
                                },
                                playingLineTopOffset = settings.playingLineTopOffset,
                                animatedLyricsBlurEnabled = settings.animatedLyricsBlurEnabled,
                                animatedLyricsBlurStrengthDp = settings.animatedLyricsBlurStrengthDp,
                                showTranslation = settings.showTranslation,
                                showRomanization = settings.showRomanization,
                                duetLayoutEnabled = settings.duetLayoutEnabled,
                                showBackgroundVocals = settings.showBackgroundVocals,
                                visual = settings.visual,
                                modifier = lyricsModifier,
                            )
                        }

                        lyrics?.plain?.isNotEmpty() == true -> PlainLyrics(
                            lyrics = lyrics,
                            textStyle = lyricStyle,
                            lyricsAlignment = settings.visual.alignment,
                            bottomPadding = 32.dp,
                            contentColor = lyricContentColor,
                            modifier = lyricsModifier,
                        )

                        isLoading -> M3ELoadingState(
                            label = stringResource(R.string.lyrics_loading),
                            modifier = lyricsModifier,
                        )

                        // Anything left over is a settled miss — including a local file that parsed
                        // to nothing. It must never fall back to the loading indicator: that is what
                        // made the page spin forever while the header already said "Local".
                        else -> {
                            // The miss replaces the spinner after an asynchronous search, so it
                            // fades in instead of appearing as a hard cut.
                            var errorVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { errorVisible = true }
                            AnimatedVisibility(
                                visible = errorVisible,
                                enter = fadeIn(tween(360)),
                                modifier = lyricsModifier,
                            ) {
                                M3EErrorState(
                                    icon = Icons.Rounded.SearchOff,
                                    title = stringResource(R.string.external_lyrics_not_found),
                                    description = stringResource(R.string.lyrics_not_found_local_hint),
                                    action = {
                                        M3EButtonGroup {
                                            M3ETonalButton(
                                                label = stringResource(R.string.external_lyrics_retry),
                                                onClick = { lyricsViewModel.retry(externalMedia) },
                                            )
                                            M3ESecondaryButton(
                                                label = stringResource(R.string.lyrics_line_actions_manual_search),
                                                onClick = onOpenManualLyricsSearch,
                                            )
                                        }
                                    },
                                    modifier = lyricsModifier,
                                )
                            }
                        }
                    }
                },
                bottomPadding = paddingValues.calculateBottomPadding(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    actionLine?.let { line ->
        LyricsLineActionsSheet(
            lineText = LyricsClipboardText.syncedLine(
                line,
                includeBackground = settings.showBackgroundVocals,
            ).replace('\n', ' '),
            offsetMs = syncOffsetMs,
            onDismiss = { actionLine = null },
            onCopyLine = {
                val text = LyricsClipboardText.syncedLine(
                    line,
                    includeBackground = settings.showBackgroundVocals,
                )
                actionLine = null
                scope.launch {
                    runCatching {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("CarpeLines lyrics", text))
                        )
                    }
                }
            },
            onSharePage = {
                val synced = lyrics?.synced.orEmpty()
                val index = synced.indexOfFirst { it.time == line.time }
                shareInitialIndex = index.coerceAtLeast(0)
                actionLine = null
                chrome.hold()
            },
            onOffsetChange = { targetMs ->
                scope.launch {
                    syncOffsetMs = LyricsOffsetStore.set(context, trackKey, targetMs)
                }
            },
            onResetOffset = {
                scope.launch {
                    syncOffsetMs = LyricsOffsetStore.clear(context, trackKey)
                }
            },
            onRefreshLyrics = {
                actionLine = null
                lyricsViewModel.retry(externalMedia)
            },
            onManualSearch = {
                actionLine = null
                onOpenManualLyricsSearch()
            },
            onOpenLyricsSettings = {
                actionLine = null
                onOpenLyricsSettings()
            },
            sourcePreference = sourcePreference,
            onSourcePreferenceChange = lyricsViewModel::setSourcePreference,
            providerLabel = lyricsUi.providerLabel
                ?.takeIf { it.isNotBlank() },
        )
    }

    shareInitialIndex?.let { initialIndex ->
        LyricsShareSheet(
            title = header?.title.orEmpty(),
            artist = header?.artist.orEmpty(),
            artwork = header?.artworkBitmap,
            accent = colors.accent,
            lyrics = lyrics,
            initialIndex = initialIndex,
            showBackground = settings.showBackgroundVocals,
            showTranslation = settings.showTranslation,
            showRomanization = settings.showRomanization,
            onDismiss = { shareInitialIndex = null },
        )
    }
}

/**
 * The one lyrics page, in both of its faces. With the chrome up it is the overview: the lyrics fill
 * the page from the top down, and everything the page *has* to say about the track sits at the
 * bottom in one stack — the cover shrunk to the height of two text lines beside the title and the
 * artist, the seek line under them, and the transport last. With the chrome away it is the
 * immersive face: the very same page, the very same lyrics view, only with that stack gone and the
 * lyrics given the whole height. That is why stepping into immersive lyrics reads as the controls
 * getting out of the way rather than as one screen replacing another.
 *
 * The cover is the page's one togglable element. Tapped, it trades places with the lyrics: it flies
 * from the identity row to a square in the middle of the band while the words fade and the title
 * slides into the space it left; tapped again, the flight runs backwards. It is the same view with
 * the same artwork throughout, only re-laid out, which is what keeps the change legible.
 *
 * That trade belongs to the overview only. The immersive face is the lyrics, whichever size the
 * cover was left at, so stepping into it from the focus simply puts the words back — and the focus
 * is remembered rather than dropped, so bringing the chrome back brings the cover back up with it.
 *
 * Everything the original full player offered that CarpeLines cannot (queue, favourite, repeat —
 * playback belongs to another player) is left out.
 */
@Composable
private fun LyricsOverviewPanel(
    header: LyricsHeader,
    chromeVisible: Boolean,
    progress: Float,
    isPlaying: Boolean,
    playbackSpeed: Float,
    elapsedLabel: String,
    durationLabel: String,
    durationMs: Long,
    colors: LyricsTextColors,
    onShowChrome: () -> Unit,
    onHideChrome: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    lyricsContent: @Composable (Modifier) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val identityArtworkSize = rememberLyricsIdentityArtworkSize(header.artist.isNotBlank())
    val identityArtworkSizePx = with(density) { identityArtworkSize.toPx() }
    val identitySlotWidthPx = with(density) {
        (identityArtworkSize + M3ESpacing.medium).toPx()
    }

    // The cover lives in two places — a thumbnail next to the title while the lyrics are the page,
    // and the page's subject while they are not. It is one view in both: only the rectangle it
    // occupies changes, and that rectangle is what is animated. The image never swaps out, so the
    // eye follows the same cover moving up and growing instead of one picture replacing another.
    var artworkFocused by remember { mutableStateOf(false) }
    // The stage belongs to the overview, so the focus only *applies* while the chrome is up: the
    // immersive face is always the lyrics. The flag itself is kept rather than cleared, which is
    // what brings the cover back onto the stage when the chrome returns.
    val coverOnStage = artworkFocused && chromeVisible
    // The band's tap shield follows the stage and outlives it by one animation: while the cover
    // shrinks back into the row the words are still translucent, and a tap on them would seek
    // invisibly.
    var artworkShield by remember { mutableStateOf(false) }
    LaunchedEffect(coverOnStage) { if (coverOnStage) artworkShield = true }
    // A new track opens on its lyrics.
    LaunchedEffect(header.key) {
        artworkFocused = false
        artworkShield = false
    }
    // Nothing below reads this value in composition on purpose. Every consumer — the cover's
    // rectangle, its corner, the text's offset, the words' alpha — reads it inside a layout or a
    // layer block, so a frame of the flight costs a measure and a layer update instead of
    // rebuilding the page (the karaoke list, the slider and the transport) sixty times a second.
    val focusProgress = animateFloatAsState(
        targetValue = if (coverOnStage) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (coverOnStage) 420 else 340,
            easing = if (coverOnStage) {
                M3EMotion.Easings.emphasizedDecelerate
            } else {
                M3EMotion.Easings.emphasizedAccelerate
            },
        ),
        label = "lyricsArtworkFocus",
        finishedListener = { progress -> if (progress <= 0.001f) artworkShield = false },
    )
    // The cover is chrome in both of its sizes, so it leaves the page with the rest of it — the
    // immersive face has no artwork on it at all.
    val chromeAlpha = animateFloatAsState(
        targetValue = if (chromeVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (chromeVisible) 300 else 200,
            easing = if (chromeVisible) {
                M3EMotion.Easings.emphasizedDecelerate
            } else {
                M3EMotion.Easings.emphasizedAccelerate
            },
        ),
        label = "lyricsArtworkChromeAlpha",
    )
    // The two rectangles the cover flies between, in root coordinates: its place in the identity
    // row and the stage in the lyrics band. Both are measured rather than computed — they depend on
    // typography, insets and the chrome's own height, none of which this file should re-derive.
    var panelOrigin by remember { mutableStateOf(Offset.Zero) }
    var thumbnailRect by remember { mutableStateOf(Rect.Zero) }
    var stageRect by remember { mutableStateOf(Rect.Zero) }

    Box(modifier = modifier.onGloballyPositioned { panelOrigin = it.positionInRoot() }) {
        // The lyrics own everything above the identity, so the page has nothing to clear up there
        // but the status bar — and this page hides it, which leaves the words the room the cover
        // used to take. The cutout survives the hiding, so the inset is taken from the safe
        // drawing area rather than from the bar alone.
        val lyricTopInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() +
            M3ESpacing.medium
        // The band keeps a constant top edge and only grows downwards: re-insetting the top would
        // re-wrap the lines every time the chrome left, which reads as the page flickering.
        val lyricBottomInset by animateDpAsState(
            targetValue = bottomPadding +
                if (chromeVisible) M3ESpacing.extraLarge else M3ESpacing.small,
            animationSpec = M3EMotion.spatial(),
            label = "overviewLyricsBottomInset",
        )

        // The transport and the elapsed part of the seek bar take the app's theme colour instead
        // of the monochrome ink used for the lyrics, so the controls read as chrome rather than
        // as part of the artwork tint.
        val haptics = LocalHapticFeedback.current
        val accent = MaterialTheme.colorScheme.primary
        val accentContent = MaterialTheme.colorScheme.onPrimary
        val skipContainer = accent.copy(alpha = 0.16f)
        val remainingTrack = accent.copy(alpha = 0.35f)

        // One driver for the whole entrance: every stage slices the same 0..1 value, so the
        // panel arrives as a single gesture instead of six independent fades. The value is read
        // in the layer block, which keeps the 560 ms choreography out of recomposition.
        val reducedMotion = LocalReducedMotion.current
        val reveal = remember { Animatable(if (reducedMotion) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (reducedMotion) {
                reveal.snapTo(1f)
            } else {
                reveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 460,
                        easing = M3EMotion.Easings.emphasizedDecelerate,
                    ),
                )
            }
        }
        fun Modifier.revealStage(
            start: Float,
            end: Float,
            rise: Dp = 12.dp,
            scaleFrom: Float = 1f,
        ): Modifier = graphicsLayer {
            val raw = ((reveal.value - start) / (end - start)).coerceIn(0f, 1f)
            val staged = M3EMotion.Easings.emphasizedDecelerate.transform(raw)
            alpha = staged
            translationY = (1f - staged) * rise.toPx()
            if (scaleFrom < 1f) {
                val scale = scaleFrom + (1f - scaleFrom) * staged
                scaleX = scale
                scaleY = scale
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = lyricTopInset)
                // The controls clear the gesture bar by a full spacing step instead of hugging it:
                // at 8 dp they read as part of the system bar rather than as the app's controls.
                .padding(bottom = lyricBottomInset),
        ) {
            // The lyrics are the page in both faces: the whole page above the identity while the
            // chrome is up, the whole page once it steps away. It is one view in both states, so
            // nothing gets swapped out — the list simply grows into the space the chrome leaves.
            // Taps the lines do not claim bring the chrome back; the lines keep their own seek and
            // long-press.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // The tap target covers the whole band, side margins included; the margins
                    // themselves belong to the content.
                    .then(
                        if (chromeVisible) {
                            Modifier
                        } else {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onShowChrome,
                            )
                        }
                    )
                    // The lyrics are lined up to the same edge as the title and the seek line: the
                    // band only carries the part of the margin the karaoke view does not add
                    // itself. Not animated with the chrome — the text would re-wrap while it moved.
                    .padding(horizontal = (LyricsPageInset - LyricLineInset)),
            ) {
                // The words fade rather than being removed: the karaoke list keeps its place, its
                // scroll and its follow state, and the cover simply takes the light from it. The
                // modifier is remembered so the fade is read in the layer, not in composition —
                // the list must not be rebuilt sixty times a second while it fades.
                val lyricFade = remember {
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 1f - focusProgress.value }
                }
                lyricsContent(lyricFade)

                // The stage the cover takes over: the band's own rectangle, squared off and inset
                // from every edge. This box draws nothing and never leaves the layout — it is the
                // target of the flight, so it has to stay measurable in both faces.
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    // A square that fits the band on its narrow side, one inset off each edge.
                    val stageSide = (minOf(maxWidth, maxHeight) - ArtworkStageInset * 2)
                        .takeIf { it > 0.dp } ?: 0.dp
                    Box(
                        modifier = Modifier
                            .size(stageSide)
                            .onGloballyPositioned { stageRect = it.boundsInRoot() },
                    )
                }

                // A faded-out lyrics column still reads taps, and a tap beside the cover would
                // seek in them invisibly. The band listens for the way back instead — or, with the
                // chrome away, for the way back to the chrome, so the focus itself survives the
                // immersive face.
                if (artworkShield) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (chromeVisible) {
                                        artworkFocused = false
                                    } else {
                                        onShowChrome()
                                    }
                                },
                            ),
                    )
                }
            }

            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(
                    tween(240, delayMillis = 120, easing = M3EMotion.Easings.emphasizedDecelerate)
                ) + expandVertically(
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = 60,
                        easing = M3EMotion.Easings.emphasizedDecelerate,
                    ),
                    expandFrom = Alignment.Bottom,
                ),
                exit = fadeOut(tween(120, easing = M3EMotion.Easings.emphasizedAccelerate)) +
                    shrinkVertically(
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = M3EMotion.Easings.emphasizedAccelerate,
                        ),
                        shrinkTowards = Alignment.Bottom,
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LyricsPageInset),
                ) {
                    Spacer(Modifier.height(M3ESpacing.medium))

                    // The identity belongs to the transport now: the lyrics own the page above it,
                    // so the track's name sits with the controls it describes, its cover scaled
                    // down to the two lines of text beside it. It crossfades only when the session
                    // actually moves to another track, and leaves with the rest of the chrome.
                    AnimatedContent(
                        targetState = header,
                        contentKey = { it.key },
                        transitionSpec = {
                            fadeIn(tween(300, easing = LinearEasing)) togetherWith
                                fadeOut(tween(300, easing = LinearEasing))
                        },
                        label = "lyricsIdentity",
                        modifier = Modifier.fillMaxWidth(),
                    ) { track ->
                        val hasArtist = track.artist.isNotBlank()
                        Row(
                            // Fades only, no rise: the cover is drawn outside this row and
                            // tracks its layout position, so a translated row would leave the
                            // artwork behind for the length of the entrance.
                            modifier = Modifier
                                .fillMaxWidth()
                                .revealStage(0.10f, 0.56f, rise = 0.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // The cover itself is drawn above the page (see the flight below), so
                            // the row only keeps its place — the rectangle the cover returns to.
                            Spacer(
                                modifier = Modifier
                                    .width(identityArtworkSize + M3ESpacing.medium)
                                    .height(identityArtworkSize)
                                    .onGloballyPositioned { coordinates ->
                                        thumbnailRect = Rect(
                                            offset = coordinates.positionInRoot(),
                                            size = Size(identityArtworkSizePx, identityArtworkSizePx),
                                        )
                                    },
                            )
                            // One line each, scrolling with feathered edges when the text
                            // outgrows the row, instead of truncating behind an ellipsis.
                            //
                            // The text slides into the place the cover left by moving its layer,
                            // not by shrinking the row: re-laying the two marquees out on every
                            // frame of the flight is what made it stutter. The cost is that the
                            // line keeps the width it had beside the cover — invisible for one
                            // left-aligned line, and a marquee simply starts a step earlier.
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        translationX = -identitySlotWidthPx * focusProgress.value
                                    },
                            ) {
                                AutoScrollingText(
                                    text = track.title,
                                    style = MaterialTheme.typography.emphasized.headlineSmall
                                        .copy(color = MaterialTheme.colorScheme.primary),
                                    gradientEdgeColor = Color.Black,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (hasArtist) {
                                    Spacer(Modifier.height(M3ESpacing.extraSmall))
                                    AutoScrollingText(
                                        text = track.artist,
                                        style = MaterialTheme.typography.titleMedium
                                            .copy(color = MaterialTheme.colorScheme.primary),
                                        gradientEdgeColor = Color.Black,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(M3ESpacing.medium))

                    // Scrubbing holds the slider until the player reports the committed position,
                    // so a seek does not visibly snap back for a tick.
                    var sliderPosition by remember { mutableStateOf(progress) }
                    var isUserScrubbing by remember { mutableStateOf(false) }
                    var pendingSeekFraction by remember { mutableFloatStateOf(-1f) }
                    var pendingSeekAtMs by remember { mutableLongStateOf(0L) }

                    LaunchedEffect(header.title, header.artist) {
                        isUserScrubbing = false
                        pendingSeekFraction = -1f
                        sliderPosition = progress
                    }
                    LaunchedEffect(progress, isUserScrubbing) {
                        if (isUserScrubbing) return@LaunchedEffect
                        if (pendingSeekFraction >= 0f) {
                            val settled = abs(progress - pendingSeekFraction) < 0.04f ||
                                System.currentTimeMillis() - pendingSeekAtMs > 5_000L
                            if (!settled) return@LaunchedEffect
                            pendingSeekFraction = -1f
                        }
                        sliderPosition = progress
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .revealStage(0.40f, 0.88f, rise = 8.dp)
                    ) {
                        WavySliderExpressive(
                            value = { sliderPosition },
                            enabled = durationMs > 0L,
                            onValueChange = { newValue ->
                                isUserScrubbing = true
                                sliderPosition = newValue.coerceIn(0f, 1f)
                            },
                            onValueCommit = { finalValue ->
                                val finalFraction = finalValue.coerceIn(0f, 1f)
                                sliderPosition = finalFraction
                                onSeek((finalFraction * durationMs).roundToLong())
                                pendingSeekFraction = finalFraction
                                pendingSeekAtMs = System.currentTimeMillis()
                                isUserScrubbing = false
                            },
                            activeTrackColor = accent,
                            inactiveTrackColor = remainingTrack,
                            thumbColor = accent,
                            wavelength = 30.dp,
                            isPlaying = isPlaying,
                            semanticsLabel = stringResource(R.string.cd_playback_position),
                        )
                    }
                    Spacer(Modifier.height(M3ESpacing.small))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .revealStage(0.44f, 0.92f, rise = 6.dp)
                    ) {
                        Text(
                            text = elapsedLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.translation,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = durationLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.translation,
                        )
                    }

                    Spacer(Modifier.height(M3ESpacing.large))

                    val controlAnimationSpec = remember {
                        spring<Float>(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        )
                    }
                    AnimatedPlaybackControls(
                        modifier = Modifier
                            .fillMaxWidth()
                            .revealStage(0.50f, 1f, rise = 10.dp)
                            .padding(horizontal = 12.dp),
                        isPlayingProvider = { isPlaying },
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        height = 76.dp,
                        pressAnimationSpec = controlAnimationSpec,
                        colorOtherButtons = skipContainer,
                        colorPlayPause = accent,
                        tintPlayPauseIcon = accentContent,
                        tintOtherIcons = accent,
                        colorPreviousButton = skipContainer,
                        colorNextButton = skipContainer,
                        tintPreviousIcon = accent,
                        tintNextIcon = accent,
                        // The second way into the lyrics-only face, next to the auto-hide timer:
                        // a hold on play/pause takes the chrome away and leaves the words.
                        onPlayPauseLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHideChrome()
                        },
                        playPauseLongClickLabel = stringResource(R.string.lyrics_overview_lyrics_only),
                    )
                }
            }
        }

        // The cover, drawn once for both of its sizes and above the page. Its bounds are the
        // animation: the identity row's square while the lyrics are the page, the stage's square
        // while they are not, and the interpolation between the two. Everything else in the change
        // — the words fading, the text sliding left, the corner rounding out — hangs off the same
        // progress value, which is why the piece reads as one movement instead of four.
        //
        // The bounds are applied in the layout phase and the rest in a single layer, so the flight
        // never touches composition. Until the two rectangles have been measured it simply has no
        // size — and the entrance alpha keeps that first frame invisible.
        Box(
            modifier = Modifier
                .offset {
                    val rect = lerpRect(thumbnailRect, stageRect, focusProgress.value)
                    IntOffset(
                        (rect.left - panelOrigin.x).roundToInt(),
                        (rect.top - panelOrigin.y).roundToInt(),
                    )
                }
                .layout { measurable, _ ->
                    val rect = lerpRect(thumbnailRect, stageRect, focusProgress.value)
                    val width = rect.width.roundToInt().coerceAtLeast(0)
                    val height = rect.height.roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) { placeable.place(0, 0) }
                }
                .graphicsLayer {
                    val entrance = M3EMotion.Easings.emphasizedDecelerate.transform(
                        ((reveal.value - 0.10f) / 0.46f).coerceIn(0f, 1f)
                    )
                    val focus = focusProgress.value
                    alpha = entrance * chromeAlpha.value
                    // 8 dp as a thumbnail, 20 dp as the page's subject.
                    shape = M3EShape.rounded(
                        ArtworkThumbnailCorner +
                            (ArtworkStageCorner - ArtworkThumbnailCorner) * focus
                    )
                    clip = true
                }
                .then(
                    if (chromeVisible) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            // Toggling is an interaction with the chrome, so it restarts the
                            // auto-hide timer instead of racing it.
                            onClick = {
                                artworkFocused = !artworkFocused
                                artworkShield = true
                                onShowChrome()
                            },
                        )
                    } else {
                        // With the chrome away the cover is off the page with it, and a tap
                        // belongs to bringing the chrome back — the band underneath listens.
                        Modifier
                    },
                ),
        ) {
            LyricsIdentityArtwork(track = header, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * The track's cover, drawn at whatever rectangle the panel gives it.
 *
 * The panel owns the size — the identity row's square in one state, the stage in the other — so this
 * view only has to fill the box it is put in. That is what lets one instance of the cover carry both
 * states instead of being swapped for a second image mid-flight.
 */
@Composable
private fun LyricsIdentityArtwork(track: LyricsHeader, modifier: Modifier = Modifier) {
    if (track.artworkModel != null) {
        SmartImage(
            model = track.artworkModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        // A track without a cover keeps the same silhouette, so the page does not open a hole
        // where the artwork should be.
        Box(
            modifier = modifier.background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surface,
                    )
                )
            ),
        )
    }
}

/** Corner of the cover while it is the row's thumbnail. */
private val ArtworkThumbnailCorner = 8.dp

/** Corner of the cover while it holds the stage: one step softer, as a larger surface reads. */
private val ArtworkStageCorner = 20.dp

/** Free space the cover keeps from the lyrics band's edges once it takes the stage. */
private val ArtworkStageInset = 24.dp

/** Straight-line interpolation of two rectangles, so a size and a position move as one. */
private fun lerpRect(start: Rect, stop: Rect, fraction: Float): Rect = Rect(
    left = start.left + (stop.left - start.left) * fraction,
    top = start.top + (stop.top - start.top) * fraction,
    right = start.right + (stop.right - start.right) * fraction,
    bottom = start.bottom + (stop.bottom - start.bottom) * fraction,
)

/**
 * Side of the identity's cover: exactly the title, the gap and the artist — the height of the two
 * lines it is placed beside.
 *
 * It is derived from the typography instead of measured from the composed text, because those two
 * lines are [AutoScrollingText]s: they measure their content in a subcomposition, so they cannot
 * answer an intrinsic-height query that the row would need.
 */
@Composable
private fun rememberLyricsIdentityArtworkSize(hasArtist: Boolean): Dp {
    val density = LocalDensity.current
    val titleStyle = MaterialTheme.typography.emphasized.headlineSmall
    val artistStyle = MaterialTheme.typography.titleMedium
    return remember(density, titleStyle, artistStyle, hasArtist) {
        with(density) {
            lineSpan(titleStyle) +
                if (hasArtist) M3ESpacing.extraSmall + lineSpan(artistStyle) else 0.dp
        }
    }
}

/** Rendered height of one line of [style] on its own metrics. */
private fun Density.lineSpan(style: TextStyle): Dp {
    val lineHeight = style.lineHeight
    if (lineHeight.isSpecified) return lineHeight.toDp()
    val fontSize = style.fontSize
    return if (fontSize.isSpecified) (fontSize.value * 1.3f).sp.toDp() else 20.dp
}

/**
 * Even wash behind the lyrics. It takes the polarity of the text instead of the theme's surface:
 * a light page needs white behind dark words, a dark page needs black behind light ones, and either
 * way the album art still shows through.
 */
@Composable
private fun LyricsScrim(colors: LyricsTextColors, modifier: Modifier = Modifier) {
    val base = if (colors.isLightBackground) Color.White else Color.Black
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0.0f to base.copy(alpha = 0.34f),
                0.28f to base.copy(alpha = 0.16f),
                0.62f to base.copy(alpha = 0.20f),
                1.0f to base.copy(alpha = 0.44f),
            )
        )
    )
}

private fun resolveHeader(
    externalMedia: ExternalMediaState?,
    localArtworkModel: Any?,
): LyricsHeader? {
    if (externalMedia == null || !externalMedia.hasMetadata) return null
    return LyricsHeader(
        key = externalMedia.identity,
        title = externalMedia.title,
        artist = externalMedia.artist,
        album = externalMedia.album,
        artworkModel = externalMedia.artworkBitmap ?: externalMedia.artworkUri ?: localArtworkModel,
        artworkBitmap = externalMedia.artworkBitmap,
    )
}

private fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

@Composable
private fun PlainLyrics(
    lyrics: Lyrics,
    textStyle: TextStyle,
    lyricsAlignment: LyricsAlignment,
    bottomPadding: androidx.compose.ui.unit.Dp,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val textAlign = lyricsAlignment.toTextAlign()
    LazyColumn(
        modifier = modifier,
        // Plain lines are drawn here, not by the karaoke view, so they carry the full line inset
        // themselves; the band already supplies the rest of the page margin. The top inset is
        // local: the band starts right under the status bar, and the first line needs an edge.
        contentPadding = PaddingValues(
            start = LyricLineInset,
            end = LyricLineInset,
            top = 48.dp,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(lyrics.plain.orEmpty()) { line ->
            Text(
                text = line,
                style = textStyle,
                color = contentColor,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
