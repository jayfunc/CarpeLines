package io.github.jayfunc.carpelines.presentation.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mocharealm.accompanist.lyrics.ui.composable.list.LyricsScrollChain
import com.mocharealm.accompanist.lyrics.ui.composable.list.rememberLyricsLazyListState
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeBreathingDotsDefaults
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsAnchor
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsFade
import io.github.jayfunc.carpelines.data.model.SyncedLine
import io.github.jayfunc.carpelines.presentation.components.lyrics.CoreLyricsRenderMapper
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsAnchorMode
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsBlendMode
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsFadeMode
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsVisualSettings
import io.github.jayfunc.carpelines.presentation.components.lyrics.lyricsRenderProfiles
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

/**
 * Synced lyrics rendered by accompanist-lyrics-ui. The player model is still the source of truth;
 * [CoreLyricsRenderMapper] rebuilds the core scene and a tap on a core line seeks the source line.
 */
@Composable
fun SyncedLyricsList(
    lines: ImmutableList<SyncedLine>,
    playbackPositionFlow: StateFlow<Long>,
    isPlaying: Boolean,
    playbackSpeed: Float,
    lyricsSyncOffset: Int,
    contentColor: Color,
    textStyle: TextStyle,
    accentColor: Color? = null,
    onLineClick: (SyncedLine) -> Unit,
    onLineLongClick: ((SyncedLine) -> Unit)? = null,
    positionOverrideMs: Long? = null,
    playingLineTopOffset: Float = 0.5f,
    animatedLyricsBlurEnabled: Boolean = true,
    animatedLyricsBlurStrengthDp: Float = 5f,
    showTranslation: Boolean = true,
    showRomanization: Boolean = true,
    duetLayoutEnabled: Boolean = true,
    showBackgroundVocals: Boolean = true,
    visual: LyricsVisualSettings = LyricsVisualSettings(),
    footer: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val sampledPlaybackPosition = playbackPositionFlow.collectAsStateWithLifecycle()
    val interpolatedPosition = rememberInterpolatedPlaybackPosition(
        sampledPosition = sampledPlaybackPosition,
        isPlaying = isPlaying,
        playbackSpeed = playbackSpeed,
        positionOverrideMs = positionOverrideMs,
    )
    val currentSyncOffset by rememberUpdatedState(lyricsSyncOffset)
    val positionProvider: () -> Int = remember(interpolatedPosition) {
        {
            (interpolatedPosition.value + currentSyncOffset)
                .coerceAtLeast(0L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        }
    }

    val rendered = remember(lines, duetLayoutEnabled, showBackgroundVocals, visual.alignment) {
        CoreLyricsRenderMapper.toRenderedLyrics(
            lines = lines,
            showBackgroundVocals = showBackgroundVocals,
            duetLayoutEnabled = duetLayoutEnabled,
            alignment = visual.alignment,
        )
    }
    if (rendered.lyrics.lines.isEmpty()) return

    // The immersive body is composed afresh every time the overview steps aside, so the list has
    // to open at the line being sung. Otherwise it starts from the first line and the follow
    // animation rolls the whole song down the screen before it settles.
    val initialItemIndex = remember(rendered.lyrics) {
        val startPositionMs = (sampledPlaybackPosition.value + currentSyncOffset).coerceAtLeast(0L)
        rendered.lines.indexOfLast { it.source.time.toLong() <= startPositionMs }
            .coerceAtLeast(0)
    }

    val baseFontSize = textStyle.fontSize.takeIf { it.isSpecified }?.value ?: 34f
    val baseLineHeightRatio = remember(textStyle) {
        val ratio = if (textStyle.fontSize.isSpecified && textStyle.lineHeight.isSpecified &&
            textStyle.fontSize.value > 0f
        ) {
            textStyle.lineHeight.value / textStyle.fontSize.value
        } else {
            1.18f
        }
        ratio.takeIf { it.isFinite() && it > 0f } ?: 1.18f
    }
    val translationStyle = remember(textStyle, visual.translationFontRatio, baseLineHeightRatio) {
        val size = baseFontSize * visual.translationFontRatio
        textStyle.copy(
            fontSize = size.sp,
            lineHeight = (size * baseLineHeightRatio).sp,
            fontWeight = FontWeight.Normal,
            textMotion = TextMotion.Animated,
        )
    }
    val phoneticStyle = remember(textStyle, visual.romanizationFontRatio, baseLineHeightRatio) {
        val size = baseFontSize * visual.romanizationFontRatio
        textStyle.copy(
            fontSize = size.sp,
            lineHeight = (size * baseLineHeightRatio).sp,
            fontWeight = FontWeight.Normal,
            textMotion = TextMotion.Static,
        )
    }
    val accompanimentStyle = remember(textStyle, visual.backgroundVocalFontRatio, baseLineHeightRatio) {
        val size = baseFontSize * visual.backgroundVocalFontRatio
        textStyle.copy(
            fontSize = size.sp,
            lineHeight = (size * baseLineHeightRatio).sp,
            textMotion = TextMotion.Animated,
        )
    }
    val normalStyle = remember(textStyle) {
        textStyle.copy(textMotion = TextMotion.Animated)
    }
    val breathingDotsDefaults = remember(
        contentColor,
        visual.gapDotsCount,
        visual.gapDotsSizeDp,
        visual.gapDotsSpacingDp,
        visual.gapDotsEnterDurationMs,
        visual.gapDotsHoldDurationMs,
        visual.gapDotsPulseDurationMs,
        visual.gapDotsExitDurationMs,
        visual.gapDotsUseAccentColor,
        accentColor,
    ) {
        KaraokeBreathingDotsDefaults(
            number = visual.gapDotsCount.coerceIn(1, 5),
            size = visual.gapDotsSizeDp.coerceIn(8f, 24f).dp,
            margin = visual.gapDotsSpacingDp.coerceIn(4f, 24f).dp,
            enterDurationMs = visual.gapDotsEnterDurationMs.coerceIn(0, 5_000),
            preExitStillDuration = visual.gapDotsHoldDurationMs.coerceIn(0, 1_500),
            preExitDipAndRiseDuration = visual.gapDotsPulseDurationMs.coerceIn(0, 6_000),
            exitDurationMs = visual.gapDotsExitDurationMs.coerceIn(0, 1_000),
            breathingDotsColor =
                if (visual.gapDotsUseAccentColor) accentColor ?: contentColor else contentColor,
        )
    }
    val followScrollSpec = remember(visual.followScrollDurationMs, visual.scrollEasing) {
        tween<Float>(
            durationMillis = visual.followScrollDurationMs.coerceIn(0, 1_200),
            easing = visual.scrollEasing.resolve(),
        )
    }
    val scrollChain = remember(
        visual.followSpringStiffness,
        visual.followSpringDamping,
        visual.followSpringCoupling,
        visual.followSpringDistanceFalloff,
        visual.followSpringMinResponse,
    ) {
        LyricsScrollChain(
            stiffness = visual.followSpringStiffness.coerceIn(1f, 1_000f),
            damping = visual.followSpringDamping.coerceIn(1f, 100f),
            coupling = visual.followSpringCoupling.coerceIn(0f, 0.95f),
            distanceFalloff = visual.followSpringDistanceFalloff.coerceIn(0f, 1f),
            minResponse = visual.followSpringMinResponse.coerceIn(0.05f, 1f),
        )
    }
    val renderProfiles = remember(
        visual.segmentation,
        visual.scaleEffectEnabled,
        visual.glowEffectEnabled,
        visual.floatEffectEnabled,
    ) {
        lyricsRenderProfiles(
            segmentation = visual.segmentation,
            scaleEffectEnabled = visual.scaleEffectEnabled,
            glowEffectEnabled = visual.glowEffectEnabled,
            floatEffectEnabled = visual.floatEffectEnabled,
        )
    }
    val blendMode = when (visual.blendMode) {
        LyricsBlendMode.NORMAL -> BlendMode.SrcOver
        LyricsBlendMode.PLUS -> BlendMode.Plus
        LyricsBlendMode.SCREEN -> BlendMode.Screen
    }
    val edgeFadeEnabled = visual.edgeFeatheringEffectEnabled
    // The core view anchors the focused line's top edge; the app setting describes its centre.
    val lineOffset = playingLineTopOffset.coerceIn(0f, 1f)
    val anchorFraction = (lineOffset - 0.05f).coerceIn(0f, 1f)
    // The core view masks the list from the screen edges, so both fades have to split at the
    // playing line. With ToAnchor/Fraction(0.5) every line above the current one sat inside the
    // top mask while the neighbours below were still fully opaque, which read as two different
    // unplayed-line alphas. Fade reach scales that split without moving it.
    val edgeFadeReach = visual.edgeFadeReach.coerceIn(0f, 1f)
    val autoTopFadeFraction = lineOffset * edgeFadeReach
    val autoBottomFadeFraction = (1f - lineOffset) * edgeFadeReach
    fun fadeFor(top: Boolean): LyricsFade {
        if (!edgeFadeEnabled) return LyricsFade.Fixed(0.dp)
        return when (visual.edgeFadeMode) {
            LyricsFadeMode.AUTO ->
                LyricsFade.Fraction(if (top) autoTopFadeFraction else autoBottomFadeFraction)
            LyricsFadeMode.FRACTION ->
                LyricsFade.Fraction(
                    (if (top) visual.topFadeFraction else visual.bottomFadeFraction)
                        .coerceIn(0f, 1f)
                )
            LyricsFadeMode.FIXED ->
                LyricsFade.Fixed(
                    (if (top) visual.topFadeDp else visual.bottomFadeDp).coerceIn(0f, 240f).dp
                )
            LyricsFadeMode.TO_ANCHOR ->
                LyricsFade.ToAnchor(
                    (if (top) visual.topFadeDp else visual.bottomFadeDp).coerceIn(0f, 240f).dp
                )
        }
    }
    val topFade = fadeFor(top = true)
    val bottomFade = fadeFor(top = false)
    val anchor = when (visual.anchorMode) {
        LyricsAnchorMode.FRACTION -> LyricsAnchor.Fraction(anchorFraction)
        LyricsAnchorMode.FIXED -> LyricsAnchor.Fixed(visual.anchorOffsetDp.coerceIn(0f, 240f).dp)
    }

    Box(modifier) {
        key(rendered.lyrics) {
            val listState = rememberLyricsLazyListState(
                initialFirstVisibleItemIndex = initialItemIndex,
            )
            KaraokeLyricsView(
                listState = listState,
                lyrics = rendered.lyrics,
                currentPosition = positionProvider,
                onLineClicked = { coreLine ->
                    rendered.lines
                        .firstOrNull { it.core === coreLine }
                        ?.let { onLineClick(it.source) }
                },
                onLinePressed = { coreLine ->
                    rendered.lines
                        .firstOrNull { it.core === coreLine }
                        ?.let { onLineLongClick?.invoke(it.source) }
                },
                modifier = Modifier.fillMaxSize(),
                normalLineTextStyle = normalStyle,
                translationTextStyle = translationStyle,
                accompanimentLineTextStyle = accompanimentStyle,
                phoneticTextStyle = phoneticStyle,
                textColor = contentColor,
                breathingDotsDefaults = breathingDotsDefaults,
                blendMode = blendMode,
                useBlurEffect = animatedLyricsBlurEnabled,
                showTranslation = showTranslation,
                showPhonetic = showRomanization,
                anchor = anchor,
                topFade = topFade,
                bottomFade = bottomFade,
                keepAliveZone = visual.keepAliveZoneDp.coerceIn(0f, 240f).dp,
                blurDelta = animatedLyricsBlurStrengthDp.coerceAtLeast(0.1f),
                renderProfiles = renderProfiles,
                itemSpacing = (baseFontSize * visual.lineSpacingFactor).dp,
                scrollAnimationSpec = followScrollSpec,
                autoScrollResumeDelayMillis = visual.scrollResumeDelayMs.coerceIn(0L, 5_000L),
                scrollChain = scrollChain,
            )
        }

        footer?.let { content ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
