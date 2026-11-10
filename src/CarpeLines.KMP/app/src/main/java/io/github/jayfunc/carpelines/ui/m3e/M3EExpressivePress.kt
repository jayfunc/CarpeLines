package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.ui.theme.LocalReducedMotion
import io.github.jayfunc.carpelines.ui.theme.M3EMotion
import io.github.jayfunc.carpelines.ui.theme.M3ESize
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Pressed corner radius: a pill squishes toward a rounded-square, never to a hard rectangle. */
internal val PressedCorner: Dp = 16.dp

/** The label shrinks a touch under the finger, so the press lands before the action does. */
internal const val PressedContentScale = 0.95f

/** Extra horizontal padding the label gains while pressed, widening the segment's content box. */
internal val PressedPaddingGrowth: Dp = 4.dp

/** How long the pressed look outlives a quick tap, so the feedback is actually visible. */
internal const val TapLingerMillis = 100L

/** The label's scale is bouncier than the container: it is the part the eye is already on. */
private val PressedContentSpec =
    spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

/** Disabled containers drop to the M3 12% `onSurface` scrim instead of a faded brand colour. */
internal const val DisabledContainerAlpha = 0.12f
internal const val DisabledContentAlpha = 0.38f

/**
 * Press bookkeeping for one expressive button.
 *
 * The interaction source is shared with the animations, so the scale and the visual press cannot
 * disagree about whether the button is down. [lingering] holds the pressed look on for
 * [TapLingerMillis] after a quick tap.
 */
internal class ExpressivePressState {
    val interaction = MutableInteractionSource()

    var lingering by mutableStateOf(false)

    fun holdAfterTap(scope: CoroutineScope) {
        scope.launch {
            lingering = true
            delay(TapLingerMillis)
            lingering = false
        }
    }
}

/** A tick at press time, not at click time: the finger wants to know the button took the press. */
@Composable
internal fun ExpressivePressHaptic(press: ExpressivePressState, enabled: Boolean) {
    val pressed by press.interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(pressed) {
        if (pressed && enabled) {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    }
}

/** Animated values of the expressive press, shared by connected segments and lone buttons. */
@Immutable
internal data class ExpressivePressVisuals(
    val contentScale: Float,
    val contentGrowth: Dp,
    val outerCorner: Dp,
    val innerCorner: Dp,
)

@Composable
internal fun rememberExpressivePressVisuals(
    press: ExpressivePressState,
    restingOuter: Dp,
    restingInner: Dp,
): ExpressivePressVisuals {
    val pressed by press.interaction.collectIsPressedAsState()
    val active = pressed || press.lingering
    val reducedMotion = LocalReducedMotion.current

    val spec: FiniteAnimationSpec<Dp> = if (reducedMotion) snap() else M3EMotion.spatialFast()
    val outer by animateDpAsState(
        targetValue = if (active) PressedCorner else restingOuter,
        animationSpec = spec,
        label = "pressOuterCorner",
    )
    val inner by animateDpAsState(
        targetValue = if (active) PressedCorner else restingInner,
        animationSpec = spec,
        label = "pressInnerCorner",
    )
    val contentGrowth by animateDpAsState(
        targetValue = if (active && !reducedMotion) PressedPaddingGrowth else 0.dp,
        animationSpec = spec,
        label = "pressContentGrowth",
    )
    val contentScale by animateFloatAsState(
        targetValue = if (active && !reducedMotion) PressedContentScale else 1f,
        animationSpec = if (reducedMotion) snap() else PressedContentSpec,
        label = "pressContentScale",
    )
    return ExpressivePressVisuals(
        contentScale = contentScale,
        contentGrowth = contentGrowth,
        outerCorner = outer,
        innerCorner = inner,
    )
}

/**
 * A standalone M3 Expressive button with the press feedback of a [M3EConnectedActions] segment.
 *
 * Use it for a button that stands on its own (a dialog's confirm, a page's only call to action)
 * but should still feel like the connected bars elsewhere: the corners squish toward
 * [PressedCorner], the content scales down a touch and widens, the pressed look lingers for
 * [TapLingerMillis], and a haptic tick lands at press time. Everything spatial is skipped under
 * reduced motion.
 *
 * [cornerRadius] sets the resting rounding; leave it `null` for a pill. The caller supplies the
 * colours because a standalone button often animates them (busy/disabled states) itself.
 */
@Composable
fun M3EExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    height: Dp = M3ESize.buttonExpressive,
    cornerRadius: Dp? = null,
    border: BorderStroke? = null,
    shadowElevation: Dp = 0.dp,
    contentPadding: Dp = M3ESpacing.large,
    content: @Composable () -> Unit,
) {
    val press = remember { ExpressivePressState() }
    val scope = rememberCoroutineScope()
    ExpressivePressHaptic(press, enabled)

    val restingCorner = cornerRadius ?: (height / 2)
    val visuals = rememberExpressivePressVisuals(
        press = press,
        restingOuter = restingCorner,
        restingInner = restingCorner,
    )

    Surface(
        onClick = {
            press.holdAfterTap(scope)
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(visuals.outerCorner),
        color = color,
        contentColor = contentColor,
        border = border,
        shadowElevation = shadowElevation,
        interactionSource = press.interaction,
        modifier = modifier
            .heightIn(min = M3ESize.minimumTarget)
            .semantics { role = Role.Button },
    ) {
        Box(
            modifier = Modifier
                .height(height)
                .padding(horizontal = contentPadding + visuals.contentGrowth)
                .graphicsLayer {
                    scaleX = visuals.contentScale
                    scaleY = visuals.contentScale
                },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
