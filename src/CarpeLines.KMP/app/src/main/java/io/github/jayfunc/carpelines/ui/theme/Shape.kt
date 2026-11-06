package io.github.jayfunc.carpelines.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive shape tokens.
 *
 * Shape in M3 is a **semantic scale**, not decoration: a component picks the step that matches
 * its containment role, which is what keeps a screen coherent when the radius changes. The
 * values below are the M3 shape scale plus the Expressive "increased" steps, which exist so a
 * layout can raise containment on one element without jumping a whole step.
 *
 * | Token                  | Radius | Typical role                                     |
 * |------------------------|--------|--------------------------------------------------|
 * | extraSmall             | 4      | badges, tiny inline containers                   |
 * | small                  | 8      | chips, thumbnails, dense rows                    |
 * | medium                 | 12     | list items, small cards, inputs                  |
 * | large                  | 16     | cards, menus                                     |
 * | largeIncreased         | 20     | emphasised cards                                 |
 * | extraLarge             | 28     | dialogs, sheets, floating containers             |
 * | extraLargeIncreased    | 32     | pushed sheets, hero media                        |
 * | extraExtraLarge        | 48     | oversized media wells                            |
 * | full                   | stadium| buttons, chips, FABs, avatars                    |
 *
 * The app renders these with the project's smooth-corner implementation so the shape family
 * stays visually consistent with the existing artwork-heavy surfaces. Use [Shapes] through
 * `MaterialTheme.shapes` for anything a Material component already shapes; reach for [M3EShape]
 * when choosing a shape explicitly.
 */
object M3EShape {

    /** No rounding. Use only for edge-to-edge media or intentional full-bleed surfaces. */
    val none: Shape = RectangleShape

    val extraSmall: CornerBasedShape = ShapeCache.smooth4

    val small: CornerBasedShape = ShapeCache.smooth8

    val medium: CornerBasedShape = ShapeCache.smooth12

    val large: CornerBasedShape = ShapeCache.smooth16

    val largeIncreased: Shape = ShapeCache.smooth20

    val extraLarge: CornerBasedShape = ShapeCache.smooth28

    val extraLargeIncreased: Shape = ShapeCache.smooth32

    val extraExtraLarge: Shape = ShapeCache.smooth48

    /** Stadium / pill. Buttons, chips, active indicators, avatars. */
    val full: Shape = CircleShape

    /** Ad-hoc step for content that does not sit on the scale. Prefer a token when one fits. */
    fun rounded(radius: Dp): Shape = when (radius) {
        4.dp -> extraSmall
        8.dp -> small
        12.dp -> medium
        16.dp -> large
        20.dp -> largeIncreased
        28.dp -> extraLarge
        32.dp -> extraLargeIncreased
        48.dp -> extraExtraLarge
        else -> ShapeCache.smooth(radius)
    }

    /**
     * Corner treatment for one row of a contiguous group, so only the group's outer corners
     * stay rounded. This is the M3 "grouped list" convention used by settings and sheets —
     * rows are separated by shape, not by individual card outlines.
     */
    fun grouped(index: Int, count: Int, radius: Dp = 24.dp): Shape = when {
        count <= 1 -> ShapeCache.smooth(radius)
        index <= 0 -> ShapeCache.smoothTop(radius)
        index >= count - 1 -> ShapeCache.smoothBottom(radius)
        else -> ShapeCache.smooth4
    }

    /**
     * Corner treatment for one row of a group whose rows are already separate cards.
     *
     * Rows keep [radius] on every corner they share with a neighbour, while the first and last row
     * lift their outer corner to [outerRadius] — the radius a settings subsection's container
     * clips to — so the ends of the group read as the group they belong to. Use with the same 2dp
     * gap a settings subsection puts between its items.
     */
    fun groupedCards(
        index: Int,
        count: Int,
        radius: Dp = 12.dp,
        outerRadius: Dp = 28.dp,
    ): Shape = when {
        count <= 1 -> ShapeCache.smooth(outerRadius)
        index <= 0 -> ShapeCache.smoothVertical(top = outerRadius, bottom = radius)
        index >= count - 1 -> ShapeCache.smoothVertical(top = radius, bottom = outerRadius)
        else -> ShapeCache.smooth(radius)
    }

    /**
     * Corner treatment for one segment of a horizontally connected group — the M3E "connected
     * action bar", where related actions share one silhouette and a hairline gap instead of
     * being drawn as separate floating pills.
     *
     * Outer ends keep [radius]; the facing ends step down to [innerRadius] so the segments read
     * as one bar. Use with a 2dp gap between segments.
     */
    fun connected(
        index: Int,
        count: Int,
        radius: Dp = 24.dp,
        innerRadius: Dp = 8.dp,
    ): Shape = when {
        count <= 1 -> ShapeCache.smooth(radius)
        index <= 0 -> ShapeCache.smoothLeading(radius, innerRadius)
        index >= count - 1 -> ShapeCache.smoothTrailing(radius, innerRadius)
        else -> ShapeCache.smooth(innerRadius)
    }
}

/**
 * The shape scale exposed to Material components. Values follow [M3EShape] so a component's
 * default shape and an explicitly chosen shape never disagree.
 */
val Shapes = Shapes(
    extraSmall = M3EShape.extraSmall,
    small = M3EShape.small,
    medium = M3EShape.medium,
    large = M3EShape.large,
    extraLarge = M3EShape.extraLarge
)
