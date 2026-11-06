@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESize
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import io.github.jayfunc.carpelines.ui.theme.ShapeCache
import io.github.jayfunc.carpelines.ui.theme.emphasized

/**
 * Empty state.
 *
 * An empty state must answer three questions in order: what is missing, why, and what the user
 * can do next. The action is therefore part of the component rather than something a screen
 * bolts on afterwards.
 *
 * The hero glyph sits in an M3 Expressive soft-burst container — one deliberate expressive
 * moment for the whole screen, not decoration on every state.
 */
@Composable
fun M3EEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    M3EEmptyState(
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(M3ESize.iconLarge)) },
        title = title,
        modifier = modifier,
        description = description,
        action = action
    )
}

/** Slot-based variant, for product artwork that ships as a drawable rather than a vector icon. */
@Composable
fun M3EEmptyState(
    icon: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    M3EStateScaffold(
        modifier = modifier,
        iconContent = icon,
        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        title = title,
        description = description,
        action = action
    )
}

/**
 * Error state.
 *
 * Errors use the `errorContainer` pair so they are recognisable without relying on a red icon
 * alone, and carry a recovery action. An error without a way forward is a dead end.
 */
@Composable
fun M3EErrorState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    M3EErrorState(
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(M3ESize.iconLarge)) },
        title = title,
        modifier = modifier,
        description = description,
        action = action
    )
}

/** Slot-based variant, for product artwork that ships as a drawable rather than a vector icon. */
@Composable
fun M3EErrorState(
    icon: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    M3EStateScaffold(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        iconContent = icon,
        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
        title = title,
        description = description,
        action = action
    )
}

@Composable
private fun M3EStateScaffold(
    iconContent: @Composable () -> Unit,
    iconContainerColor: androidx.compose.ui.graphics.Color,
    iconContentColor: androidx.compose.ui.graphics.Color,
    title: String,
    description: String?,
    action: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .padding(horizontal = M3ESpacing.extraLarge, vertical = M3ESpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(M3ESpacing.large, Alignment.CenterVertically)
    ) {
        M3ESurface(
            modifier = Modifier.size(M3ESize.stateHero),
            shape = ShapeCache.expressiveHero,
            color = iconContainerColor,
            contentColor = iconContentColor
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(M3ESize.stateHero)
            ) {
                iconContent()
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(M3ESpacing.small)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.emphasized.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (action != null) {
            action()
        }
    }
}

/**
 * Loading state.
 *
 * Pass [progress] in `0f..1f` when the work has a known length; the indicator becomes a
 * determinate wave and the node reports `progressBarRangeInfo` so screen readers can announce
 * progress. Omit it for indeterminate work, which only announces that something is happening.
 *
 * The whole block is a polite live region: it is announced when it appears, without stealing
 * focus from what the user was doing.
 */
@Composable
fun M3ELoadingState(
    modifier: Modifier = Modifier,
    label: String? = null,
    progress: Float? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                if (progress != null) {
                    progressBarRangeInfo =
                        androidx.compose.ui.semantics.ProgressBarRangeInfo(progress, 0f..1f)
                }
            }
            .padding(horizontal = M3ESpacing.extraLarge, vertical = M3ESpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(M3ESpacing.large, Alignment.CenterVertically)
    ) {
        if (progress == null) {
            LoadingIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Placeholder block for content that is still resolving.
 *
 * Skeletons keep the final layout stable while data arrives, which avoids the layout jump that
 * a spinner causes. They are hidden from accessibility on purpose: announcing placeholder boxes
 * is noise, the accompanying [M3ELoadingState] carries the state.
 */
@Composable
fun M3ESkeletonBlock(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 16.dp
) {
    M3ESurface(
        modifier = modifier
            .height(height)
            .semantics { },
        shape = M3EShape.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {}
}
