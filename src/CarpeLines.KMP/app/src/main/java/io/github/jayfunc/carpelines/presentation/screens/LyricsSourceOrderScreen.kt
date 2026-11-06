package io.github.jayfunc.carpelines.presentation.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.betterlyrics.LyricsSourceId
import io.github.jayfunc.carpelines.presentation.components.CollapsibleCommonTopBar
import io.github.jayfunc.carpelines.presentation.components.MiniPlayerHeight
import io.github.jayfunc.carpelines.presentation.model.SettingsCategory
import io.github.jayfunc.carpelines.presentation.navigation.Screen
import io.github.jayfunc.carpelines.presentation.navigation.navigateSafely
import io.github.jayfunc.carpelines.presentation.utils.LocalAppHapticsConfig
import io.github.jayfunc.carpelines.presentation.utils.performAppCompatHapticFeedback
import io.github.jayfunc.carpelines.presentation.viewmodel.LyricsSourceOrderViewModel
import io.github.jayfunc.carpelines.ui.theme.m3eContentWidth
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun LyricsSourceOrderScreen(
    navController: NavController,
    viewModel: LyricsSourceOrderViewModel = hiltViewModel(),
) {
    val order by viewModel.order.collectAsStateWithLifecycle()
    val disabled = LyricsSourceId.entries.filterNot { order.contains(it) }

    val title = stringResource(R.string.lyrics_source_order_title)
    val isLongTitle = title.length > 13

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val view = LocalView.current
    val appHapticsConfig = LocalAppHapticsConfig.current

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

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            val fromSource = order.sourceForKey(from.key) ?: return@rememberReorderableLazyListState
            val toSource = order.sourceForKey(to.key) ?: return@rememberReorderableLazyListState
            val targetIndex = order.indexOf(toSource)
            if (targetIndex < 0 || order.indexOf(fromSource) == targetIndex) {
                return@rememberReorderableLazyListState
            }
            viewModel.moveTo(fromSource, targetIndex)
            performAppCompatHapticFeedback(
                view,
                appHapticsConfig,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    )

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
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(key = "intro") {
                Text(
                    text = stringResource(R.string.lyrics_source_order_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp),
                )
            }

            // A local source can only answer for a track the library scanned, and the scan lives
            // on its own page: without this shortcut the list looks broken.
            item(key = "local_music_action") {
                SettingsItem(
                    title = stringResource(R.string.settings_category_local_music_title),
                    subtitle = stringResource(R.string.lyrics_source_order_local_music_hint),
                    leadingIcon = {
                        Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    trailingIcon = {
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = settingsGroupItemShape(index = 0, count = 2),
                    onClick = {
                        navController.navigateSafely(
                            Screen.SettingsCategory.createRoute(SettingsCategory.LOCAL_MUSIC.id)
                        )
                    },
                )
            }
            item(key = "reset_action") {
                SettingsItem(
                    title = stringResource(R.string.lyrics_source_order_reset),
                    subtitle = stringResource(R.string.lyrics_source_order_reset_subtitle),
                    leadingIcon = {
                        Icon(Icons.Rounded.RestartAlt, null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    trailingIcon = {
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = settingsGroupItemShape(index = 1, count = 2),
                    onClick = viewModel::reset,
                )
            }

            item(key = "enabled_header") {
                Column {
                    Spacer(Modifier.height(10.dp))
                    SettingsSubsectionHeader(stringResource(R.string.lyrics_source_order_enabled))
                }
            }

            itemsIndexed(order, key = { _, source -> source.name }) { index, source ->
                ReorderableItem(reorderableState, key = source.name) { _ ->
                    SwitchSettingItem(
                        title = sourceTitle(source),
                        subtitle = sourceOriginLabel(source),
                        checked = true,
                        onCheckedChange = { viewModel.setEnabled(source, it) },
                        leadingIcon = { SourceLeadingIcon(source) },
                        actionContent = {
                            FilledIconButton(
                                onClick = {},
                                modifier = Modifier
                                    .draggableHandle(
                                        onDragStarted = {
                                            performAppCompatHapticFeedback(
                                                view,
                                                appHapticsConfig,
                                                HapticFeedbackConstants.GESTURE_START
                                            )
                                        },
                                        onDragStopped = {
                                            performAppCompatHapticFeedback(
                                                view,
                                                appHapticsConfig,
                                                HapticFeedbackConstants.GESTURE_END
                                            )
                                        }
                                    )
                                    .size(40.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragIndicator,
                                    contentDescription = stringResource(R.string.cd_drag_handle)
                                )
                            }
                        },
                        shape = settingsGroupItemShape(index = index, count = order.size),
                    )
                }
            }

            if (disabled.isNotEmpty()) {
                item(key = "disabled_header") {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        SettingsSubsectionHeader(stringResource(R.string.lyrics_source_order_disabled))
                    }
                }
                itemsIndexed(disabled, key = { _, source -> source.name }) { index, source ->
                    SwitchSettingItem(
                        title = sourceTitle(source),
                        subtitle = sourceOriginLabel(source),
                        checked = false,
                        onCheckedChange = { viewModel.setEnabled(source, it) },
                        leadingIcon = { SourceLeadingIcon(source) },
                        shape = settingsGroupItemShape(index = index, count = disabled.size),
                    )
                }
            }
        }

        CollapsibleCommonTopBar(
            title = title,
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = { navController.popBackStack() },
            maxLines = titleMaxLines
        )
    }
}

/** Resolves a reorderable item key back to the source it represents, ignoring non-source rows. */
private fun List<LyricsSourceId>.sourceForKey(key: Any?): LyricsSourceId? =
    (key as? String)?.let { name -> firstOrNull { it.name == name } }

@Composable
private fun SourceLeadingIcon(source: LyricsSourceId) {
    Icon(
        imageVector = if (source.isLocal) Icons.Rounded.MusicNote else Icons.Rounded.Cloud,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun sourceOriginLabel(source: LyricsSourceId): String = stringResource(
    if (source.isLocal) R.string.lyrics_source_order_local else R.string.lyrics_source_order_online
)

@Composable
private fun sourceTitle(source: LyricsSourceId): String = stringResource(
    when (source) {
        LyricsSourceId.EMBEDDED -> R.string.lyrics_source_embedded
        LyricsSourceId.LOCAL_LRC -> R.string.lyrics_source_local_lrc
        LyricsSourceId.QQ_MUSIC -> R.string.lyrics_source_qq
        LyricsSourceId.NETEASE -> R.string.lyrics_source_netease
        LyricsSourceId.KUGOU -> R.string.lyrics_source_kugou
        LyricsSourceId.LRCLIB -> R.string.lyrics_source_lrclib
        LyricsSourceId.APPLE_MUSIC -> R.string.lyrics_source_apple
    }
)
