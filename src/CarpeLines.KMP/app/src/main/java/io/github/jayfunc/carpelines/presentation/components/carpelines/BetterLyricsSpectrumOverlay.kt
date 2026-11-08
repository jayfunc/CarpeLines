package io.github.jayfunc.carpelines.presentation.components.betterlyrics

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.data.audio.SpectrumFrame
import kotlin.math.exp
import kotlin.math.max
import kotlinx.coroutines.flow.StateFlow

/**
 * A light-weight Compose canvas version of BetterLyrics' `SpectrumRenderer`.
 *
 * The original draws Win2D geometry; here the same visual vocabulary is rebuilt from a Compose
 * `Path`: bars or a cubic curve, a vertical fade, an optional halo and a bass-driven breathing
 * scale. The data arrives at the Visualizer callback rate (usually ~20 Hz), so the canvas
 * interpolates the target values on every display frame instead of redrawing the analysis.
 */
@Composable
internal fun BetterLyricsSpectrumOverlay(
    frameFlow: StateFlow<SpectrumFrame?>,
    style: SpectrumStyle,
    placement: SpectrumPlacement,
    barCount: Int,
    opacity: Float,
    glowEnabled: Boolean,
    breathingEnabled: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val displayValues = remember(barCount) { FloatArray(barCount) }
    val displayBass = remember(barCount) { FloatArray(1) }
    var frameTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(frameFlow, barCount) {
        var previousFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val deltaSeconds = if (previousFrameNanos == 0L) {
                    0f
                } else {
                    ((frameNanos - previousFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                }
                previousFrameNanos = frameNanos

                val frame = frameFlow.value
                val attack = 1f - exp(-deltaSeconds / 0.06f)
                val release = 1f - exp(-deltaSeconds / 0.20f)
                for (index in 0 until barCount) {
                    val target = frame?.bars?.getOrNull(index) ?: 0f
                    val current = displayValues[index]
                    displayValues[index] = if (target > current) {
                        current + (target - current) * attack
                    } else {
                        current + (target - current) * release
                    }
                }

                val targetBass = frame?.bass ?: 0f
                displayBass[0] += (targetBass - displayBass[0]) * (1f - exp(-deltaSeconds / 0.10f))
                frameTick++
            }
        }
    }

    Canvas(modifier) {
        // Reading the state is what invalidates the draw on every frame; the arrays themselves
        // are plain buffers because only this draw pass reads them.
        if (frameTick < 0L) return@Canvas
        drawSpectrumBody(
            values = displayValues,
            bass = displayBass[0],
            style = style,
            placement = placement,
            opacity = opacity.coerceIn(0f, 1f),
            glowEnabled = glowEnabled,
            breathingEnabled = breathingEnabled,
            color = color,
        )
    }
}

private fun DrawScope.drawSpectrumBody(
    values: FloatArray,
    bass: Float,
    style: SpectrumStyle,
    placement: SpectrumPlacement,
    opacity: Float,
    glowEnabled: Boolean,
    breathingEnabled: Boolean,
    color: Color,
) {
    if (values.isEmpty() || size.width <= 0f || size.height <= 0f) return

    val bottomAnchored = placement == SpectrumPlacement.BOTTOM
    val pivot = Offset(size.width / 2f, if (bottomAnchored) size.height else 0f)
    val breathScale = if (breathingEnabled) {
        1f + bass.coerceIn(0f, 1f) * 0.06f
    } else {
        1f
    }

    withTransform({
        scale(breathScale, breathScale, pivot)
    }) {
        when (style) {
            SpectrumStyle.BAR -> drawBars(
                values = values,
                bottomAnchored = bottomAnchored,
                opacity = opacity,
                glowEnabled = glowEnabled,
                color = color,
            )

            SpectrumStyle.CURVE -> drawCurve(
                values = values,
                bottomAnchored = bottomAnchored,
                opacity = opacity,
                glowEnabled = glowEnabled,
                color = color,
            )
        }
    }
}

private fun DrawScope.drawBars(
    values: FloatArray,
    bottomAnchored: Boolean,
    opacity: Float,
    glowEnabled: Boolean,
    color: Color,
) {
    val count = values.size
    val gap = if (count > 1) (2.dp.toPx()).coerceAtMost(size.width / (count * 4f)) else 0f
    val barWidth = max(1f, (size.width - gap * (count - 1)) / count)
    val bodyColor = color.copy(alpha = opacity)
    val glowColor = color.copy(alpha = opacity * 0.10f)
    val bodyBrush = spectrumGradient(bodyColor, bottomAnchored)
    val glowBrush = spectrumGradient(glowColor, bottomAnchored)
    val glowPad = 3.dp.toPx()
    val maxBarHeight = size.height * 0.92f
    val minimumVisibleHeight = 1.dp.toPx()

    for (index in 0 until count) {
        val value = values[index]
        val barHeight = value * maxBarHeight
        if (barHeight <= 0f) continue

        val left = index * (barWidth + gap)
        val top = if (bottomAnchored) size.height - barHeight else 0f
        if (glowEnabled) {
            drawRect(
                brush = glowBrush,
                topLeft = Offset(left - glowPad / 2f, top - glowPad / 2f),
                size = Size(barWidth + glowPad, barHeight + glowPad),
            )
        }
        drawRect(
            brush = bodyBrush,
            topLeft = Offset(left, top),
            size = Size(barWidth, max(barHeight, minimumVisibleHeight)),
        )
    }
}

private fun DrawScope.drawCurve(
    values: FloatArray,
    bottomAnchored: Boolean,
    opacity: Float,
    glowEnabled: Boolean,
    color: Color,
) {
    val count = values.size
    if (count < 2) return

    val xs = FloatArray(count)
    val ys = FloatArray(count)
    val maxCurveHeight = size.height * 0.92f
    val spacing = size.width / (count - 1)
    val baseline = if (bottomAnchored) size.height else 0f

    for (index in 0 until count) {
        xs[index] = index * spacing
        val level = (values[index] * maxCurveHeight).coerceIn(0f, maxCurveHeight)
        ys[index] = if (bottomAnchored) size.height - level else level
    }

    val path = Path().apply {
        moveTo(xs[0], ys[0])
        for (index in 0 until count - 1) {
            val previous = max(0, index - 1)
            val next = minOf(count - 1, index + 2)
            val p0x = xs[previous]
            val p0y = ys[previous]
            val p1x = xs[index]
            val p1y = ys[index]
            val p2x = xs[index + 1]
            val p2y = ys[index + 1]
            val p3x = xs[next]
            val p3y = ys[next]

            cubicTo(
                p1x + (p2x - p0x) * 0.1666f,
                p1y + (p2y - p0y) * 0.1666f,
                p2x - (p3x - p1x) * 0.1666f,
                p2y - (p3y - p1y) * 0.1666f,
                p2x,
                p2y,
            )
        }
        lineTo(xs[count - 1], baseline)
        lineTo(xs[0], baseline)
        close()
    }

    if (glowEnabled) {
        drawPath(
            path = path,
            color = color.copy(alpha = opacity * 0.12f),
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
        )
    }

    drawPath(
        path = path,
        brush = spectrumGradient(color.copy(alpha = opacity), bottomAnchored),
    )
}

private fun DrawScope.spectrumGradient(color: Color, bottomAnchored: Boolean): Brush =
    if (bottomAnchored) {
        Brush.verticalGradient(
            0f to Color.Transparent,
            1f to color,
            startY = 0f,
            endY = size.height,
        )
    } else {
        Brush.verticalGradient(
            0f to color,
            1f to Color.Transparent,
            startY = 0f,
            endY = size.height,
        )
    }
