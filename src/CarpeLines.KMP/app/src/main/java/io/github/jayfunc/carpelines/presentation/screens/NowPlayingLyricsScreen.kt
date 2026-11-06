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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

/**
 * How much of the page's entrance the cover spends fading in. It is folded into the cover's own
 * graphics layer (see [LyricsOverviewPanel]) rather than given a layer of its own.
 */
private const val ARTWORK_REVEAL_SPAN = 0.55f

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
 * The lyrics page. Its default face is the overview: the cover centred, the track identity under
 * it, and the progress line with the transport controls at the bottom. The immersive timer, when
 * it is switched on, dismisses everything but the lyrics; tapping the lyrics brings the overview
 * back.
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
    // The bar goes away for the whole page, not just the lyrics-only face: the cover is the top
    // of the page, and system icons on it read as dirt on the artwork.
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
                hasLyrics = hasLyricsContent,
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
 * The one lyrics page, in both of its faces. With the chrome up it is the overview: the cover
 * filling the top of the page — full-bleed and dissolved into the background at its bottom edge —
 * the track identity on that fade, the lyrics themselves underneath, and the seek line with the
 * transport at the bottom. With the chrome away it is the immersive face: the very same page, the
 * very same lyrics view, only with the chrome gone and the lyrics given the whole height. That is
 * why stepping into immersive lyrics reads as the controls getting out of the way rather than as
 * one screen replacing another.
 *
 * Everything the original full player offered that CarpeLines cannot (queue, favourite, repeat —
 * playback belongs to another player) is left out.
 */
@Composable
private fun LyricsOverviewPanel(
    header: LyricsHeader,
    chromeVisible: Boolean,
    hasLyrics: Boolean,
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
    BoxWithConstraints(modifier = modifier) {
        // The cover is a backdrop, not a card: it spans the full width and roughly the upper half
        // of the page, never taller than the width so a square cover is never cropped on the
        // sides. It takes the most room when there is a lyric band under it, and gives a step back
        // for the loading and "no lyrics" cards, which need that height more.
        val artworkHeight by animateDpAsState(
            targetValue = minOf(maxWidth, maxHeight * if (hasLyrics) 0.42f else 0.34f),
            animationSpec = M3EMotion.spatial(),
            label = "overviewArtworkHeight",
        )
        // The identity block bites a step into the bottom fade, so the cover and the text share
        // one edge instead of meeting at a seam. The status bar is the floor: the artwork runs
        // behind it, the text never does.
        val identityTop = maxOf(
            artworkHeight - M3ESpacing.large,
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + M3ESpacing.medium,
        )
        // The lyrics live between the identity and the transport while the chrome is up, and take
        // the page back when it steps away. Both insets animate, so the lyrics grow into the space
        // the chrome leaves instead of being re-laid out on a hard cut.
        val lyricTopInset by animateDpAsState(
            targetValue = if (chromeVisible) identityTop else 24.dp,
            animationSpec = M3EMotion.spatial(),
            label = "overviewLyricsTopInset",
        )
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

        // The artwork is the page's top half rather than a card on it: full-bleed, running behind
        // the status bar, with its bottom edge feathered away so it dissolves into the ambient
        // wash instead of ending on a hard line. It leaves with the rest of the chrome, so the
        // immersive face is this same page with the cover gone. The crossfade is keyed on the
        // track identity, so a pause/play or an artwork refresh does not replay it.
        val artworkAlpha = animateFloatAsState(
            targetValue = if (chromeVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (chromeVisible) 300 else 200,
                delayMillis = if (chromeVisible) 60 else 0,
                easing = if (chromeVisible) {
                    M3EMotion.Easings.emphasizedDecelerate
                } else {
                    M3EMotion.Easings.emphasizedAccelerate
                },
            ),
            label = "overviewArtworkAlpha",
        )
        // The entrance alpha, the chrome alpha and the feather are one layer, and the layer is
        // remembered instead of rebuilt per recomposition: three stacked graphicsLayers used to
        // let the mask drop out for a frame while the page recomposed on a position tick, which
        // showed the cover's raw bottom edge. Both alphas are read inside the layer block, so a
        // recomposition does not touch the cover at all.
        val artworkFade = remember(artworkAlpha) {
            Modifier
                .graphicsLayer {
                    val staged = (reveal.value / ARTWORK_REVEAL_SPAN).coerceIn(0f, 1f)
                    alpha = M3EMotion.Easings.emphasizedDecelerate.transform(staged) *
                        artworkAlpha.value
                    // DstIn needs a buffer of the cover's own pixels to erase from, and the clip
                    // keeps that layer alive even in the frame where both alphas are exactly 1.
                    clip = true
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                // The fade has to be a mask, not a scrim on top: DstIn erases the cover's own
                // pixels, which is what lets the background show through it.
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.00f to Color.Black,
                            0.46f to Color.Black,
                            0.70f to Color.Black.copy(alpha = 0.74f),
                            0.86f to Color.Black.copy(alpha = 0.36f),
                            1.00f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
        }
        AnimatedContent(
            targetState = header,
            contentKey = { it.key },
            transitionSpec = {
                fadeIn(tween(320, easing = LinearEasing)) togetherWith
                    fadeOut(tween(320, easing = LinearEasing))
            },
            label = "lyricsArtwork",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(artworkHeight)
                .then(artworkFade),
        ) { track ->
            if (track.artworkModel != null) {
                SmartImage(
                    model = track.artworkModel,
                    contentDescription = null,
                    shape = RectangleShape,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    // The track change is already crossfaded by AnimatedContent, outside the mask.
                    // A second fade inside it would blend the placeholder into the cover while the
                    // feathered layer is being rebuilt, which reads as a flash.
                    crossfadeDurationMillis = 0,
                )
            } else {
                // A track without artwork keeps the same silhouette instead of opening a hole
                // above the identity, so the page does not jump when a cover is missing.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = lyricTopInset)
                // The controls clear the gesture bar by a full spacing step instead of hugging it:
                // at 8 dp they read as part of the system bar rather than as the app's controls.
                .padding(bottom = lyricBottomInset),
        ) {
            // The identity rides the bottom of the fade, so the artwork reads as the page's own
            // skin rather than as an image with a caption under it. It collapses out of the way
            // with the rest of the chrome — that collapse is what moves the lyrics up — while the
            // text itself crossfades only when the session actually moves to another track.
            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(
                    tween(240, delayMillis = 90, easing = M3EMotion.Easings.emphasizedDecelerate)
                ) + expandVertically(
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = 40,
                        easing = M3EMotion.Easings.emphasizedDecelerate,
                    ),
                    expandFrom = Alignment.Top,
                ),
                exit = fadeOut(tween(120, easing = M3EMotion.Easings.emphasizedAccelerate)) +
                    shrinkVertically(
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = M3EMotion.Easings.emphasizedAccelerate,
                        ),
                        shrinkTowards = Alignment.Top,
                    ),
            ) {
                AnimatedContent(
                    targetState = header,
                    contentKey = { it.key },
                    transitionSpec = {
                        fadeIn(tween(300, easing = LinearEasing)) togetherWith
                            fadeOut(tween(300, easing = LinearEasing))
                    },
                    label = "lyricsIdentity",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LyricsPageInset),
                ) { track ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // One line each, scrolling with feathered edges when the text outgrows
                        // the row, instead of truncating a long title behind an ellipsis.
                        AutoScrollingText(
                            text = track.title,
                            style = MaterialTheme.typography.emphasized.headlineSmall
                                .copy(color = MaterialTheme.colorScheme.primary),
                            gradientEdgeColor = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .revealStage(0.10f, 0.56f, rise = 10.dp),
                        )
                        if (track.artist.isNotBlank()) {
                            Spacer(Modifier.height(M3ESpacing.extraSmall))
                            AutoScrollingText(
                                text = track.artist,
                                style = MaterialTheme.typography.titleMedium
                                    .copy(color = MaterialTheme.colorScheme.primary),
                                gradientEdgeColor = Color.Black,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .revealStage(0.16f, 0.62f, rise = 8.dp),
                            )
                        }
                    }
                }
            }

            // The lyrics are the page in both faces: the middle band while the chrome is up, the
            // whole page once it steps away. It is one view in both states, so nothing gets
            // swapped out — the list simply grows into the space the chrome leaves. Taps the lines
            // do not claim bring the chrome back; the lines keep their own seek and long-press.
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
                lyricsContent(Modifier.fillMaxSize())
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
    }
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
        // themselves; the band already supplies the rest of the page margin. Only the top inset is
        // local, small enough to still show a few lines while the chrome is up.
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
