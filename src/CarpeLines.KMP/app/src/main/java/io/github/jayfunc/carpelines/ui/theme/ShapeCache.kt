package io.github.jayfunc.carpelines.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * OPT #6 — Cached instances of frequently-used AbsoluteSmoothCornerShape.
 *
 * AbsoluteSmoothCornerShape is significantly more expensive than RoundedCornerShape because it
 * computes cubic Bézier curves analytically. In LazyColumn items (song cards, album cards, etc.)
 * each item pays this cost on first composition. By reusing singleton instances we avoid
 * repeated Path construction for the most common radii.
 *
 * Usage:
 *   Modifier.clip(ShapeCache.smooth12)
 *   Modifier.background(color, ShapeCache.smooth16)
 */
object ShapeCache {
    /** M3 Expressive corner smoothing. 60% keeps corners readable at every step of the scale. */
    const val SMOOTHNESS_PERCENT = 60

    /** 4dp smooth corners — M3 extraSmall (badges, tiny containers) */
    val smooth4 = AbsoluteSmoothCornerShape(cornerRadius = 4.dp, smoothnessAsPercent = SMOOTHNESS_PERCENT)

    /** 8dp smooth corners — compact chips, small surfaces */
    val smooth8 = AbsoluteSmoothCornerShape(cornerRadius = 8.dp, smoothnessAsPercent = SMOOTHNESS_PERCENT)

    /** 10dp smooth corners */
    val smooth10 = AbsoluteSmoothCornerShape(cornerRadius = 10.dp, smoothnessAsPercent = 60)

    /** 12dp smooth corners — song list items, small cards */
    val smooth12 = AbsoluteSmoothCornerShape(cornerRadius = 12.dp, smoothnessAsPercent = 60)

    /** 14dp smooth corners */
    val smooth14 = AbsoluteSmoothCornerShape(cornerRadius = 14.dp, smoothnessAsPercent = 60)

    /** 16dp smooth corners — album cards, playlist items */
    val smooth16 = AbsoluteSmoothCornerShape(cornerRadius = 16.dp, smoothnessAsPercent = 60)

    /** 20dp smooth corners — larger cards */
    val smooth20 = AbsoluteSmoothCornerShape(cornerRadius = 20.dp, smoothnessAsPercent = 60)

    /** 24dp smooth corners — dialog surfaces */
    val smooth24 = AbsoluteSmoothCornerShape(cornerRadius = 24.dp, smoothnessAsPercent = 60)

    /** 28dp smooth corners */
    val smooth28 = AbsoluteSmoothCornerShape(cornerRadius = 28.dp, smoothnessAsPercent = 60)

    /** 32dp smooth corners — bottom sheets, floating panels */
    val smooth32 = AbsoluteSmoothCornerShape(cornerRadius = 32.dp, smoothnessAsPercent = 60)

    /** 48dp smooth corners — M3 extraExtraLarge, oversized media wells */
    val smooth48 = AbsoluteSmoothCornerShape(cornerRadius = 48.dp, smoothnessAsPercent = 60)

    /** Fully smooth (pill) — 50dp, used for buttons and chips */
    val smoothPill = AbsoluteSmoothCornerShape(cornerRadius = 50.dp, smoothnessAsPercent = 60)

    private val radiusCache = java.util.concurrent.ConcurrentHashMap<Int, AbsoluteSmoothCornerShape>()
    private val topCache = java.util.concurrent.ConcurrentHashMap<Int, AbsoluteSmoothCornerShape>()
    private val bottomCache = java.util.concurrent.ConcurrentHashMap<Int, AbsoluteSmoothCornerShape>()
    private val leadingCache = java.util.concurrent.ConcurrentHashMap<Long, AbsoluteSmoothCornerShape>()
    private val trailingCache = java.util.concurrent.ConcurrentHashMap<Long, AbsoluteSmoothCornerShape>()
    private val verticalCache = java.util.concurrent.ConcurrentHashMap<Long, AbsoluteSmoothCornerShape>()

    /** Smooth shape for an arbitrary radius, memoised so list items do not rebuild paths. */
    fun smooth(radius: Dp): AbsoluteSmoothCornerShape =
        radiusCache.getOrPut(radius.value.toInt()) {
            AbsoluteSmoothCornerShape(cornerRadius = radius, smoothnessAsPercent = SMOOTHNESS_PERCENT)
        }

    /** Only the top corners are rounded — first row of a grouped list. */
    fun smoothTop(radius: Dp): AbsoluteSmoothCornerShape =
        topCache.getOrPut(radius.value.toInt()) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTL = radius,
                smoothnessAsPercentTL = SMOOTHNESS_PERCENT,
                cornerRadiusTR = radius,
                smoothnessAsPercentTR = SMOOTHNESS_PERCENT,
                cornerRadiusBR = 0.dp,
                smoothnessAsPercentBR = 0,
                cornerRadiusBL = 0.dp,
                smoothnessAsPercentBL = 0
            )
        }

    /** Only the bottom corners are rounded — last row of a grouped list. */
    fun smoothBottom(radius: Dp): AbsoluteSmoothCornerShape =
        bottomCache.getOrPut(radius.value.toInt()) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTL = 0.dp,
                smoothnessAsPercentTL = 0,
                cornerRadiusTR = 0.dp,
                smoothnessAsPercentTR = 0,
                cornerRadiusBR = radius,
                smoothnessAsPercentBR = SMOOTHNESS_PERCENT,
                cornerRadiusBL = radius,
                smoothnessAsPercentBL = SMOOTHNESS_PERCENT
            )
        }

    /**
     * Leading segment of a horizontally connected group — a connected action bar.
     *
     * The outer end keeps [outer], the inner end steps down to [inner], so the two segments read
     * as one bar split by a hairline gap instead of two neighbouring pills. Absolute corners: an
     * RTL layout has to mirror this explicitly.
     */
    fun smoothLeading(outer: Dp, inner: Dp): AbsoluteSmoothCornerShape =
        leadingCache.getOrPut(sideKey(outer, inner)) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTL = outer,
                smoothnessAsPercentTL = SMOOTHNESS_PERCENT,
                cornerRadiusBL = outer,
                smoothnessAsPercentBL = SMOOTHNESS_PERCENT,
                cornerRadiusTR = inner,
                smoothnessAsPercentTR = smoothnessOf(inner),
                cornerRadiusBR = inner,
                smoothnessAsPercentBR = smoothnessOf(inner)
            )
        }

    /** Trailing segment of a horizontally connected group. Mirror of [smoothLeading]. */
    fun smoothTrailing(outer: Dp, inner: Dp): AbsoluteSmoothCornerShape =
        trailingCache.getOrPut(sideKey(outer, inner)) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTR = outer,
                smoothnessAsPercentTR = SMOOTHNESS_PERCENT,
                cornerRadiusBR = outer,
                smoothnessAsPercentBR = SMOOTHNESS_PERCENT,
                cornerRadiusTL = inner,
                smoothnessAsPercentTL = smoothnessOf(inner),
                cornerRadiusBL = inner,
                smoothnessAsPercentBL = smoothnessOf(inner)
            )
        }

    /**
     * Vertically asymmetric card: the top corners take [top], the bottom corners take [bottom].
     *
     * Used by the outer rows of a group whose rows are individual cards, where the group's edge
     * corners are lifted to the container step while the facing corners stay at the row step.
     */
    fun smoothVertical(top: Dp, bottom: Dp): AbsoluteSmoothCornerShape =
        verticalCache.getOrPut(sideKey(top, bottom)) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTL = top,
                smoothnessAsPercentTL = smoothnessOf(top),
                cornerRadiusTR = top,
                smoothnessAsPercentTR = smoothnessOf(top),
                cornerRadiusBR = bottom,
                smoothnessAsPercentBR = smoothnessOf(bottom),
                cornerRadiusBL = bottom,
                smoothnessAsPercentBL = smoothnessOf(bottom)
            )
        }

    private fun sideKey(outer: Dp, inner: Dp): Long =
        (outer.value.toInt().toLong() shl 32) or inner.value.toInt().toLong()

    /** A zero-radius corner must not be smoothed, or the smoothing would pull it round again. */
    private fun smoothnessOf(radius: Dp): Int = if (radius.value > 0f) SMOOTHNESS_PERCENT else 0

    /** M3 Expressive cookie — artist/people avatars and placeholders */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val expressiveAvatar: Shape = RoundedPolygonShape(MaterialShapes.Cookie9Sided)

    /** M3 Expressive clover — folder/collection icon containers */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val expressiveClover: Shape = RoundedPolygonShape(MaterialShapes.Clover8Leaf)

    /** M3 Expressive soft burst — empty-state hero icon containers */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val expressiveHero: Shape = RoundedPolygonShape(MaterialShapes.SoftBurst)
}

/**
 * Singleton-friendly alternative to material3's @Composable RoundedPolygon.toShape():
 * the normalized (0..1 bounds) polygon path is computed once and scaled to the outline size,
 * so the shape can be cached here like the other ShapeCache entries.
 */
private class RoundedPolygonShape(polygon: RoundedPolygon) : Shape {
    private val basePath = polygon.toPath().asComposePath()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val matrix = Matrix()
        matrix.scale(size.width, size.height)
        val path = Path()
        path.addPath(basePath)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}
