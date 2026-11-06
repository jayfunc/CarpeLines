package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESize
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import io.github.jayfunc.carpelines.ui.theme.emphasized

/**
 * M3 list item.
 *
 * One row is one target: pressing anywhere on it activates it, the row is at least
 * [M3ESize.minimumTarget] tall, and selection is communicated by **container colour plus a
 * content-colour change** — never by colour alone.
 *
 * Row height follows the M3 list spec (56 / 72 / 88dp) derived from the number of text lines, so
 * lists keep an even rhythm without each screen inventing its own padding.
 *
 * @param overline optional label above the headline, for grouping context.
 * @param supportingText second text line; wrapping is limited to keep rows scannable.
 * @param supportingTextMaxLines line budget for [supportingText]; raise it for descriptions
 *   that must not truncate.
 * @param leadingContent typically a 40dp icon container or artwork thumbnail.
 * @param trailingContent metadata, a switch, or an overflow button. Keep it non-nested-clickable
 *   unless it is genuinely a separate action.
 */
@Composable
fun M3EListItem(
    headline: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = M3EShape.medium,
    containerColor: Color? = null,
    contentColor: Color? = null,
    supportingTextMaxLines: Int = 2,
    onClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val scheme = MaterialTheme.colorScheme
    val resolvedContainer = containerColor ?: when {
        selected -> scheme.secondaryContainer
        else -> Color.Transparent
    }
    val resolvedContent = contentColor ?: when {
        !enabled -> scheme.onSurface.copy(alpha = DisabledContentAlpha)
        selected -> scheme.onSecondaryContainer
        else -> scheme.onSurface
    }
    val supportingColor = when {
        !enabled -> resolvedContent
        selected -> resolvedContent.copy(alpha = 0.78f)
        else -> scheme.onSurfaceVariant
    }

    val lines = 1 + (if (overline != null) 1 else 0) + (if (supportingText != null) 1 else 0)
    val minHeight = when {
        lines >= 3 -> M3ESize.listItemThreeLineHeight
        lines == 2 -> M3ESize.listItemTwoLineHeight
        else -> M3ESize.listItemHeight
    }

    M3ESurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .semantics { if (selected) this.selected = true },
        color = resolvedContainer,
        contentColor = resolvedContent,
        shape = shape,
        onClick = onClick,
        enabled = enabled,
        role = role,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = M3ESpacing.large,
                    end = if (trailingContent == null) M3ESpacing.large else M3ESpacing.medium,
                    top = M3ESpacing.small,
                    bottom = M3ESpacing.small
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.width(M3ESpacing.large))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(M3ESpacing.extraSmall, Alignment.CenterVertically)
            ) {
                if (overline != null) {
                    Text(
                        text = overline,
                        style = MaterialTheme.typography.labelSmall,
                        color = supportingColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = resolvedContent,
                    maxLines = if (supportingText == null) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = supportingColor,
                        maxLines = supportingTextMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingContent != null) {
                Spacer(Modifier.width(M3ESpacing.medium))
                trailingContent()
            }
        }
    }
}

/**
 * M3 list item whose headline is the emphasised role.
 *
 * Reserve this for one row per group that is the group's subject — for example the album a
 * detail page is about. Emphasising a whole list flattens the hierarchy it was meant to create.
 */
@Composable
fun M3EEmphasizedListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    shape: Shape = M3EShape.large,
    onClick: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    M3ESurface(
        modifier = modifier.fillMaxWidth(),
        level = if (selected) M3ESurfaceLevel.ContainerHigh else M3ESurfaceLevel.Container,
        shape = shape,
        color = if (selected) scheme.secondaryContainer else scheme.surfaceContainer,
        contentColor = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
        onClick = onClick,
        role = Role.Button
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(M3ESpacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.width(M3ESpacing.large))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(M3ESpacing.extraSmall)
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.emphasized.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(Modifier.width(M3ESpacing.medium))
                trailingContent()
            }
        }
    }
}

/**
 * Circular accent that holds a list row's leading icon.
 *
 * M3 uses a container role here so the icon stays legible on every surface and in both themes;
 * a raw tinted icon on the page background would fail contrast on top of album-art themes.
 */
@Composable
fun M3ELeadingIconContainer(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    size: Dp = M3ESize.iconContainer,
    content: @Composable () -> Unit
) {
    M3ESurface(
        modifier = modifier.size(size),
        shape = M3EShape.full,
        color = containerColor,
        contentColor = contentColor
    ) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            content()
        }
    }
}

private const val DisabledContentAlpha = 0.38f
