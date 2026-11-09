package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESize
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing

/**
 * Primary call to action.
 *
 * Exactly one per view. Filled is the highest emphasis M3 offers, so reserving it keeps the
 * hierarchy readable: filled → tonal → outlined → text.
 */
@Composable
fun M3EPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    shape: Shape = M3EShape.full
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = M3ESize.minimumTarget),
        enabled = enabled,
        shape = shape
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(M3ESize.iconSmall))
            Spacer(Modifier.width(M3ESpacing.small))
        }
        Text(label)
    }
}

/** Tonal action — useful when the primary action already lives elsewhere on the screen. */
@Composable
fun M3ETonalButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    shape: Shape = M3EShape.full
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = M3ESize.minimumTarget),
        enabled = enabled,
        shape = shape
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(M3ESize.iconSmall))
            Spacer(Modifier.width(M3ESpacing.small))
        }
        Text(label)
    }
}

/** Secondary action. Lower emphasis than [M3EPrimaryButton]. */
@Composable
fun M3ESecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    shape: Shape = M3EShape.full,
    contentColor: Color = Color.Unspecified
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = M3ESize.minimumTarget),
        enabled = enabled,
        shape = shape,
        colors = if (contentColor == Color.Unspecified) {
            ButtonDefaults.outlinedButtonColors()
        } else {
            ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
        }
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(M3ESize.iconSmall))
            Spacer(Modifier.width(M3ESpacing.small))
        }
        Text(label)
    }
}

/**
 * Group of related actions.
 *
 * Actions are related when they act on the same object; if they do not, separate them with
 * spacing instead of grouping, so the grouping itself keeps meaning something. The gap is
 * smaller than the page gutter on purpose.
 */
@Composable
fun M3EButtonGroup(
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Horizontal = Arrangement.spacedBy(M3ESpacing.small),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier = modifier.heightIn(min = M3ESize.minimumTarget),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * Filter chip.
 *
 * Selection is communicated by the leading check icon **and** container colour, so it survives
 * colour-blindness and grayscale. Chips filter a view; they are not primary navigation and not
 * a replacement for buttons.
 */
@Composable
fun M3EFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = M3EShape.chip,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        }
    )
}
