package io.github.jayfunc.carpelines.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsCandidate
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor
import io.github.jayfunc.carpelines.data.external.ExternalMediaState
import io.github.jayfunc.carpelines.lyrics.model.LyricsDocument
import io.github.jayfunc.carpelines.presentation.components.CollapsibleCommonTopBar
import io.github.jayfunc.carpelines.presentation.components.MiniPlayerHeight
import io.github.jayfunc.carpelines.presentation.components.SmartImage
import io.github.jayfunc.carpelines.presentation.components.rememberAppMessage
import io.github.jayfunc.carpelines.presentation.viewmodel.BetterLyricsSearchViewModel
import io.github.jayfunc.carpelines.presentation.viewmodel.BetterLyricsSearchViewModel.PreviewTrack
import io.github.jayfunc.carpelines.ui.m3e.M3EEmptyState
import io.github.jayfunc.carpelines.ui.m3e.M3EListItem
import io.github.jayfunc.carpelines.ui.m3e.M3EModalBottomSheet
import io.github.jayfunc.carpelines.ui.m3e.M3ESurface
import io.github.jayfunc.carpelines.ui.m3e.M3ESurfaceLevel
import io.github.jayfunc.carpelines.ui.m3e.M3ETextField
import io.github.jayfunc.carpelines.ui.theme.LocalReducedMotion
import io.github.jayfunc.carpelines.ui.theme.M3EMotion
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESize
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import io.github.jayfunc.carpelines.ui.theme.m3eContentWidth
import io.github.jayfunc.carpelines.utils.formatDuration
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Manual lyrics search and selection, the Android counterpart of BetterLyrics'
 * "Lyrics Search" window: edit the metadata, search every enabled provider, preview a result and
 * keep that exact version for the current track.
 *
 * Rows follow the settings pages: flat `surfaceContainer` cards on the 12dp/28dp grouped shape
 * scale, 2dp apart inside a group and 10dp between groups.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BetterLyricsSearchScreen(
    navController: NavController,
    viewModel: BetterLyricsSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val media by ExternalMediaMonitor.state.collectAsStateWithLifecycle()
    val mediaState = media
    val showMessage = rememberAppMessage()
    val context = LocalContext.current

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val title = stringResource(R.string.lyrics_manual_search_title)
    val subtitle = stringResource(R.string.lyrics_manual_search_subtitle)

    // Same collapsing header as the settings pages: the bar shrinks while the list scrolls under it.
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 180.dp
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }
    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    val collapseFraction = 1f - (
        (topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)
        ).coerceIn(0f, 1f)

    LaunchedEffect(mediaState?.identity) {
        viewModel.syncWithMedia(mediaState)
    }

    val nestedScrollConnection = remember(minTopBarHeightPx, maxTopBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val scrollingUp = available.y < 0f
                if (!scrollingUp && (
                        lazyListState.firstVisibleItemIndex > 0 ||
                            lazyListState.firstVisibleItemScrollOffset > 0
                        )
                ) return Offset.Zero

                val previous = topBarHeight.value
                val next = (previous + available.y).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = next - previous
                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(next) }
                }
                return if (scrollingUp && next == minTopBarHeightPx) Offset.Zero else Offset(0f, consumed)
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val canExpand = lazyListState.firstVisibleItemIndex == 0 &&
                lazyListState.firstVisibleItemScrollOffset == 0
            val expand = canExpand && topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2f
            topBarHeight.animateTo(
                targetValue = if (expand) maxTopBarHeightPx else minTopBarHeightPx,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    LaunchedEffect(uiState.appliedCount) {
        if (uiState.appliedCount > 0) {
            showMessage(context.getString(R.string.betterlyrics_applied))
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val currentTopBarHeight = with(density) { topBarHeight.value.toDp() }

        if (mediaState == null) {
            M3EEmptyState(
                icon = Icons.Rounded.MusicOff,
                title = stringResource(R.string.betterlyrics_no_song),
                description = stringResource(R.string.external_lyrics_no_media_body),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = currentTopBarHeight)
                    .padding(24.dp),
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .m3eContentWidth()
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = M3ESpacing.large,
                    top = currentTopBarHeight + M3ESpacing.small,
                    end = M3ESpacing.large,
                    bottom = MiniPlayerHeight +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                        M3ESpacing.extraExtraLarge
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(key = "track") {
                    CurrentTrackCard(media = mediaState)
                }

                item(key = "query_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = M3ESpacing.medium, top = M3ESpacing.small, bottom = M3ESpacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.lyrics_manual_search_form_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { viewModel.resetQueryToMedia(mediaState) },
                            enabled = !uiState.searching,
                        ) {
                            Text(stringResource(R.string.lyrics_manual_search_reset_fields))
                        }
                    }
                }

                item(key = "query_title") {
                    M3ETextField(
                        value = uiState.queryTitle,
                        onValueChange = viewModel::setQueryTitle,
                        label = { Text(stringResource(R.string.song_field_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = "query_artist") {
                    M3ETextField(
                        value = uiState.queryArtist,
                        onValueChange = viewModel::setQueryArtist,
                        label = { Text(stringResource(R.string.song_field_artist_optional)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = "query_album") {
                    M3ETextField(
                        value = uiState.queryAlbum,
                        onValueChange = viewModel::setQueryAlbum,
                        label = { Text(stringResource(R.string.song_field_album)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item(key = "query_search") {
                    SearchActionButton(
                        searching = uiState.searching,
                        canSearch = uiState.canSearch,
                        onSearch = { viewModel.search(mediaState) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                    )
                }

                uiState.manualSelection?.let { selection ->
                    item(key = "manual_selection") {
                        ManualSelectionBanner(
                            providerLabel = selection.providerLabel
                                .ifBlank { selection.document.provider.displayName },
                            onClear = {
                                viewModel.clearManualSelection(mediaState)
                                showMessage(
                                    context.getString(R.string.lyrics_manual_search_cleared)
                                )
                            },
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }

                if (uiState.searching) {
                    item(key = "searching") {
                        LinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                        )
                    }
                }

                uiState.error?.let { message ->
                    item(key = "error") {
                        ErrorCard(
                            message = message,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }

                if (!uiState.hasSearched && uiState.results.isEmpty() && !uiState.searching) {
                    item(key = "intro") {
                        Text(
                            text = stringResource(R.string.lyrics_manual_search_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = M3ESpacing.medium,
                                top = M3ESpacing.medium,
                                bottom = M3ESpacing.extraSmall,
                            ),
                        )
                    }
                }

                if (uiState.hasSearched &&
                    !uiState.searching &&
                    uiState.results.isEmpty() &&
                    uiState.error == null
                ) {
                    item(key = "empty") {
                        M3EEmptyState(
                            icon = Icons.Rounded.SearchOff,
                            title = stringResource(R.string.betterlyrics_no_results),
                            description = stringResource(R.string.lyrics_manual_search_empty_hint),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = M3ESpacing.extraLarge),
                        )
                    }
                }

                if (uiState.results.isNotEmpty()) {
                    item(key = "results_spacer") {
                        Spacer(Modifier.height(10.dp))
                    }
                }

                itemsIndexed(
                    items = uiState.results,
                    key = { index, candidate -> "${candidate.provider.name}_$index" },
                ) { index, candidate ->
                    CandidateCard(
                        candidate = candidate,
                        shape = M3EShape.groupedCards(index, uiState.results.size),
                        onClick = { viewModel.openPreview(candidate, mediaState) },
                    )
                }
            }
        }

        CollapsibleCommonTopBar(
            title = title,
            subtitle = subtitle,
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeight,
            onBackClick = { navController.popBackStack() },
        )
    }

    val previewCandidate = uiState.previewCandidate
    if (previewCandidate != null || uiState.previewLoading) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        M3EModalBottomSheet(
            onDismissRequest = viewModel::closePreview,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            PreviewSheetContent(
                state = uiState,
                media = mediaState,
                onSelectTrack = viewModel::selectPreviewTrack,
                onApply = { viewModel.apply(mediaState) },
                onSeek = { positionMs -> ExternalMediaMonitor.seekTo(positionMs) },
            )
        }
    }
}

@Composable
private fun CurrentTrackCard(media: ExternalMediaState) {
    val trackLine = listOf(media.artist, media.album)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val appLine = if (media.appLabel.isNotBlank()) {
        stringResource(R.string.external_lyrics_from_app, media.appLabel)
    } else {
        ""
    }
    val supporting = listOf(trackLine, appLine)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    M3EListItem(
        headline = media.title,
        overline = stringResource(R.string.betterlyrics_now_playing),
        supportingText = supporting.ifBlank { null },
        leadingContent = {
            SmartImage(
                model = media.artworkBitmap ?: media.artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
            )
        },
        shape = M3EShape.groupedCards(index = 0, count = 1),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        supportingTextMaxLines = 2,
    )
}

/**
 * The page's single call to action.
 *
 * It is an M3 Expressive pill: it squishes under the finger, its container fades into the tonal
 * "busy" role while a search runs, and icon and label swap with a short shared-axis transition
 * instead of the label jumping between two layouts.
 */
@Composable
private fun SearchActionButton(
    searching: Boolean,
    canSearch: Boolean,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current

    val containerColor by animateColorAsState(
        targetValue = when {
            searching -> scheme.secondaryContainer
            canSearch -> scheme.primary
            else -> scheme.surfaceContainerHighest
        },
        animationSpec = tween(M3EMotion.Duration.medium2),
        label = "searchButtonContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            searching -> scheme.onSecondaryContainer
            canSearch -> scheme.onPrimary
            else -> scheme.onSurfaceVariant
        },
        animationSpec = tween(M3EMotion.Duration.medium2),
        label = "searchButtonContent",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.97f else 1f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        label = "searchButtonScale",
    )

    M3ESurface(
        onClick = onSearch,
        enabled = canSearch && !searching,
        shape = M3EShape.full,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interaction,
        modifier = modifier
            .height(M3ESize.buttonExpressive)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = M3ESpacing.extraLarge),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = searching,
                transitionSpec = {
                    (fadeIn(tween(M3EMotion.Duration.short4)) + scaleIn(initialScale = 0.6f)) togetherWith
                        (fadeOut(tween(M3EMotion.Duration.short2)) + scaleOut(targetScale = 0.6f))
                },
                label = "searchButtonIcon",
            ) { isSearching ->
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = contentColor,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(M3ESpacing.small))
            AnimatedContent(
                targetState = searching,
                transitionSpec = {
                    fadeIn(tween(M3EMotion.Duration.medium1)) togetherWith
                        fadeOut(tween(M3EMotion.Duration.short3))
                },
                label = "searchButtonLabel",
            ) { isSearching ->
                Text(
                    text = if (isSearching) {
                        stringResource(R.string.betterlyrics_searching)
                    } else {
                        stringResource(R.string.lyrics_manual_search_action)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ManualSelectionBanner(
    providerLabel: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    M3ESurface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = M3EShape.groupedCards(index = 0, count = 1),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = M3ESize.minimumTarget)
                .padding(
                    start = M3ESpacing.large,
                    top = M3ESpacing.small,
                    bottom = M3ESpacing.small,
                    end = M3ESpacing.small,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(M3ESpacing.medium),
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(M3ESize.iconMedium),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.lyrics_manual_search_current_selection),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.external_lyrics_provider, providerLabel),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.lyrics_manual_search_clear))
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, modifier: Modifier = Modifier) {
    M3ESurface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = M3EShape.groupedCards(index = 0, count = 1),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = M3ESize.minimumTarget)
                .padding(M3ESpacing.large),
            horizontalArrangement = Arrangement.spacedBy(M3ESpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(M3ESize.iconMedium),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CandidateCard(
    candidate: BetterLyricsCandidate,
    shape: Shape,
    onClick: () -> Unit,
) {
    M3ESurface(
        onClick = onClick,
        level = M3ESurfaceLevel.Container,
        shape = shape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = M3ESpacing.large,
                vertical = M3ESpacing.medium,
            ),
            verticalArrangement = Arrangement.spacedBy(M3ESpacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(M3ESpacing.extraSmall),
            ) {
                Badge(text = candidate.providerLabel, emphasised = true)
                Badge(text = stringResource(R.string.betterlyrics_match_score, candidate.score))
                if (candidate.wordTimed) {
                    Badge(text = stringResource(R.string.betterlyrics_word_timed))
                }
                if (!candidate.result.translationRaw.isNullOrBlank()) {
                    Badge(text = stringResource(R.string.lyrics_manual_search_translation_badge))
                }
                if (!candidate.result.transliterationRaw.isNullOrBlank()) {
                    Badge(text = stringResource(R.string.lyrics_manual_search_romanization_badge))
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(M3ESize.iconMedium),
                )
            }
            Text(
                text = candidate.result.title.orEmpty()
                    .ifBlank { stringResource(R.string.song_field_title) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val metadata = listOfNotNull(
                candidate.result.artist,
                candidate.result.album,
                candidate.result.durationMs
                    ?.takeIf { it > 0L }
                    ?.let(::formatDuration),
            ).filter { it.isNotBlank() }
            if (metadata.isNotEmpty()) {
                Text(
                    text = metadata.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (candidate.previewLines.isNotEmpty()) {
                Text(
                    text = candidate.previewLines.joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String, emphasised: Boolean = false) {
    Surface(
        color = if (emphasised) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (emphasised) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = M3EShape.full,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PreviewSheetContent(
    state: BetterLyricsSearchViewModel.UiState,
    media: ExternalMediaState?,
    onSelectTrack: (PreviewTrack) -> Unit,
    onApply: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val candidate = state.previewCandidate
    val document = state.previewDocument

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.lyrics_manual_search_preview),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        if (candidate != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Badge(text = candidate.providerLabel, emphasised = true)
                Badge(text = stringResource(R.string.betterlyrics_match_score, candidate.score))
                if (candidate.wordTimed) {
                    Badge(text = stringResource(R.string.betterlyrics_word_timed))
                }
            }
            Text(
                text = candidate.result.title.orEmpty().ifBlank { media?.title.orEmpty() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val metadata = listOfNotNull(
                candidate.result.artist,
                candidate.result.album,
                candidate.result.durationMs?.takeIf { it > 0L }?.let(::formatDuration),
            ).filter { it.isNotBlank() }
            if (metadata.isNotEmpty()) {
                Text(
                    text = metadata.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }

        if (state.previewLoading || document == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            TrackChooser(
                document = document,
                selected = state.previewTrack,
                onSelect = onSelectTrack,
            )
            PreviewLines(
                document = document,
                track = state.previewTrack,
                onSeek = onSeek,
            )
        }

        Button(
            onClick = onApply,
            enabled = document != null && !state.applying,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = M3ESize.buttonExpressive),
            shape = M3EShape.full,
        ) {
            if (state.applying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.betterlyrics_apply))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TrackChooser(
    document: LyricsDocument,
    selected: PreviewTrack,
    onSelect: (PreviewTrack) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == PreviewTrack.ORIGINAL,
            onClick = { onSelect(PreviewTrack.ORIGINAL) },
            label = { Text(stringResource(R.string.lyrics_manual_search_track_original)) },
        )
        if (document.hasTranslation) {
            FilterChip(
                selected = selected == PreviewTrack.TRANSLATION,
                onClick = { onSelect(PreviewTrack.TRANSLATION) },
                label = { Text(stringResource(R.string.lyrics_manual_search_track_translation)) },
            )
        }
        if (document.hasRomanization) {
            FilterChip(
                selected = selected == PreviewTrack.ROMANIZATION,
                onClick = { onSelect(PreviewTrack.ROMANIZATION) },
                label = { Text(stringResource(R.string.lyrics_manual_search_track_romanization)) },
            )
        }
    }
}

private data class PreviewLine(
    val timeMs: Long,
    val text: String,
)

@Composable
private fun PreviewLines(
    document: LyricsDocument,
    track: PreviewTrack,
    onSeek: (Long) -> Unit,
) {
    val lines = remember(document, track) {
        document.lines.mapNotNull { line ->
            val text = when (track) {
                PreviewTrack.ORIGINAL -> line.text
                PreviewTrack.TRANSLATION -> line.translation
                PreviewTrack.ROMANIZATION -> line.romanization
            }
            text?.takeIf { it.isNotBlank() }?.let { PreviewLine(line.startMs, it) }
        }
    }

    if (lines.isEmpty()) {
        Text(
            text = stringResource(R.string.lyrics_manual_search_empty_track),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 340.dp),
    ) {
        items(lines) { line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(M3EShape.medium)
                    .clickable { onSeek(line.timeMs) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDuration(line.timeMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp),
                )
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.external_lyrics_play),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
