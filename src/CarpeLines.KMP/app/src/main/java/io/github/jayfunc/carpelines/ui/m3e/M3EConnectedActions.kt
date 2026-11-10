package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

/** Resting corner radius of a segment's inner (facing) corners. */
private val RestingInnerCorner: Dp = 8.dp

/** How much wider a pressed segment grows inside a horizontal bar.
 *
 * M3 Expressive's press feedback is spatial, not just tonal: the pressed segment claims the room
 * it is about to act on while its neighbour yields, which reads as one bar reshaping rather than
 * two independent buttons.
 */
private const val PressedWeightGain = 1.25f

/** Weight runs on a low-bouncy spring: the bar widens with a little overshoot, then settles. */
private val PressedWeightSpec =
    spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)

/**
 * Related actions drawn as one Material 3 Expressive connected action bar.
 *
 * A lone action is a pill that hugs its label; two or more related actions become a single
 * silhouette — fully round on the outside, tight on the facing corners, separated by a 2dp
 * hairline. Drawing them as separate floating buttons (a filled one stacked above an outlined
 * one) is the Material 2 pattern this replaces: it reads as two unrelated controls and makes the
 * page look like a form.
 *
 * Pressing a segment is answered in four ways at once, which is what makes it read as expressive
 * instead of merely tinted: the corners squish toward [PressedCorner], the label scales down a
 * touch, its content box widens by [PressedPaddingGrowth], and in a [M3EConnectedOrientation.Horizontal]
 * bar the pressed segment grows to [PressedWeightGain] of its share while its neighbour yields. A
 * short [TapLingerMillis] hold keeps that visible after a flick, and a haptic tick lands at press
 * time rather than at click time. Everything spatial is skipped under reduced motion.
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
                val press = remember { ExpressivePressState() }
                val pressed by press.interaction.collectIsPressedAsState()
                val active = pressed || press.lingering
                val weight by animateFloatAsState(
                    targetValue = if (active && action.enabled) PressedWeightGain else 1f,
                    animationSpec = PressedWeightSpec,
                    label = "segmentWeight",
                )
                ConnectedSegment(
                    action = action,
                    index = index,
                    count = actions.size,
                    vertical = false,
                    height = height,
                    contentPadding = M3ESpacing.large,
                    press = press,
                    modifier = Modifier.weight(weight)
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
    press: ExpressivePressState = remember { ExpressivePressState() },
) {
    val scope = rememberCoroutineScope()

    ExpressivePressHaptic(press, action.enabled)

    val restingOuter = height / 2
    val visuals = rememberExpressivePressVisuals(
        press = press,
        restingOuter = restingOuter,
        restingInner = RestingInnerCorner,
    )

    Surface(
        onClick = {
            // Hold the pressed look briefly: without it a quick tap is over before the eyes catch
            // the animation, and the button reads as unresponsive.
            press.holdAfterTap(scope)
            action.onClick()
        },
        enabled = action.enabled,
        shape = connectedSegmentShape(index, count, vertical, visuals.outerCorner, visuals.innerCorner),
        color = action.emphasis.container(action.enabled),
        contentColor = action.emphasis.content(action.enabled),
        border = action.emphasis.border(action.enabled),
        interactionSource = press.interaction,
        modifier = modifier.heightIn(min = M3ESize.minimumTarget)
    ) {
        SegmentContent(
            action = action,
            height = height,
            horizontalPadding = contentPadding + visuals.contentGrowth,
            contentScale = visuals.contentScale,
        )
    }
}

@Composable
private fun SegmentContent(
    action: M3EAction,
    height: Dp,
    horizontalPadding: Dp,
    contentScale: Float,
) {
    Row(
        modifier = Modifier
            .height(height)
            .padding(horizontal = horizontalPadding)
            .graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
            },
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
