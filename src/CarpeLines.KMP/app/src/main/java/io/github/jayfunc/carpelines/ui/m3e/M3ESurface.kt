package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.ui.theme.M3EShape

/**
 * The M3 surface hierarchy.
 *
 * M3 separates surfaces *by role in the hierarchy*, not by shadow. A dialog sits above a sheet
 * which sits above a card which sits above the page; the tonal steps below encode exactly that.
 * Faking depth with shadows instead is the most common "Material-looking but not Material"
 * mistake.
 */
@Immutable
enum class M3ESurfaceLevel {
    /** Page background. Nothing sits below it. */
    Page,

    /** Lowest container — inset wells, scrims behind content. */
    ContainerLowest,

    /** Inline containers that should recede, e.g. a grouped block on a card page. */
    ContainerLow,

    /** Default container for cards and list groups. */
    Container,

    /** Raised containers: menus, selected rows, small sheets. */
    ContainerHigh,

    /** Topmost in-page container: modal-adjacent panels, active drag targets. */
    ContainerHighest,
}

/** Resolves a [M3ESurfaceLevel] against the active semantic colour roles. */
fun ColorScheme.surfaceColorFor(level: M3ESurfaceLevel): Color = when (level) {
    M3ESurfaceLevel.Page -> surface
    M3ESurfaceLevel.ContainerLowest -> surfaceContainerLowest
    M3ESurfaceLevel.ContainerLow -> surfaceContainerLow
    M3ESurfaceLevel.Container -> surfaceContainer
    M3ESurfaceLevel.ContainerHigh -> surfaceContainerHigh
    M3ESurfaceLevel.ContainerHighest -> surfaceContainerHighest
}

/**
 * A semantic container.
 *
 * Wraps Material 3 [Surface] so a container always gets the behaviour that matters — correct
 * content colour, a bounded ripple state layer, and `Role.Button` semantics when clickable —
 * instead of each screen re-deriving it.
 *
 * @param level which rung of the surface hierarchy this container occupies.
 * @param shape rounding from the M3 shape scale. Pass `M3EShape.none` for full-bleed media.
 * @param onClick makes the whole container one target. Prefer this over a bare `clickable` so
 *   the state layer, ripple bounds and role stay correct.
 */
@Composable
fun M3ESurface(
    modifier: Modifier = Modifier,
    level: M3ESurfaceLevel = M3ESurfaceLevel.Container,
    shape: Shape = M3EShape.large,
    color: Color = MaterialTheme.colorScheme.surfaceColorFor(level),
    contentColor: Color = m3eContentColorOf(color),
    tonalElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        val source = interactionSource ?: remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.semantics { role?.let { this.role = it } },
            enabled = enabled,
            shape = shape,
            color = color,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            border = border,
            interactionSource = source,
            content = content
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            border = border,
            content = content
        )
    }
}

/** The `on*` role M3 pairs with [color], falling back to `onSurface` for custom tones. */
@Composable
fun m3eContentColorOf(color: Color): Color {
    val scheme = MaterialTheme.colorScheme
    return when (color) {
        scheme.surface,
        scheme.surfaceContainerLowest,
        scheme.surfaceContainerLow,
        scheme.surfaceContainer,
        scheme.surfaceContainerHigh,
        scheme.surfaceContainerHighest -> scheme.onSurface

        scheme.primary -> scheme.onPrimary
        scheme.primaryContainer -> scheme.onPrimaryContainer
        scheme.secondary -> scheme.onSecondary
        scheme.secondaryContainer -> scheme.onSecondaryContainer
        scheme.tertiary -> scheme.onTertiary
        scheme.tertiaryContainer -> scheme.onTertiaryContainer
        scheme.error -> scheme.onError
        scheme.errorContainer -> scheme.onErrorContainer
        else -> scheme.onSurface
    }
}
