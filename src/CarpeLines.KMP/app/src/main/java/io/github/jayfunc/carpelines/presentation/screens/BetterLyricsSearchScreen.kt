package io.github.jayfunc.carpelines.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import io.github.jayfunc.carpelines.ui.m3e.M3ETextField
import io.github.jayfunc.carpelines.utils.formatDuration
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Manual lyrics search and selection, the Android counterpart of BetterLyrics'
 * "Lyrics Search" window: edit the metadata, search every enabled provider, preview a result and
 * keep that exact version for the current track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetterLyricsSearchScreen(
    navController: NavController,
    viewModel: BetterLyricsSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val media by ExternalMediaMonitor.state.collectAsStateWithLifecycle()
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

    LaunchedEffect(media?.identity) {
        viewModel.syncWithMedia(media)
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

    Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        val currentTopBarHeight = with(density) { topBarHeight.value.toDp() }

        if (media == null) {
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = currentTopBarHeight + 8.dp,
                    end = 16.dp,
                    bottom = MiniPlayerHeight +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "track") {
                    media?.let { CurrentTrackCard(media = it) }
                }

                item(key = "query") {
                    QueryCard(
                        title = uiState.queryTitle,
                        artist = uiState.queryArtist,
                        album = uiState.queryAlbum,
                        searching = uiState.searching,
                        canSearch = uiState.canSearch,
                        onTitleChange = viewModel::setQueryTitle,
                        onArtistChange = viewModel::setQueryArtist,
                        onAlbumChange = viewModel::setQueryAlbum,
                        onResetToMedia = { viewModel.resetQueryToMedia(media) },
                        onSearch = { viewModel.search(media) },
                    )
                }

                uiState.manualSelection?.let { selection ->
                    item(key = "manual_selection") {
                        ManualSelectionBanner(
                            providerLabel = selection.providerLabel
                                .ifBlank { selection.document.provider.displayName },
                            onClear = {
                                viewModel.clearManualSelection(media)
                                showMessage(
                                    context.getString(R.string.lyrics_manual_search_cleared)
                                )
                            },
                        )
                    }
                }

                if (uiState.searching) {
                    item(key = "searching") {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                uiState.error?.let { message ->
                    item(key = "error") {
                        ErrorCard(message = message)
                    }
                }

                if (!uiState.hasSearched && uiState.results.isEmpty() && !uiState.searching) {
                    item(key = "intro") {
                        Text(
                            text = stringResource(R.string.lyrics_manual_search_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
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
                                .padding(vertical = 24.dp),
                        )
                    }
                }

                items(items = uiState.results) { candidate ->
                    CandidateCard(
                        candidate = candidate,
                        onClick = { viewModel.openPreview(candidate, media) },
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
        ModalBottomSheet(
            onDismissRequest = viewModel::closePreview,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            PreviewSheetContent(
                state = uiState,
                media = media,
                onSelectTrack = viewModel::selectPreviewTrack,
                onApply = { viewModel.apply(media) },
                onSeek = { positionMs -> ExternalMediaMonitor.seekTo(positionMs) },
            )
        }
    }
}

@Composable
private fun CurrentTrackCard(media: ExternalMediaState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmartImage(
                model = media.artworkBitmap ?: media.artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.betterlyrics_now_playing),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(media.artist, media.album)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.external_lyrics_from_app, media.appLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QueryCard(
    title: String,
    artist: String,
    album: String,
    searching: Boolean,
    canSearch: Boolean,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onResetToMedia: () -> Unit,
    onSearch: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.lyrics_manual_search_form_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onResetToMedia, enabled = !searching) {
                    Text(stringResource(R.string.lyrics_manual_search_reset_fields))
                }
            }

            M3ETextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.song_field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            M3ETextField(
                value = artist,
                onValueChange = onArtistChange,
                label = { Text(stringResource(R.string.song_field_artist_optional)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            M3ETextField(
                value = album,
                onValueChange = onAlbumChange,
                label = { Text(stringResource(R.string.song_field_album)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSearch,
                enabled = canSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (searching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (searching) {
                        stringResource(R.string.betterlyrics_searching)
                    } else {
                        stringResource(R.string.lyrics_manual_search_action)
                    },
                )
            }
        }
    }
}

@Composable
private fun ManualSelectionBanner(providerLabel: String, onClear: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.lyrics_manual_search_current_selection),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.external_lyrics_provider, providerLabel),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.lyrics_manual_search_clear))
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
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
private fun CandidateCard(candidate: BetterLyricsCandidate, onClick: () -> Unit) {
    val previewLines = remember(candidate.result.raw) {
        previewLinesOf(candidate.result.raw)
    }
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            }
            Text(
                text = candidate.result.title.orEmpty().ifBlank { stringResource(R.string.song_field_title) },
                style = MaterialTheme.typography.titleMedium,
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (previewLines.isNotEmpty()) {
                Text(
                    text = previewLines.joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
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
        shape = CircleShape,
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
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
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
                    .clip(RoundedCornerShape(12.dp))
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

private fun previewLinesOf(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    val withoutTags = Regex("""<[^>]*>""")
    val lineTimestamp = Regex("""^\[[^\]]*]""")
    val voicePrefix = Regex("""^v\d+:\s*""", RegexOption.IGNORE_CASE)
    return raw.lineSequence()
        .take(60)
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("<") }
        .map { line ->
            voicePrefix.replace(lineTimestamp.replace(line, ""), "").trim()
        }
        .filter { it.isNotEmpty() }
        .take(3)
        .toList()
}
