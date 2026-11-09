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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.data.audio.SpectrumFrame
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
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
    /**
     * The artwork rectangle in this canvas' coordinates, read at draw time. Only
     * [SpectrumPlacement.AROUND_ALBUM_ART] needs it; the provider is invoked inside the draw
     * pass so the animating cover can move without recomposing the page.
     */
    coverBounds: (() -> Rect?)? = null,
    /** The artwork's corner radius, so the frame follows the cover's rounded corners. */
    coverCornerRadius: () -> Dp = { 0.dp },
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
            coverBounds = coverBounds?.invoke(),
            coverCornerRadius = coverCornerRadius(),
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
    coverBounds: Rect?,
    coverCornerRadius: Dp,
) {
    if (values.isEmpty() || size.width <= 0f || size.height <= 0f) return

    val breathScale = if (breathingEnabled) {
        1f + bass.coerceIn(0f, 1f) * 0.06f
    } else {
        1f
    }

    // Around the artwork the breathing multiplies the bars' length instead of scaling the frame:
    // scaling would lift the bars off the cover they are anchored to.
    if (placement == SpectrumPlacement.AROUND_ALBUM_ART) {
        val bounds = coverBounds ?: return
        if (bounds.width <= 0f || bounds.height <= 0f) return
        when (style) {
            SpectrumStyle.BAR -> drawBarsAroundCover(
                values = values,
                bounds = bounds,
                cornerRadius = coverCornerRadius,
                opacity = opacity,
                glowEnabled = glowEnabled,
                color = color,
                breathScale = breathScale,
            )

            SpectrumStyle.CURVE -> drawCurveAroundCover(
                values = values,
                bounds = bounds,
                cornerRadius = coverCornerRadius,
                opacity = opacity,
                glowEnabled = glowEnabled,
                color = color,
                breathScale = breathScale,
            )
        }
        return
    }

    val bottomAnchored = placement == SpectrumPlacement.BOTTOM
    val pivot = Offset(size.width / 2f, if (bottomAnchored) size.height else 0f)

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

/**
 * The artwork as a frame: bars grow outwards from each of the four edges, and the spectrum runs
 * clockwise around the perimeter, so the cover reads as the source of the light.
 */
private fun DrawScope.drawBarsAroundCover(
    values: FloatArray,
    bounds: Rect,
    cornerRadius: Dp,
    opacity: Float,
    glowEnabled: Boolean,
    color: Color,
    breathScale: Float,
) {
    val gap = 2.dp.toPx()
    val baseGap = 4.dp.toPx()
    // Keep the bars on the straight runs: inside the rounded corners the artwork leaves a notch,
    // and a bar standing there would read as floating off the cover.
    val cornerPad = cornerRadius.toPx() + baseGap + 4.dp.toPx()
    val glowPad = 3.dp.toPx()
    val maxLength = 36.dp.toPx() * breathScale
    val bodyColor = color.copy(alpha = opacity)
    val glowColor = color.copy(alpha = opacity * 0.10f)
    val count = values.size

    val horizontalCount = (count / 4).coerceAtLeast(1)
    val verticalCount = ((count - horizontalCount * 2) / 2).coerceAtLeast(1)
    val horizontalSpan = (bounds.width - cornerPad * 2f).coerceAtLeast(1f)
    val verticalSpan = (bounds.height - cornerPad * 2f).coerceAtLeast(1f)
    val horizontalBar = max(1f, (horizontalSpan - gap * (horizontalCount - 1)) / horizontalCount)
    val verticalBar = max(1f, (verticalSpan - gap * (verticalCount - 1)) / verticalCount)

    fun levelAt(t: Float): Float =
        values[(t * count).toInt().coerceIn(0, count - 1)].coerceIn(0f, 1f)

    // The fade is built over each bar's own length rather than over the band: a short bar has to
    // dissolve towards its tip too, or the whole frame reads as one uniform opacity.
    fun drawBar(rect: Rect, brush: (Rect, Color) -> Brush) {
        if (rect.width <= 0f || rect.height <= 0f) return
        if (glowEnabled) {
            drawRect(
                brush = brush(rect, glowColor),
                topLeft = Offset(rect.left - glowPad / 2f, rect.top - glowPad / 2f),
                size = Size(rect.width + glowPad, rect.height + glowPad),
            )
        }
        drawRect(brush = brush(rect, bodyColor), topLeft = rect.topLeft, size = rect.size)
    }

    for (index in 0 until horizontalCount) {
        val t = 0.25f * (index + 0.5f) / horizontalCount
        val length = levelAt(t) * maxLength
        if (length <= 0f) continue
        val left = bounds.left + cornerPad + index * (horizontalBar + gap)
        drawBar(Rect(Offset(left, bounds.top - baseGap - length), Size(horizontalBar, length))) { bar, barColor ->
            Brush.verticalGradient(
                0f to barColor.copy(alpha = 0f),
                1f to barColor,
                startY = bar.top,
                endY = bar.bottom,
            )
        }
    }
    for (index in 0 until horizontalCount) {
        val t = 0.5f + 0.25f * (index + 0.5f) / horizontalCount
        val length = levelAt(t) * maxLength
        if (length <= 0f) continue
        val left = bounds.right - cornerPad - (index + 1) * horizontalBar - index * gap
        drawBar(Rect(Offset(left, bounds.bottom + baseGap), Size(horizontalBar, length))) { bar, barColor ->
            Brush.verticalGradient(
                0f to barColor,
                1f to barColor.copy(alpha = 0f),
                startY = bar.top,
                endY = bar.bottom,
            )
        }
    }
    for (index in 0 until verticalCount) {
        val t = 0.25f + 0.25f * (index + 0.5f) / verticalCount
        val length = levelAt(t) * maxLength
        if (length <= 0f) continue
        val top = bounds.top + cornerPad + index * (verticalBar + gap)
        drawBar(Rect(Offset(bounds.right + baseGap, top), Size(length, verticalBar))) { bar, barColor ->
            Brush.horizontalGradient(
                0f to barColor,
                1f to barColor.copy(alpha = 0f),
                startX = bar.left,
                endX = bar.right,
            )
        }
    }
    for (index in 0 until verticalCount) {
        val t = 0.75f + 0.25f * (index + 0.5f) / verticalCount
        val length = levelAt(t) * maxLength
        if (length <= 0f) continue
        val top = bounds.bottom - cornerPad - (index + 1) * verticalBar - index * gap
        drawBar(Rect(Offset(bounds.left - baseGap - length, top), Size(length, verticalBar))) { bar, barColor ->
            Brush.horizontalGradient(
                0f to barColor.copy(alpha = 0f),
                1f to barColor,
                startX = bar.left,
                endX = bar.right,
            )
        }
    }
}

/** One sample on the artwork's rounded outline, with its outward normal and loop position. */
private class CoverOutlineSample(
    val x: Float,
    val y: Float,
    val normalX: Float,
    val normalY: Float,
    val t: Float,
)

/**
 * The curve style around the artwork: one closed wave that follows the cover's rounded outline
 * and dissolves outwards. The loop is cut into four ribbons at the middle of each corner arc,
 * so every edge fades along its own direction while the outline stays continuous and no pixel
 * is painted twice.
 */
private fun DrawScope.drawCurveAroundCover(
    values: FloatArray,
    bounds: Rect,
    cornerRadius: Dp,
    opacity: Float,
    glowEnabled: Boolean,
    color: Color,
    breathScale: Float,
) {
    val baseGap = 4.dp.toPx()
    val maxLength = 36.dp.toPx() * breathScale
    val count = values.size
    val bodyColor = color.copy(alpha = opacity)
    val transparent = bodyColor.copy(alpha = 0f)

    val left = bounds.left - baseGap
    val top = bounds.top - baseGap
    val right = bounds.right + baseGap
    val bottom = bounds.bottom + baseGap
    val radius = (cornerRadius.toPx() + baseGap)
        .coerceIn(0f, minOf(right - left, bottom - top) / 2f)

    val arcLength = (PI.toFloat() / 2f) * radius
    val perimeter = (right - left) * 2f + (bottom - top) * 2f +
        (PI.toFloat() - 4f) * radius * 2f
    if (perimeter <= 0f) return

    val samples = ArrayList<CoverOutlineSample>(160)
    val spacing = (perimeter / 160f).coerceAtLeast(4.dp.toPx())
    var traveled = 0f

    // The spectrum is folded in the middle of the loop instead of running 0..100 % once around:
    // both sides of the closing seam then read the same low bin, so the wave has no step there.
    fun levelAt(t: Float): Float {
        val folded = if (t <= 0.5f) t * 2f else (1f - t) * 2f
        val index = (folded * (count - 1)).roundToInt().coerceIn(0, count - 1)
        return values[index].coerceIn(0f, 1f)
    }

    fun addSample(x: Float, y: Float, normalX: Float, normalY: Float) {
        samples += CoverOutlineSample(x, y, normalX, normalY, traveled / perimeter)
    }

    fun addLine(toX: Float, toY: Float, normalX: Float, normalY: Float) {
        val fromX = samples.last().x
        val fromY = samples.last().y
        val length = hypot(toX - fromX, toY - fromY)
        val steps = max(1, ceil(length / spacing).toInt())
        for (index in 1..steps) {
            val fraction = index / steps.toFloat()
            traveled += length / steps
            addSample(
                x = fromX + (toX - fromX) * fraction,
                y = fromY + (toY - fromY) * fraction,
                normalX = normalX,
                normalY = normalY,
            )
        }
    }

    /** Returns the index of the arc's midpoint sample, where the fade direction changes. */
    fun addArc(centerX: Float, centerY: Float, startDegrees: Float, endDegrees: Float): Int {
        val intervals = max(2, ceil(arcLength / spacing).toInt()).let {
            if (it % 2 == 0) it else it + 1
        }
        val midpointIndex = samples.size + intervals / 2 - 1
        for (index in 1..intervals) {
            val degrees = startDegrees + (endDegrees - startDegrees) * index / intervals
            val angle = degrees * (PI.toFloat() / 180f)
            traveled += arcLength / intervals
            addSample(
                x = centerX + cos(angle) * radius,
                y = centerY + sin(angle) * radius,
                normalX = cos(angle),
                normalY = sin(angle),
            )
        }
        return midpointIndex
    }

    addSample(left + radius, top, 0f, -1f)
    addLine(right - radius, top, 0f, -1f)
    val topRightMid = addArc(right - radius, top + radius, -90f, 0f)
    addLine(right, bottom - radius, 1f, 0f)
    val bottomRightMid = addArc(right - radius, bottom - radius, 0f, 90f)
    addLine(left + radius, bottom, 0f, 1f)
    val bottomLeftMid = addArc(left + radius, bottom - radius, 90f, 180f)
    addLine(left, top + radius, -1f, 0f)
    val topLeftMid = addArc(left + radius, top + radius, 180f, 270f)
    // The last sample lands on the first one; keeping both hands the spline a zero-length
    // segment right where the loop closes.
    if (samples.size > 4) {
        val first = samples.first()
        val last = samples.last()
        if (hypot(last.x - first.x, last.y - first.y) < 0.01f) samples.removeAt(samples.lastIndex)
    }
    if (samples.size < 4) return

    fun ribbon(startIndex: Int, endIndex: Int): List<CoverOutlineSample> {
        val result = ArrayList<CoverOutlineSample>()
        var index = startIndex
        while (true) {
            result += samples[index]
            if (index == endIndex) break
            index = (index + 1) % samples.size
        }
        return result
    }

    fun drawRibbon(ribbon: List<CoverOutlineSample>, fade: Brush) {
        if (ribbon.size < 2) return
        val outer = ribbon.map { sample ->
            val depth = levelAt(sample.t) * maxLength
            Offset(sample.x + sample.normalX * depth, sample.y + sample.normalY * depth)
        }
        if (glowEnabled) {
            val glow = Path().apply { appendSpectrumSpline(outer) }
            drawPath(
                path = glow,
                color = color.copy(alpha = opacity * 0.12f),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Butt),
            )
        }
        val path = Path().apply {
            appendSpectrumSpline(outer)
            appendSpectrumSpline(ribbon.map { Offset(it.x, it.y) }.asReversed(), continuePath = true)
            close()
        }
        drawPath(path = path, brush = fade)
    }

    drawRibbon(
        ribbon = ribbon(topLeftMid, topRightMid),
        fade = Brush.verticalGradient(
            0f to bodyColor,
            1f to transparent,
            startY = top,
            endY = top - maxLength,
        ),
    )
    drawRibbon(
        ribbon = ribbon(topRightMid, bottomRightMid),
        fade = Brush.horizontalGradient(
            0f to bodyColor,
            1f to transparent,
            startX = right,
            endX = right + maxLength,
        ),
    )
    drawRibbon(
        ribbon = ribbon(bottomRightMid, bottomLeftMid),
        fade = Brush.verticalGradient(
            0f to bodyColor,
            1f to transparent,
            startY = bottom,
            endY = bottom + maxLength,
        ),
    )
    drawRibbon(
        ribbon = ribbon(bottomLeftMid, topLeftMid),
        fade = Brush.horizontalGradient(
            0f to bodyColor,
            1f to transparent,
            startX = left,
            endX = left - maxLength,
        ),
    )
}

private fun Path.appendSpectrumSpline(points: List<Offset>, continuePath: Boolean = false) {
    if (points.isEmpty()) return
    if (continuePath) lineTo(points[0].x, points[0].y) else moveTo(points[0].x, points[0].y)
    for (index in 0 until points.size - 1) {
        val p0 = points[max(0, index - 1)]
        val p1 = points[index]
        val p2 = points[index + 1]
        val p3 = points[minOf(points.size - 1, index + 2)]
        cubicTo(
            p1.x + (p2.x - p0.x) * 0.1666f,
            p1.y + (p2.y - p0.y) * 0.1666f,
            p2.x - (p3.x - p1.x) * 0.1666f,
            p2.y - (p3.y - p1.y) * 0.1666f,
            p2.x,
            p2.y,
        )
    }
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
