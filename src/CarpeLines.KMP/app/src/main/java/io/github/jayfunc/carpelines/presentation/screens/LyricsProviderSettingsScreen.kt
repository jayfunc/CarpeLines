package io.github.jayfunc.carpelines.presentation.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.presentation.components.CollapsibleCommonTopBar
import io.github.jayfunc.carpelines.presentation.components.MiniPlayerHeight
import io.github.jayfunc.carpelines.presentation.settings.search.settingHighlight
import io.github.jayfunc.carpelines.presentation.viewmodel.LyricsProviderSettingsViewModel
import io.github.jayfunc.carpelines.ui.m3e.M3ETextField
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.m3eContentWidth
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Lyrics provider settings page: the Apple Music token, the LibreTranslate fallback server and the
 * translation/transliteration track toggles.
 *
 * There is no save button: switches are persisted as soon as they flip, and the text fields are
 * written while typing (see [LyricsProviderSettingsViewModel]).
 */
@Composable
fun LyricsProviderSettingsScreen(
    navController: NavController,
    highlightKey: String? = null,
    viewModel: LyricsProviderSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showToken by remember { mutableStateOf(false) }

    val title = stringResource(R.string.betterlyrics_settings_title)

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 180.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

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
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(key = "intro") {
                Column(
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.betterlyrics_settings_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.betterlyrics_autosave_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            item(key = "apple_header") {
                SettingsSubsectionHeader(stringResource(R.string.betterlyrics_apple_section))
            }

            item(key = "apple_token") {
                M3ETextField(
                    value = state.appleMusicToken,
                    onValueChange = viewModel::setAppleMusicToken,
                    label = { Text(stringResource(R.string.betterlyrics_apple_token_label)) },
                    supportingText = { Text(stringResource(R.string.betterlyrics_apple_token_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (showToken) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                imageVector = if (showToken) {
                                    Icons.Rounded.VisibilityOff
                                } else {
                                    Icons.Rounded.Visibility
                                },
                                contentDescription = stringResource(
                                    if (showToken) {
                                        R.string.betterlyrics_hide_token
                                    } else {
                                        R.string.betterlyrics_show_token
                                    }
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingHighlight("item_lyrics_provider_settings", highlightKey)
                )
            }

            item(key = "libre_header") {
                Column {
                    Spacer(Modifier.height(10.dp))
                    SettingsSubsectionHeader(stringResource(R.string.betterlyrics_libre_section))
                }
            }

            item(key = "libre_server") {
                M3ETextField(
                    value = state.libreTranslateServer,
                    onValueChange = viewModel::setLibreTranslateServer,
                    label = { Text(stringResource(R.string.betterlyrics_libre_server_label)) },
                    supportingText = { Text(stringResource(R.string.betterlyrics_libre_server_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "tracks_header") {
                Column {
                    Spacer(Modifier.height(10.dp))
                    SettingsSubsectionHeader(stringResource(R.string.betterlyrics_tracks_section))
                }
            }

            item(key = "translation_toggle") {
                SwitchSettingItem(
                    title = stringResource(R.string.betterlyrics_translation_toggle),
                    subtitle = stringResource(R.string.betterlyrics_translation_toggle_subtitle),
                    checked = state.translationEnabled,
                    onCheckedChange = viewModel::setTranslationEnabled,
                    leadingIcon = {
                        Icon(Icons.Rounded.Translate, null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    shape = M3EShape.groupedCards(index = 0, count = 2)
                )
            }

            item(key = "romanization_toggle") {
                SwitchSettingItem(
                    title = stringResource(R.string.betterlyrics_romanization_toggle),
                    subtitle = stringResource(R.string.betterlyrics_romanization_toggle_subtitle),
                    checked = state.romanizationEnabled,
                    onCheckedChange = viewModel::setRomanizationEnabled,
                    leadingIcon = {
                        Icon(Icons.Rounded.Translate, null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    shape = M3EShape.groupedCards(index = 1, count = 2)
                )
            }
        }

        CollapsibleCommonTopBar(
            title = title,
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = { navController.popBackStack() }
        )
    }
}
