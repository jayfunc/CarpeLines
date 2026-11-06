@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.jayfunc.carpelines.presentation.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.update.AppRelease
import io.github.jayfunc.carpelines.data.update.UpdateChannel
import io.github.jayfunc.carpelines.data.update.UpdateError
import io.github.jayfunc.carpelines.data.update.UpdateStatus
import io.github.jayfunc.carpelines.presentation.components.CollapsibleCommonTopBar
import io.github.jayfunc.carpelines.presentation.components.MiniPlayerHeight
import io.github.jayfunc.carpelines.presentation.viewmodel.UpdatesUiState
import io.github.jayfunc.carpelines.presentation.viewmodel.UpdatesViewModel
import io.github.jayfunc.carpelines.ui.m3e.M3EAction
import io.github.jayfunc.carpelines.ui.m3e.M3EActionEmphasis
import io.github.jayfunc.carpelines.ui.m3e.M3EConnectedActions
import io.github.jayfunc.carpelines.ui.m3e.M3EConnectedOrientation
import io.github.jayfunc.carpelines.ui.m3e.M3ESecondaryButton
import io.github.jayfunc.carpelines.ui.m3e.M3ETonalButton
import io.github.jayfunc.carpelines.ui.theme.m3eContentWidth
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun UpdatesScreen(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    viewModel: UpdatesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var switchRelease by rememberSaveable { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    val title = stringResource(R.string.updates_title)
    val isLongTitle = title.length > 13

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = if (isLongTitle) 200.dp else 180.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val titleMaxLines = if (isLongTitle) 2 else 1

    val topBarHeight = remember(maxTopBarHeightPx) { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableStateOf(0f) }

    LaunchedEffect(topBarHeight.value, maxTopBarHeightPx) {
        collapseFraction = 1f - (
            (topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)
            ).coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0

                if (
                    !isScrollingDown &&
                    (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)
                ) {
                    return Offset.Zero
                }

                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(newHeight) }
                }

                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand =
                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0

            val targetValue = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx

            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.m3eContentWidth().fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = currentTopBarHeightDp + 8.dp,
                end = 16.dp,
                bottom = MiniPlayerHeight +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "status") {
                UpdateStatusCard(
                    state = state,
                    onCheck = viewModel::checkNow,
                    onInstall = viewModel::downloadAndInstall,
                    onCancel = viewModel::cancel,
                    onDismiss = viewModel::dismissError,
                    onAllowInstalls = {
                        context.startActivitySafely(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
                        )
                    },
                    onOpenPrompt = { intent -> context.startActivitySafely(Intent(intent)) },
                    onOpenUrl = { url -> context.openUrl(url) },
                    onShowSwitch = { switchRelease = it.versionName },
                )
            }
            item(key = "settings") {
                ExpressiveSettingsGroup {
                    ThemeSelectorItem(
                        label = stringResource(R.string.updates_channel_title),
                        description = stringResource(
                            when (state.selectedChannel) {
                                UpdateChannel.STABLE -> R.string.updates_channel_stable_description
                                UpdateChannel.BETA -> R.string.updates_channel_beta_description
                                UpdateChannel.ALPHA -> R.string.updates_channel_alpha_description
                            }
                        ),
                        options = mapOf(
                            UpdateChannel.STABLE.storageKey to stringResource(R.string.updates_channel_stable_source),
                            UpdateChannel.BETA.storageKey to stringResource(R.string.updates_channel_beta_source),
                            UpdateChannel.ALPHA.storageKey to stringResource(R.string.updates_channel_alpha_source),
                        ),
                        optionDescriptions = mapOf(
                            UpdateChannel.STABLE.storageKey to stringResource(R.string.updates_channel_stable_description),
                            UpdateChannel.BETA.storageKey to stringResource(R.string.updates_channel_beta_description),
                            UpdateChannel.ALPHA.storageKey to stringResource(R.string.updates_channel_alpha_description),
                        ),
                        selectedKey = state.selectedChannel.storageKey,
                        onSelectionChanged = { key ->
                            UpdateChannel.fromStorageKey(key)?.let(viewModel::setChannel)
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    SwitchSettingItem(
                        title = stringResource(R.string.updates_auto_check_title),
                        subtitle = stringResource(R.string.updates_auto_check_subtitle),
                        checked = state.autoCheckEnabled,
                        onCheckedChange = viewModel::setAutoCheck,
                        leadingIcon = {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                    )
                }
            }
        }

        CollapsibleCommonTopBar(
            title = title,
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = onBack,
            maxLines = titleMaxLines
        )
    }

    val pendingSwitch = (state.status as? UpdateStatus.Available)?.release
        ?.takeIf { switchRelease != null && it.versionName == switchRelease }
    if (pendingSwitch != null) {
        ChannelSwitchDialog(
            release = pendingSwitch,
            onBackup = {
                switchRelease = null
                onOpenBackup()
            },
            onDownload = {
                // Both channels publish APKs on GitHub now, so the direct download works either way.
                context.openUrl(pendingSwitch.apkUrl)
            },
            onUninstall = {
                context.startActivitySafely(Intent(Intent.ACTION_DELETE, "package:${context.packageName}".toUri()))
            },
            onDismiss = { switchRelease = null }
        )
    }
}

@Composable
private fun UpdateStatusCard(
    state: UpdatesUiState,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onAllowInstalls: () -> Unit,
    onOpenPrompt: (Intent) -> Unit,
    onOpenUrl: (String) -> Unit,
    onShowSwitch: (AppRelease) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.updates_installed_version, state.installedVersion),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(channelSource(state.installedChannel)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = statusMessage(state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            when (val status = state.status) {
                UpdateStatus.Idle -> StatusActions(
                    M3EAction(
                        label = stringResource(R.string.updates_action_check),
                        onClick = onCheck,
                        icon = Icons.Rounded.Refresh,
                        emphasis = M3EActionEmphasis.Filled
                    )
                )
                UpdateStatus.Checking -> LoadingIndicator(modifier = Modifier.size(28.dp))
                is UpdateStatus.UpToDate -> StatusActions(
                    M3EAction(
                        label = stringResource(R.string.updates_action_check_again),
                        onClick = onCheck,
                        icon = Icons.Rounded.Refresh
                    )
                )
                is UpdateStatus.Available -> {
                    // Two related actions on the same release become one connected bar; the
                    // primary segment keeps the filled emphasis, the secondary stays tonal.
                    StatusActions(
                        if (status.requiresReinstall) {
                            M3EAction(
                                label = stringResource(R.string.updates_action_switch),
                                onClick = { onShowSwitch(status.release) },
                                icon = Icons.Rounded.SystemUpdate,
                                emphasis = M3EActionEmphasis.Filled
                            )
                        } else {
                            M3EAction(
                                label = stringResource(R.string.updates_action_install),
                                onClick = onInstall,
                                icon = Icons.Rounded.Download,
                                emphasis = M3EActionEmphasis.Filled
                            )
                        },
                        M3EAction(
                            label = stringResource(R.string.updates_action_release_notes),
                            onClick = { onOpenUrl(status.release.pageUrl) },
                            icon = Icons.AutoMirrored.Rounded.Article
                        )
                    )
                }
                is UpdateStatus.Downloading -> {
                    if (status.progress != null) {
                        LinearProgressIndicator(progress = { status.progress }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    StatusActions(
                        M3EAction(
                            label = stringResource(R.string.updates_action_cancel),
                            onClick = onCancel,
                            icon = Icons.Rounded.Close,
                            emphasis = M3EActionEmphasis.Outlined
                        )
                    )
                }
                is UpdateStatus.AwaitingInstallPermission -> StatusActions(
                    M3EAction(
                        label = stringResource(R.string.updates_action_allow_installs),
                        onClick = onAllowInstalls,
                        icon = Icons.Rounded.Settings,
                        emphasis = M3EActionEmphasis.Filled
                    ),
                    M3EAction(
                        label = stringResource(R.string.updates_action_cancel),
                        onClick = onCancel,
                        icon = Icons.Rounded.Close,
                        emphasis = M3EActionEmphasis.Outlined
                    )
                )
                // The committed session is decided in the system prompt (its Cancel aborts it).
                is UpdateStatus.AwaitingConfirmation -> StatusActions(
                    M3EAction(
                        label = stringResource(R.string.updates_action_open_prompt),
                        onClick = { onOpenPrompt(status.confirmIntent) },
                        icon = Icons.Rounded.PlayArrow,
                        emphasis = M3EActionEmphasis.Filled
                    )
                )
                is UpdateStatus.Installing -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is UpdateStatus.Failed -> StatusActions(
                    M3EAction(
                        label = stringResource(R.string.updates_action_check_again),
                        onClick = onCheck,
                        icon = Icons.Rounded.Refresh,
                        emphasis = M3EActionEmphasis.Filled
                    ),
                    M3EAction(
                        label = stringResource(R.string.updates_action_dismiss),
                        onClick = onDismiss,
                        icon = Icons.Rounded.Close,
                        emphasis = M3EActionEmphasis.Outlined
                    )
                )
            }
        }
    }
}

/**
 * The status card's actions as one horizontal connected M3 Expressive bar.
 *
 * A single action becomes a pill hugging its label; related actions share one row, one
 * silhouette and a 2dp hairline, with evenly weighted segments. Stacked full-width filled +
 * outlined buttons (the previous design) read as a Material 2 form rather than a set of related
 * actions.
 */
@Composable
private fun StatusActions(vararg actions: M3EAction) {
    M3EConnectedActions(
        actions = actions.toList(),
        orientation = M3EConnectedOrientation.Horizontal
    )
}

@Composable
private fun statusMessage(state: UpdatesUiState): String = when (val status = state.status) {
    UpdateStatus.Idle -> stringResource(R.string.updates_status_idle)
    UpdateStatus.Checking -> stringResource(R.string.updates_status_checking)
    is UpdateStatus.UpToDate -> stringResource(
        R.string.updates_status_up_to_date,
        stringResource(channelLabel(status.channel))
    )
    is UpdateStatus.Available -> when {
        !status.requiresReinstall -> stringResource(R.string.updates_status_available, status.release.versionName)
        status.release.channel != state.installedChannel -> stringResource(
            R.string.updates_status_reinstall,
            status.release.versionName,
            stringResource(channelLabel(status.release.channel))
        )
        else -> stringResource(R.string.updates_status_reinstall_signature, status.release.versionName)
    }
    is UpdateStatus.Downloading -> status.progress
        ?.let { stringResource(R.string.updates_status_downloading_progress, status.release.versionName, (it * 100).toInt()) }
        ?: stringResource(R.string.updates_status_downloading, status.release.versionName)
    is UpdateStatus.AwaitingInstallPermission -> stringResource(R.string.updates_status_permission, status.release.versionName)
    is UpdateStatus.AwaitingConfirmation -> stringResource(R.string.updates_status_confirm, status.release.versionName)
    is UpdateStatus.Installing -> stringResource(R.string.updates_status_installing, status.release.versionName)
    is UpdateStatus.Failed -> stringResource(
        when (status.error) {
            UpdateError.CHECK_FAILED -> R.string.updates_error_check
            UpdateError.NOTHING_PUBLISHED -> R.string.updates_error_nothing_published
            UpdateError.DOWNLOAD_FAILED -> R.string.updates_error_download
            UpdateError.INVALID_APK -> R.string.updates_error_invalid_apk
            UpdateError.NOT_NEWER -> R.string.updates_error_not_newer
            UpdateError.INSTALL_FAILED -> R.string.updates_error_install
        }
    )
}

@Composable
private fun ChannelSwitchDialog(
    release: AppRelease,
    onBackup: () -> Unit,
    onDownload: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.updates_switch_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.updates_switch_dialog_body))
                Text(stringResource(R.string.updates_switch_step_backup))
                M3ETonalButton(
                    label = stringResource(R.string.updates_switch_action_backup),
                    onClick = onBackup,
                    icon = Icons.Rounded.Save,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.updates_switch_step_download_alpha, release.versionName))
                M3ETonalButton(
                    label = stringResource(R.string.updates_switch_action_download),
                    onClick = onDownload,
                    icon = Icons.Rounded.Download,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.updates_switch_step_uninstall))
                M3ESecondaryButton(
                    label = stringResource(R.string.updates_switch_action_uninstall),
                    onClick = onUninstall,
                    icon = Icons.Rounded.Delete,
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.updates_switch_action_close)) }
        }
    )
}

private fun channelLabel(channel: UpdateChannel): Int = when (channel) {
    UpdateChannel.STABLE -> R.string.updates_channel_stable
    UpdateChannel.BETA -> R.string.updates_channel_beta
    UpdateChannel.ALPHA -> R.string.updates_channel_alpha
}

private fun channelSource(channel: UpdateChannel): Int = when (channel) {
    UpdateChannel.STABLE -> R.string.updates_channel_stable_source
    UpdateChannel.BETA -> R.string.updates_channel_beta_source
    UpdateChannel.ALPHA -> R.string.updates_channel_alpha_source
}

private fun Context.openUrl(url: String) = startActivitySafely(Intent(Intent.ACTION_VIEW, url.toUri()))

private fun Context.startActivitySafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (error: ActivityNotFoundException) {
        Timber.w(error, "No activity for $intent")
    }
}
