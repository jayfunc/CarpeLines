package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.ui.theme.M3EMotion
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESize
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing

/** One destination of an M3 navigation bar or rail. */
@Immutable
data class M3ENavItem(
    val label: String,
    val icon: Painter,
    val selectedIcon: Painter = icon
)

/**
 * M3 navigation bar.
 *
 * The bar owns *navigation only*: three to five peer destinations, all equally reachable. It is
 * not a place for actions, and it is not a place for a single "more" catch-all that hides the
 * real structure.
 *
 * Behaviour that the M3 spec requires and this component guarantees:
 * - every destination is a `Role.Tab` with `selected` semantics, so the selected state is
 *   announced and not only coloured;
 * - the active indicator animates its width with the theme's spatial motion spec instead of a
 *   hardcoded duration;
 * - labels stay visible — an icon-only bar loses recognition for infrequent destinations;
 * - each destination is at least [M3ESize.minimumTarget] tall.
 */
@Composable
fun M3ENavigationBar(
    items: List<M3ENavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compactMode: Boolean = false,
    containerHeight: Dp = NavigationBarHeight
) {
    M3ESurface(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight),
        level = M3ESurfaceLevel.Container,
        shape = M3EShape.extraLarge,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = M3ESpacing.small),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                M3ENavigationBarItem(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    enabled = enabled,
                    alwaysShowLabel = !compactMode,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * A single destination.
 *
 * The indicator is one box that grows rather than two overlapping boxes, so the transition reads
 * as a single object changing state. The icon lifts slightly with it, which is the M3E cue that
 * the destination became active.
 *
 * Pressing draws a *state layer* on that same pill and lets it swell a few dp, instead of the
 * default rectangular ripple: the old ripple was bounded by the item's full width and height, so a
 * tap painted a hard-edged rectangle across the bar, outside the rounded indicator it belonged to.
 */
@Composable
private fun M3ENavigationBarItem(
    item: M3ENavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val pressed by interactionSource.collectIsPressedAsState()

    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) SelectedIndicatorWidth else IdleIndicatorWidth,
        animationSpec = M3EMotion.spatial<Dp>(),
        label = "navIndicatorWidth"
    )
    // The press state layer is part of the indicator, not the item: one shape, one motion.
    val pressExpansion by animateDpAsState(
        targetValue = if (pressed) 6.dp else 0.dp,
        animationSpec = M3EMotion.spatial<Dp>(),
        label = "navPressExpansion"
    )
    val pressLayerAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = M3EMotion.effects<Float>(),
        label = "navPressLayer"
    )
    val iconScale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.88f
            selected -> 1.05f
            else -> 1f
        },
        animationSpec = M3EMotion.spatial<Float>(),
        label = "navIconScale"
    )
    val labelScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = M3EMotion.spatial<Float>(),
        label = "navLabelScale"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        animationSpec = M3EMotion.effects<androidx.compose.ui.graphics.Color>(),
        label = "navIconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
        animationSpec = M3EMotion.effects<androidx.compose.ui.graphics.Color>(),
        label = "navLabelColor"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) scheme.secondaryContainer else Color.Transparent,
        animationSpec = M3EMotion.effects<androidx.compose.ui.graphics.Color>(),
        label = "navIndicatorColor"
    )

    Column(
        modifier = modifier
            .height(if (alwaysShowLabel) M3ESize.minimumTarget + 24.dp else M3ESize.minimumTarget)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Tab,
                onClick = {
                    if (enabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onClick()
                    }
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(indicatorWidth + pressExpansion)
                .height(SelectedIndicatorHeight + pressExpansion)
                .background(indicatorColor, M3EShape.full),
            contentAlignment = Alignment.Center
        ) {
            if (pressLayerAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            scheme.onSurface.copy(
                                alpha = (if (selected) 0.10f else 0.08f) * pressLayerAlpha
                            ),
                            M3EShape.full
                        )
                )
            }
            Icon(
                painter = if (selected) item.selectedIcon else item.icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(M3ESize.iconMedium)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }
        if (alwaysShowLabel) {
            Spacer(Modifier.height(M3ESpacing.extraSmall))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    scaleX = labelScale
                    scaleY = labelScale
                }
            )
        }
    }
}

/** Height of the M3 navigation bar container. Shared with screens that pad content for it. */
val NavigationBarHeight: Dp = 80.dp

private val SelectedIndicatorWidth: Dp = 56.dp
private val IdleIndicatorWidth: Dp = 32.dp
private val SelectedIndicatorHeight: Dp = 32.dp
