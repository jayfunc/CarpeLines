package io.github.jayfunc.carpelines.presentation.screens

import android.content.ClipData
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.window.core.layout.WindowSizeClass
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
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsTextColors
import io.github.jayfunc.carpelines.presentation.components.lyrics.rememberArtworkBitmap
import io.github.jayfunc.carpelines.presentation.components.lyrics.rememberArtworkPalette
import io.github.jayfunc.carpelines.presentation.components.lyrics.rememberLyricsChromeState
import io.github.jayfunc.carpelines.presentation.components.lyrics.rememberLyricsDisplaySettings
import io.github.jayfunc.carpelines.presentation.components.lyrics.resolveLyricContentColor
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

/**
 * Reading width for the words on a wide page. A lyric line is followed while it is sung, so it
 * stops growing before the eye has to travel across the whole window.
 */
private val LyricsReadingWidth = 680.dp

/** The album face's control bar stops widening with the window, so its controls stay together. */
private val LyricsControlBarWidth = 720.dp

/**
 * The split's words share the window with the player card, so their column keeps a wider margin
 * than the phone page: at [LyricsPageInset] the lines crowd the card beside them and the window
 * edge behind it. The karaoke view still adds [LyricLineInset] on its own, so the band carries
 * only the difference.
 */
private val LyricsSplitPageInset = 48.dp

/** The split gives each side half the window; the card's own content stops growing before that. */
private val LyricsPlayerCardWidth = 640.dp

/** Height the album face's control band is assumed to need until it has been measured. */
private val LyricsAlbumBarReserve = 248.dp

internal data class LyricsHeader(
    /** Stable per-track identity, so a pause/play or artwork refresh does not replay the transition. */
    val key: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkModel: Any?,
    val artworkBitmap: Bitmap?,
    val artworkUri: String?,
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
    onOpenShareCard: (Int) -> Unit = {},
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

    // Width alone decides the split. A short window scrolls the player card rather than falling
    // back to the phone column, which would waste the extra width.
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val splitLayout =
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

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
    val headerArtwork = rememberArtworkBitmap(header?.artworkBitmap, header?.artworkUri)
    val palette = rememberArtworkPalette(headerArtwork)?.takeIf { header != null }
    val colors = resolveLyricsTextColors(MaterialTheme.colorScheme)
    // `KaraokeLyricsView.textColor` is a single colour for every lyric layer, so the override is
    // resolved once here instead of per line. The artwork mode takes the scheme's primary, which
    // is the colour the title and artist below the words are drawn in.
    val lyricContentColor = resolveLyricContentColor(
        mode = settings.visual.textColorMode,
        colorScheme = MaterialTheme.colorScheme,
    )
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
        baseLyricTypography,
        settings.visual.fontWeight,
        settings.visual.fontFamily,
    ) {
        baseLyricTypography
            .scaledForLyrics(settings.fontSizeSp)
            .copy(
                fontWeight = settings.visual.fontWeight.weight,
                fontFamily = settings.visual.fontFamily.resolve(baseLyricTypography.fontFamily),
            )
    }

    var actionLine by remember { mutableStateOf<SyncedLine?>(null) }

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
                    // The words arrive asynchronously — a local tag first, an online search after
                    // it — so their first appearance has to be a transition rather than a state
                    // flip: the spinner occupies this same band, and swapping the two used to
                    // read as the lyrics snapping from 0 to 1 with nothing in between.
                    val reducedMotion = LocalReducedMotion.current
                    val showsSynced = lyrics?.synced?.isNotEmpty() == true
                    val showsPlain = !showsSynced && lyrics?.plain?.isNotEmpty() == true
                    val arrival = remember { Animatable(if (reducedMotion) 1f else 0f) }
                    var wordsOnPage by remember { mutableStateOf(false) }
                    LaunchedEffect(showsSynced, showsPlain) {
                        if (!showsSynced && !showsPlain) {
                            // Whatever takes the band next (spinner, settled miss) brings its own
                            // entrance, so the words after it start from transparent again.
                            wordsOnPage = false
                            arrival.snapTo(if (reducedMotion) 1f else 0f)
                            return@LaunchedEffect
                        }
                        // An in-place swap (plain → synced for the same track) keeps the words
                        // where they are; only the first arrival fades.
                        if (wordsOnPage) return@LaunchedEffect
                        wordsOnPage = true
                        if (reducedMotion) {
                            arrival.snapTo(1f)
                        } else {
                            arrival.snapTo(0f)
                            arrival.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 320,
                                    easing = M3EMotion.Easings.emphasizedDecelerate,
                                ),
                            )
                        }
                    }
                    // Read in the layer block, not in composition: the fade then costs one layer
                    // update per frame instead of rebuilding the karaoke list.
                    val lyricsArrival = if (showsSynced || showsPlain) {
                        lyricsModifier.graphicsLayer { alpha = arrival.value }
                    } else {
                        lyricsModifier
                    }
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
                                modifier = lyricsArrival,
                            )
                        }

                        lyrics?.plain?.isNotEmpty() == true -> PlainLyrics(
                            lyrics = lyrics,
                            textStyle = lyricStyle,
                            lyricsAlignment = settings.visual.alignment,
                            bottomPadding = 32.dp,
                            contentColor = lyricContentColor,
                            modifier = lyricsArrival,
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
                splitLayout = splitLayout,
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
                actionLine = null
                chrome.hold()
                onOpenShareCard(index.coerceAtLeast(0))
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

}

/**
 * The lyrics page on a compact window, in both of its faces. A window wide enough for two panes
 * gets [LyricsWidePanel] instead. With the chrome up it is the overview: the lyrics fill the page
 * from the top down, and everything the page *has* to say about the track sits at the bottom in one
 * stack — the cover shrunk to the height of two text lines beside the title and the artist, the seek
 * line under them, and the transport last. With the chrome away it is the immersive face: the very
 * same page, the very same lyrics view, only with that stack gone and the lyrics given the whole
 * height. That is why stepping into immersive lyrics reads as the controls getting out of the way
 * rather than as one screen replacing another.
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
    splitLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    // A window wide enough for two panes gets the player card beside the words; the compact page
    // is one column with the controls at its foot.
    if (splitLayout) {
        LyricsWidePanel(
            header = header,
            chromeVisible = chromeVisible,
            progress = progress,
            isPlaying = isPlaying,
            elapsedLabel = elapsedLabel,
            durationLabel = durationLabel,
            durationMs = durationMs,
            colors = colors,
            onShowChrome = onShowChrome,
            onHideChrome = onHideChrome,
            onPrevious = onPrevious,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onSeek = onSeek,
            lyricsContent = lyricsContent,
            bottomPadding = bottomPadding,
            modifier = modifier,
        )
        return
    }

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
                    LyricsTransportChrome(
                        header = header,
                        colors = colors,
                        progress = progress,
                        isPlaying = isPlaying,
                        elapsedLabel = elapsedLabel,
                        durationLabel = durationLabel,
                        durationMs = durationMs,
                        onSeek = onSeek,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onHideChrome = onHideChrome,
                        artworkSlot = {
                            // The cover itself is drawn above the page (see the flight below),
                            // so the row only keeps its place — the rectangle it returns to.
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
                        },
                        identityShiftPx = { -identitySlotWidthPx * focusProgress.value },
                        reveal = reveal,
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
 * The track's identity, seek line and transport, with no position of their own.
 *
 * The wide page needs the same controls twice — under the cover in the split and along the
 * bottom of the album face — so they are one view the page places rather than two copies. The
 * compact page adds its cover thumbnail through [artworkSlot]; the wide page keeps the cover
 * beside the controls and passes none. [reveal] drives the compact page's entrance; the wide
 * page passes none, so its controls are simply there.
 */
@Composable
private fun LyricsTransportChrome(
    header: LyricsHeader,
    colors: LyricsTextColors,
    progress: Float,
    isPlaying: Boolean,
    elapsedLabel: String,
    durationLabel: String,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onHideChrome: () -> Unit,
    modifier: Modifier = Modifier,
    artworkSlot: (@Composable () -> Unit)? = null,
    showAlbum: Boolean = false,
    identityShiftPx: () -> Float = { 0f },
    reveal: Animatable<Float, AnimationVector1D>? = null,
) {
    // The transport and the elapsed part of the seek bar take the app's theme colour instead of
    // the monochrome ink used for the lyrics, so the controls read as chrome rather than as part
    // of the artwork tint.
    val haptics = LocalHapticFeedback.current
    val accent = MaterialTheme.colorScheme.primary
    val accentContent = MaterialTheme.colorScheme.onPrimary
    val skipContainer = accent.copy(alpha = 0.16f)
    val remainingTrack = accent.copy(alpha = 0.35f)

    fun Modifier.entrance(start: Float, end: Float, rise: Dp): Modifier =
        if (reveal == null) this else lyricsRevealStage(reveal, start, end, rise)

    Column(modifier = modifier) {
        // The identity belongs to the transport: the track's name sits with the controls it
        // describes. It crossfades only when the session actually moves to another track.
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
                // Fades only, no rise: the cover is drawn outside this row and tracks its
                // layout position, so a translated row would leave the artwork behind.
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(0.10f, 0.56f, 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                artworkSlot?.invoke()
                // One line each, scrolling with feathered edges when the text outgrows the row,
                // instead of truncating behind an ellipsis.
                //
                // The text slides into the place the cover left by moving its layer, not by
                // shrinking the row: re-laying the marquees out on every frame of the flight is
                // what made it stutter.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer { translationX = identityShiftPx() },
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
                    if (showAlbum && track.album.isNotBlank()) {
                        Spacer(Modifier.height(M3ESpacing.extraSmall))
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.translation,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                .entrance(0.40f, 0.88f, 8.dp)
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
                .entrance(0.44f, 0.92f, 6.dp)
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
                .entrance(0.50f, 1f, 10.dp)
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
            // The second way into the lyrics-only face, next to the auto-hide timer: a hold on
            // play/pause takes the chrome away and leaves the words.
            onPlayPauseLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onHideChrome()
            },
            playPauseLongClickLabel = stringResource(R.string.lyrics_overview_lyrics_only),
        )
    }
}

/** One stage of an entrance: slices 0..1 out of [reveal] into alpha and rise. */
private fun Modifier.lyricsRevealStage(
    reveal: Animatable<Float, AnimationVector1D>,
    start: Float,
    end: Float,
    rise: Dp = 12.dp,
): Modifier = graphicsLayer {
    val raw = ((reveal.value - start) / (end - start)).coerceIn(0f, 1f)
    val staged = M3EMotion.Easings.emphasizedDecelerate.transform(raw)
    alpha = staged
    translationY = (1f - staged) * rise.toPx()
}

/**
 * The wide face of the lyrics page: a player card on the left and the words on the right.
 *
 * The three faces are answers to "which thing owns the window": both (the split), the cover (the
 * album face), or the words (the lyrics-only face). The split is the page's home, so the other
 * two always return to it — the album face by tapping the cover or Back, the lyrics face by
 * tapping the words or Back.
 */
@Composable
private fun LyricsWidePanel(
    header: LyricsHeader,
    chromeVisible: Boolean,
    progress: Float,
    isPlaying: Boolean,
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
    var artworkFocused by remember { mutableStateOf(false) }
    // A new track opens on the split, like it opens on its lyrics on the compact page.
    LaunchedEffect(header.key) { artworkFocused = false }
    val coverOnStage = artworkFocused && chromeVisible

    // The tap shield outlives the flight by one animation: while the words are still translucent
    // a tap on them would seek invisibly.
    var artworkShield by remember { mutableStateOf(false) }
    LaunchedEffect(coverOnStage) { if (coverOnStage) artworkShield = true }

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
        label = "wideLyricsAlbumFocus",
        finishedListener = { if (it <= 0.001f) artworkShield = false },
    )
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
        label = "wideLyricsChromeAlpha",
    )

    // Back steps out of the album face into the split rather than off the page.
    BackHandler(enabled = coverOnStage) { artworkFocused = false }

    val density = LocalDensity.current
    var panelOrigin by remember { mutableStateOf(Offset.Zero) }
    var coverRect by remember { mutableStateOf(Rect.Zero) }
    var stageRect by remember { mutableStateOf(Rect.Zero) }
    // The album bar holds the bottom of the face, so the cover has to stop above it. Measured
    // rather than assumed: the identity can wrap and the text scale changes its height.
    var controlBarHeightPx by remember { mutableStateOf(0) }
    // Until the starting rectangle is measured the overlay stays invisible, so the first frame
    // does not flash a zero-sized cover. A separate flag rather than a comparison, because the
    // rectangle is also re-measured while the card scrolls and must not recompose the page.
    var coverMeasured by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier.onGloballyPositioned { panelOrigin = it.positionInRoot() },
    ) {
        // The two panes share the width evenly: the track and the words are peers on a tablet.
        val playerWidth = maxWidth / 2
        val lyricTopInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() +
            M3ESpacing.medium
        val lyricBottomInset = bottomPadding + M3ESpacing.medium

        val controlBarHeight = if (controlBarHeightPx > 0) {
            with(density) { controlBarHeightPx.toDp() }
        } else {
            LyricsAlbumBarReserve
        }
        // The cover's stage is the area above the control band, never behind it.
        val stageBottom = (maxHeight - controlBarHeight - M3ESpacing.extraLarge)
            .coerceAtLeast(lyricTopInset)
        val stageAreaHeight = (stageBottom - lyricTopInset).coerceAtLeast(0.dp)
        val stageSide = (minOf(maxWidth, stageAreaHeight) - ArtworkStageInset * 2)
            .coerceAtLeast(0.dp)
        val stageLeft = (maxWidth - stageSide) / 2
        val stageTop = lyricTopInset + (stageAreaHeight - stageSide) / 2

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = lyricTopInset, bottom = lyricBottomInset),
        ) {
            LyricsPlayerPane(
                visible = chromeVisible,
                width = playerWidth,
                header = header,
                colors = colors,
                progress = progress,
                isPlaying = isPlaying,
                elapsedLabel = elapsedLabel,
                durationLabel = durationLabel,
                durationMs = durationMs,
                onSeek = onSeek,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onHideChrome = onHideChrome,
                chromeAlpha = { chromeAlpha.value },
                albumFade = { focusProgress.value },
                onCoverRect = { rect ->
                    coverRect = rect
                    if (!coverMeasured && rect.width > 0f) coverMeasured = true
                },
            )

            // The words. On a wide window the reading column is already as wide as it can get, so
            // stepping into the lyrics-only face only moves the room around it. Narrower windows
            // do re-wrap, since the column is what widens.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
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
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = LyricsReadingWidth)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        // Backed out of the split's own inset for the same reason as the compact
                        // page: the karaoke view adds the line inset itself.
                        .padding(horizontal = (LyricsSplitPageInset - LyricLineInset))
                        .graphicsLayer { alpha = 1f - focusProgress.value },
                ) {
                    lyricsContent(Modifier.fillMaxSize())
                }
            }
        }

        // The album stage. Laid out even while the split is showing, so the cover never starts
        // its flight from a zero rectangle.
        Box(
            modifier = Modifier
                .offset { IntOffset(stageLeft.roundToPx(), stageTop.roundToPx()) }
                .size(stageSide)
                .onGloballyPositioned { stageRect = it.boundsInRoot() },
        )

        // A faded-out lyrics pane still reads taps, so while the cover owns the page the band
        // listens for the way back instead.
        if (artworkShield) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (coverOnStage) {
                                artworkFocused = false
                            } else {
                                onShowChrome()
                            }
                        },
                    ),
            )
        }

        // The cover, drawn once for both of its rectangles: the player card's square, the album
        // stage's square, and the interpolation between them. Everything else in the change hangs
        // off the same progress value, so it reads as one movement.
        Box(
            modifier = Modifier
                .offset {
                    val rect = lerpRect(coverRect, stageRect, focusProgress.value)
                    IntOffset(
                        (rect.left - panelOrigin.x).roundToInt(),
                        (rect.top - panelOrigin.y).roundToInt(),
                    )
                }
                .layout { measurable, _ ->
                    val rect = lerpRect(coverRect, stageRect, focusProgress.value)
                    val width = rect.width.roundToInt().coerceAtLeast(0)
                    val height = rect.height.roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) { placeable.place(0, 0) }
                }
                .graphicsLayer {
                    alpha = if (coverMeasured) chromeAlpha.value else 0f
                    val focus = focusProgress.value
                    shape = M3EShape.rounded(
                        ArtworkCardCorner +
                            (ArtworkStageCorner - ArtworkCardCorner) * focus
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
                        Modifier
                    },
                ),
        ) {
            LyricsIdentityArtwork(track = header, modifier = Modifier.fillMaxSize())
        }

        // The controls follow the cover in the draw order, but no longer sit on it: the stage
        // stops above this band, so the identity and transport read against the page, not the art.
        AnimatedVisibility(
            visible = coverOnStage,
            enter = fadeIn(
                tween(240, delayMillis = 120, easing = M3EMotion.Easings.emphasizedDecelerate)
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = M3EMotion.Easings.emphasizedDecelerate,
                ),
                initialOffsetY = { it / 4 },
            ),
            exit = fadeOut(tween(140, easing = M3EMotion.Easings.emphasizedAccelerate)) +
                slideOutVertically(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = M3EMotion.Easings.emphasizedAccelerate,
                    ),
                    targetOffsetY = { it / 4 },
                ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            LyricsTransportChrome(
                header = header,
                colors = colors,
                progress = progress,
                isPlaying = isPlaying,
                elapsedLabel = elapsedLabel,
                durationLabel = durationLabel,
                durationMs = durationMs,
                onSeek = onSeek,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onHideChrome = onHideChrome,
                modifier = Modifier
                    // Outermost, so the measurement includes the bar's own bottom padding and
                    // the stage below can stop exactly above the band.
                    .onSizeChanged { controlBarHeightPx = it.height }
                    .widthIn(max = LyricsControlBarWidth)
                    .fillMaxWidth()
                    .padding(horizontal = LyricsPageInset)
                    .padding(bottom = lyricBottomInset),
            )
        }
    }
}

/**
 * The split's player card: the cover's placeholder, the track's identity and its controls.
 *
 * The pane keeps its content at a fixed width and clips it while it slides out for the
 * lyrics-only face, so the card is never squeezed into an intermediate layout on the way.
 */
@Composable
private fun LyricsPlayerPane(
    visible: Boolean,
    width: Dp,
    header: LyricsHeader,
    colors: LyricsTextColors,
    progress: Float,
    isPlaying: Boolean,
    elapsedLabel: String,
    durationLabel: String,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onHideChrome: () -> Unit,
    chromeAlpha: () -> Float,
    albumFade: () -> Float,
    onCoverRect: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val paneWidth by animateDpAsState(
        targetValue = if (visible) width else 0.dp,
        animationSpec = tween(
            durationMillis = 320,
            easing = if (visible) {
                M3EMotion.Easings.emphasizedDecelerate
            } else {
                M3EMotion.Easings.emphasizedAccelerate
            },
        ),
        label = "wideLyricsPaneWidth",
    )
    val paneWidthPx = with(density) { paneWidth.toPx() }
    val widthPx = with(density) { width.toPx() }

    Box(
        modifier = modifier
            .width(paneWidth)
            .fillMaxHeight()
            .clipToBounds(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .requiredWidth(width)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = -(widthPx - paneWidthPx)
                    alpha = chromeAlpha() * (1f - albumFade())
                },
        ) {
            val coverSide = minOf(
                maxWidth - LyricsSplitPageInset * 2,
                maxHeight * 0.46f,
                420.dp,
            ).coerceAtLeast(0.dp)
            // Centred while it fits and scrollable when it does not — a very large text scale on
            // a short window must not clip the transport off the bottom of the card.
            // The card takes the split's page margin rather than the phone page's, so the cover
            // clears the window edge by the same distance as the words in the pane beside it.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = LyricsSplitPageInset),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = LyricsPlayerCardWidth)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(coverSide)
                            .onGloballyPositioned { onCoverRect(it.boundsInRoot()) },
                    )
                    Spacer(Modifier.height(M3ESpacing.extraLarge))
                    LyricsTransportChrome(
                        header = header,
                        colors = colors,
                        progress = progress,
                        isPlaying = isPlaying,
                        elapsedLabel = elapsedLabel,
                        durationLabel = durationLabel,
                        durationMs = durationMs,
                        onSeek = onSeek,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onHideChrome = onHideChrome,
                        showAlbum = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
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

/** Corner of the cover in the wide page's player card, where the cover is a surface of its own. */
private val ArtworkCardCorner = 16.dp

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

internal fun resolveHeader(
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
        artworkUri = externalMedia.artworkUri,
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
