package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.ui.theme.LocalReducedMotion
import io.github.jayfunc.carpelines.ui.theme.M3EMotion
import io.github.jayfunc.carpelines.ui.theme.M3ESize
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing

/** Emphasis of one segment in a [M3EConnectedActions] bar, highest first. */
@Immutable
enum class M3EActionEmphasis {
    /** Page's single call to action. Like [M3EPrimaryButton], at most one per view. */
    Filled,

    /** A section's main action: strong, but never competing with the page CTA. */
    Tonal,

    /** Lowest emphasis. Keeps an outline so the segment still reads inside the group. */
    Outlined,
}

/**
 * One action inside a [M3EConnectedActions] bar.
 *
 * [icon] is the segment's *label* counterpart, not decoration: M3 Expressive relies on a leading
 * glyph to make the action scannable at a glance, so every segment in a bar should carry one.
 */
@Immutable
data class M3EAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val emphasis: M3EActionEmphasis = M3EActionEmphasis.Tonal,
    val enabled: Boolean = true,
)

/** Axis along which [M3EConnectedActions] lays its segments out. */
enum class M3EConnectedOrientation {
    /** One segment per row, full width. Keeps long localised labels on a single line. */
    Vertical,

    /** Segments share one row, evenly weighted, so the bar stays one silhouette. */
    Horizontal,
}

/** Gap between connected segments — the M3 Expressive "hairline" that keeps one silhouette. */
private val ConnectedSpaceBetween: Dp = 2.dp

/** Pressed corner radius: a pill squishes toward a rounded-square, never to a hard rectangle. */
private val PressedCorner: Dp = 16.dp

/** Resting corner radius of a segment's inner (facing) corners. */
private val RestingInnerCorner: Dp = 8.dp

/**
 * Related actions drawn as one Material 3 Expressive connected action bar.
 *
 * A lone action is a pill that hugs its label; two or more related actions become a single
 * silhouette — fully round on the outside, tight on the facing corners, separated by a 2dp
 * hairline. Drawing them as separate floating buttons (a filled one stacked above an outlined
 * one) is the Material 2 pattern this replaces: it reads as two unrelated controls and makes the
 * page look like a form.
 *
 * Pressing a segment squishes its corners toward [PressedCorner] on the expressive spatial
 * spring, so the touch is answered by shape — not only by a ripple. The squish is skipped when
 * the user asked for reduced motion.
 *
 * [orientation] picks the axis: [M3EConnectedOrientation.Vertical] stacks full-width segments so
 * long labels never get squeezed, while [M3EConnectedOrientation.Horizontal] keeps the bar on a
 * single row with evenly weighted segments. A lone action is a pill that hugs its label, placed
 * by [loneActionAlignment] inside the space the modifier claims.
 *
 * Only actions acting on the *same* object belong in one bar. Unrelated actions should be
 * separated by page spacing instead, which keeps the connected silhouette meaningful.
 */
@Composable
fun M3EConnectedActions(
    actions: List<M3EAction>,
    modifier: Modifier = Modifier,
    height: Dp = M3ESize.buttonExpressive,
    orientation: M3EConnectedOrientation = M3EConnectedOrientation.Vertical,
    loneActionAlignment: Alignment.Horizontal = Alignment.Start,
) {
    if (actions.isEmpty()) return

    if (actions.size == 1) {
        ConnectedSegment(
            action = actions[0],
            index = 0,
            count = 1,
            vertical = false,
            height = height,
            modifier = modifier.wrapContentWidth(loneActionAlignment)
        )
        return
    }

    if (orientation == M3EConnectedOrientation.Horizontal) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ConnectedSpaceBetween)
        ) {
            actions.forEachIndexed { index, action ->
                ConnectedSegment(
                    action = action,
                    index = index,
                    count = actions.size,
                    vertical = false,
                    height = height,
                    contentPadding = M3ESpacing.large,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ConnectedSpaceBetween)
    ) {
        actions.forEachIndexed { index, action ->
            ConnectedSegment(
                action = action,
                index = index,
                count = actions.size,
                vertical = true,
                height = height,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConnectedSegment(
    action: M3EAction,
    index: Int,
    count: Int,
    vertical: Boolean,
    height: Dp,
    modifier: Modifier = Modifier,
    contentPadding: Dp = M3ESpacing.extraLarge,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val spec: FiniteAnimationSpec<Dp> =
        if (LocalReducedMotion.current) snap() else M3EMotion.spatialFast()
    val restingOuter = height / 2
    val outer by animateDpAsState(
        targetValue = if (pressed) PressedCorner else restingOuter,
        animationSpec = spec,
        label = "segmentOuterCorner"
    )
    val inner by animateDpAsState(
        targetValue = if (pressed) PressedCorner else RestingInnerCorner,
        animationSpec = spec,
        label = "segmentInnerCorner"
    )

    Surface(
        onClick = action.onClick,
        enabled = action.enabled,
        shape = connectedSegmentShape(index, count, vertical, outer, inner),
        color = action.emphasis.container(action.enabled),
        contentColor = action.emphasis.content(action.enabled),
        border = action.emphasis.border(action.enabled),
        interactionSource = interaction,
        modifier = modifier.heightIn(min = M3ESize.minimumTarget)
    ) {
        SegmentContent(action = action, height = height, horizontalPadding = contentPadding)
    }
}

@Composable
private fun SegmentContent(action: M3EAction, height: Dp, horizontalPadding: Dp) {
    Row(
        modifier = Modifier
            .height(height)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (action.icon != null) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(SegmentIconSize)
            )
            Spacer(Modifier.width(M3ESpacing.small))
        }
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val SegmentIconSize: Dp = 18.dp

/**
 * Segment silhouette. Outer corners come from [outer] so a pill stays a pill; facing corners use
 * [inner] so segments read as one bar. A single segment is fully round on every side.
 */
private fun connectedSegmentShape(
    index: Int,
    count: Int,
    vertical: Boolean,
    outer: Dp,
    inner: Dp,
): Shape {
    if (count <= 1) return RoundedCornerShape(outer)
    val leading = index == 0
    val trailing = index == count - 1
    return if (vertical) {
        RoundedCornerShape(
            topStart = if (leading) outer else inner,
            topEnd = if (leading) outer else inner,
            bottomStart = if (trailing) outer else inner,
            bottomEnd = if (trailing) outer else inner
        )
    } else {
        RoundedCornerShape(
            topStart = if (leading) outer else inner,
            bottomStart = if (leading) outer else inner,
            topEnd = if (trailing) outer else inner,
            bottomEnd = if (trailing) outer else inner
        )
    }
}

/** Disabled containers drop to the M3 12% `onSurface` scrim instead of a faded brand colour. */
private const val DisabledContainerAlpha = 0.12f
private const val DisabledContentAlpha = 0.38f

@Composable
private fun M3EActionEmphasis.container(enabled: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    if (!enabled) return scheme.onSurface.copy(alpha = DisabledContainerAlpha)
    return when (this) {
        M3EActionEmphasis.Filled -> scheme.primary
        M3EActionEmphasis.Tonal -> scheme.secondaryContainer
        M3EActionEmphasis.Outlined -> Color.Transparent
    }
}

@Composable
private fun M3EActionEmphasis.content(enabled: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    if (!enabled) return scheme.onSurface.copy(alpha = DisabledContentAlpha)
    return when (this) {
        M3EActionEmphasis.Filled -> scheme.onPrimary
        M3EActionEmphasis.Tonal -> scheme.onSecondaryContainer
        M3EActionEmphasis.Outlined -> scheme.onSurface
    }
}

@Composable
private fun M3EActionEmphasis.border(enabled: Boolean): BorderStroke? = when {
    this != M3EActionEmphasis.Outlined -> null
    !enabled -> BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerAlpha))
    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
}
