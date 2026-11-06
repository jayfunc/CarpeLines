package io.github.jayfunc.carpelines.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Slider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.worker.SyncProgress
import io.github.jayfunc.carpelines.presentation.viewmodel.LyricsRefreshProgress
import io.github.jayfunc.carpelines.ui.m3e.M3EAction
import io.github.jayfunc.carpelines.ui.m3e.M3EActionEmphasis
import io.github.jayfunc.carpelines.ui.m3e.M3EConnectedActions
import io.github.jayfunc.carpelines.ui.m3e.M3EListItem
import io.github.jayfunc.carpelines.ui.m3e.M3ESurface
import io.github.jayfunc.carpelines.ui.m3e.M3ESurfaceLevel
import io.github.jayfunc.carpelines.ui.m3e.M3ESwitch
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESize
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import io.github.jayfunc.carpelines.ui.theme.emphasized
import io.github.jayfunc.carpelines.ui.theme.RoundedSans
import androidx.compose.ui.res.vectorResource
import androidx.core.view.HapticFeedbackConstantsCompat
import io.github.jayfunc.carpelines.presentation.utils.LocalAppHapticsConfig
import io.github.jayfunc.carpelines.presentation.utils.performAppCompatHapticFeedback
import io.github.jayfunc.carpelines.presentation.components.rememberSyncedSliderState

@Composable
fun SettingsSection(title: String, icon: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(
            horizontal = M3ESpacing.large,
            vertical = M3ESpacing.small
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = M3ESize.minimumTarget)
                .padding(top = M3ESpacing.small)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.emphasized.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        content()
    }
}

@Composable
fun SettingsItem(
        title: String,
        subtitle: String,
        leadingIcon: @Composable () -> Unit,
        trailingIcon: @Composable () -> Unit = {},
        modifier: Modifier = Modifier,
        supportingTextMaxLines: Int = 2,
        shape: Shape = M3EShape.medium,
        onClick: () -> Unit
) {
    M3EListItem(
        headline = title,
        supportingText = subtitle,
        modifier = modifier,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        supportingTextMaxLines = supportingTextMaxLines,
        onClick = onClick,
        leadingContent = { leadingIcon() },
        trailingContent = trailingIcon
    )
}

@Composable
fun SwitchSettingItem(
        title: String,
        subtitle: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        leadingIcon: @Composable (() -> Unit)? = null,
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
        actionContent: (@Composable () -> Unit)? = null,
        shape: Shape = M3EShape.medium
) {
    val view = LocalView.current
    val appHapticsConfig = LocalAppHapticsConfig.current

    M3ESurface(
        modifier = modifier.fillMaxWidth(),
        level = M3ESurfaceLevel.Container,
        shape = shape
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = M3ESpacing.large, vertical = M3ESpacing.medium)
                    .fillMaxWidth()
                    .heightIn(min = M3ESize.minimumTarget),
                horizontalArrangement = Arrangement.spacedBy(M3ESpacing.medium)
        ) {
            if (leadingIcon != null) {
                Box(
                        modifier = Modifier.size(M3ESize.iconMedium),
                        contentAlignment = Alignment.Center
                ) { leadingIcon() }
            }

            Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(M3ESpacing.extraSmall)
            ) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color =
                                if (enabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                                if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }

            if (actionContent != null) {
                actionContent()
            }

            M3ESwitch(
                checked = checked,
                onCheckedChange = { newValue ->
                    if (enabled) {
                        performAppCompatHapticFeedback(
                            view,
                            appHapticsConfig,
                            HapticFeedbackConstantsCompat.GESTURE_START
                        )
                        onCheckedChange(newValue)
                    }
                },
                enabled = enabled
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorItem(
        label: String,
        description: String,
        options: Map<String, String>,
        optionDescriptions: Map<String, String> = emptyMap(),
        disabledOptionKeys: Set<String> = emptySet(),
        selectedKey: String,
        onSelectionChanged: (String) -> Unit,
        leadingIcon: @Composable () -> Unit,
        modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val selectedOption = options[selectedKey] ?: selectedKey

    // `Surface(onClick = …)` instead of a clip + clickable: the M3 surface owns the press state,
    // the shape and the button semantics for a whole-row target.
    Surface(
            onClick = { showSheet = true },
            shape = M3EShape.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(end = 16.dp).size(24.dp),
                        contentAlignment = Alignment.Center
                ) { leadingIcon() }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = M3EShape.full,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                             text = selectedOption,
                             style = MaterialTheme.typography.labelMedium,
                             color = MaterialTheme.colorScheme.primary,
                             fontWeight = FontWeight.Bold,
                             modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        OptionSelectionSheet(
            title = label,
            options = options,
            selectedKey = selectedKey,
            onSelect = { key ->
                onSelectionChanged(key)
                showSheet = false
            },
            onDismiss = { showSheet = false },
            optionDescriptions = optionDescriptions,
            disabledOptionKeys = disabledOptionKeys
        )
    }
}

/**
 * Bottom sheet listing [options] (key to label) with [selectedKey] checked. [onSelect] receives
 * the tapped key; callers decide whether to dismiss. Keys in [disabledOptionKeys] are shown but
 * cannot be picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionSelectionSheet(
        title: String,
        options: Map<String, String>,
        selectedKey: String,
        onSelect: (String) -> Unit,
        onDismiss: () -> Unit,
        optionDescriptions: Map<String, String> = emptyMap(),
        disabledOptionKeys: Set<String> = emptySet()
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                fontWeight = FontWeight.Bold
            )

            // Same treatment as a settings subsection: rows sit 2dp apart as their own `medium`
            // cards, and the ends of the group take the subsection container's corner step.
            val optionEntries = options.entries.toList()
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(optionEntries) { index, option ->
                    val (key, optionLabel) = option
                    val isSelected = key == selectedKey
                    val isEnabled = key !in disabledOptionKeys
                    val optionDescription = optionDescriptions[key]
                    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    val resolvedContentColor = if (isEnabled) {
                        contentColor
                    } else {
                        contentColor.copy(alpha = 0.38f)
                    }
                    val resolvedDescriptionColor = contentColor.copy(
                        alpha = if (isEnabled) 0.75f else 0.38f
                    )

                    Surface(
                        onClick = { onSelect(key) },
                        enabled = isEnabled,
                        shape = M3EShape.groupedCards(index, optionEntries.size),
                        color = containerColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp)
                            .semantics(mergeDescendants = true) {
                                role = Role.RadioButton
                                selected = isSelected
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).padding(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = optionLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = resolvedContentColor
                                )
                                if (optionDescription != null) {
                                    Text(
                                        text = optionDescription,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = resolvedDescriptionColor
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = resolvedContentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpressiveSettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(M3EShape.extraLarge)
            .background(Color.Transparent),
    ) {
        content()
    }
}

@Composable
fun SliderSettingsItem(
        label: String,
        value: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        onValueChange: (Float) -> Unit,
        onValueChangeFinished: (() -> Unit)? = null,
        valueText: (Float) -> String,
        modifier: Modifier = Modifier
) {
    Surface(
            shape = M3EShape.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = valueText(value),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                )
            }
            Slider(
                state = rememberSyncedSliderState(
                    value = value,
                    steps = steps,
                    valueRange = valueRange
                ),
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                // Without a label the reader only announces a number, not which setting it is.
                modifier = Modifier.semantics {
                    contentDescription = label
                    stateDescription = valueText(value)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshLibraryItem(
        isSyncing: Boolean,
        syncProgress: SyncProgress,
        activeOperationLabel: String? = null,
        onFullSync: () -> Unit,
        onRebuild: () -> Unit
) {
    Surface(
            shape = M3EShape.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                        modifier = Modifier.padding(end = 16.dp).size(24.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                            text = stringResource(R.string.presentation_batch_f_refresh_library_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text = stringResource(R.string.presentation_batch_f_refresh_library_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            FilledTonalButton(
                    onClick = onFullSync,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.presentation_batch_f_full_rescan))
                }
            }
             
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                    onClick = onRebuild,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.presentation_batch_f_rebuild_database))
                }
            }

            if (isSyncing) {
                Spacer(modifier = Modifier.height(12.dp))
                val phaseLabel = activeOperationLabel ?: syncPhaseLabel(syncProgress.phase)
                if (syncProgress.hasProgress) {
                    LinearWavyProgressIndicator(
                            progress = { syncProgress.progress },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text = stringResource(
                                R.string.presentation_batch_f_sync_progress_detailed,
                                phaseLabel,
                                (syncProgress.progress * 100).toInt(),
                                syncProgress.currentCount,
                                syncProgress.totalCount
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text = stringResource(
                                R.string.presentation_batch_f_sync_progress_indeterminate,
                                phaseLabel
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun syncPhaseLabel(phase: SyncProgress.SyncPhase): String =
        stringResource(
                when (phase) {
                    SyncProgress.SyncPhase.IDLE -> R.string.presentation_batch_f_sync_phase_preparing
                    SyncProgress.SyncPhase.FETCHING_MEDIASTORE ->
                            R.string.presentation_batch_f_sync_phase_reading_mediastore
                    SyncProgress.SyncPhase.PROCESSING_FILES ->
                            R.string.presentation_batch_f_sync_phase_processing_tracks
                    SyncProgress.SyncPhase.SAVING_TO_DATABASE ->
                            R.string.presentation_batch_f_sync_phase_saving_db
                    SyncProgress.SyncPhase.SCANNING_LRC -> R.string.presentation_batch_f_sync_phase_scanning_lrc
                    SyncProgress.SyncPhase.CLEANING_CACHE ->
                            R.string.presentation_batch_f_sync_phase_cleaning_cache
                    SyncProgress.SyncPhase.SYNCING_CLOUD ->
                            R.string.presentation_batch_f_sync_phase_syncing_cloud
                    SyncProgress.SyncPhase.COMPLETING -> R.string.presentation_batch_f_sync_phase_completing
                }
        )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshLyricsItem(
        isRefreshing: Boolean,
        progress: LyricsRefreshProgress,
        onRefresh: () -> Unit
) {
    Surface(
            shape = M3EShape.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                        modifier = Modifier.padding(end = 16.dp).size(24.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            painter = painterResource(id = R.drawable.rounded_lyrics_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                            text = stringResource(R.string.presentation_batch_f_refresh_lyrics_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text = stringResource(R.string.presentation_batch_f_refresh_lyrics_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledIconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        colors =
                                IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                ) {
                    Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = stringResource(R.string.presentation_batch_f_cd_refresh_lyrics),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (isRefreshing && progress.hasProgress) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearWavyProgressIndicator(
                        progress = { progress.progress },
                        modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = stringResource(
                            R.string.presentation_batch_f_refresh_lyrics_processing,
                            progress.currentCount,
                            progress.totalSongs
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActionSettingsItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = M3EShape.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(end = 16.dp).size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(M3ESpacing.large))

            // One lone action is a pill that hugs its label; a second related action turns the
            // pair into a single connected bar instead of two stacked, unrelated buttons.
            M3EConnectedActions(
                actions = buildList {
                    add(
                        M3EAction(
                            label = primaryActionLabel,
                            onClick = onPrimaryAction,
                            emphasis = M3EActionEmphasis.Tonal,
                            enabled = enabled
                        )
                    )
                    if (secondaryActionLabel != null && onSecondaryAction != null) {
                        add(
                            M3EAction(
                                label = secondaryActionLabel,
                                onClick = onSecondaryAction,
                                emphasis = M3EActionEmphasis.Outlined,
                                enabled = enabled
                            )
                        )
                    }
                }
            )
        }
    }
}
