package io.github.jayfunc.carpelines.presentation.components.lyrics.card

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.model.Lyrics
import io.github.jayfunc.carpelines.presentation.components.LyricsClipboardText
import io.github.jayfunc.carpelines.presentation.components.rememberAppMessage
import io.github.jayfunc.carpelines.ui.m3e.M3EAction
import io.github.jayfunc.carpelines.ui.m3e.M3EActionEmphasis
import io.github.jayfunc.carpelines.ui.m3e.M3EConnectedActions
import io.github.jayfunc.carpelines.ui.m3e.M3EConnectedOrientation
import io.github.jayfunc.carpelines.ui.m3e.M3EFilterChip
import io.github.jayfunc.carpelines.ui.m3e.M3EListItem
import io.github.jayfunc.carpelines.ui.m3e.M3ESurface
import io.github.jayfunc.carpelines.ui.m3e.M3ESurfaceLevel
import io.github.jayfunc.carpelines.ui.theme.M3EMotion
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESize
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import io.github.jayfunc.carpelines.ui.theme.emphasized
import kotlinx.coroutines.launch

private data class ShareLine(val card: LyricsCardLine, val sourceIndex: Int)

/**
 * The lyric share page reachable from the long-press sheet.
 *
 * It follows BetterLyrics' share window: pick lyric rows on one side, preview the card, choose one
 * of the ported templates and export the rendered composition. On a phone the lyric picker and the
 * style gallery are stacked under the preview instead of sitting in side columns.
 *
 * Structurally this is an M3 "sheet with a pinned action bar": the preview takes the flexible space
 * because it is the only element that must grow, everything that configures it sits in fixed
 * bands, and the export actions stay anchored to the bottom edge where the thumb expects them.
 * Selection inside the style gallery is carried by a ring **and** a check badge, so it survives
 * both grayscale rendering and the wildly different swatches the templates use.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LyricsShareSheet(
    title: String,
    artist: String,
    artwork: android.graphics.Bitmap?,
    accent: Color,
    lyrics: Lyrics?,
    initialIndex: Int,
    showBackground: Boolean,
    showTranslation: Boolean,
    showRomanization: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showMessage = rememberAppMessage()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sourceLines = remember(lyrics, showBackground, showTranslation, showRomanization) {
        buildShareLines(lyrics, showBackground, showTranslation, showRomanization)
    }

    var style by remember { mutableStateOf(LyricsCardStyleStore.style(context)) }
    var config by remember { mutableStateOf(LyricsCardConfig(LyricsCardStyleStore.font(context, style))) }
    var group by remember { mutableStateOf(style.group) }
    var fontMenuOpen by remember { mutableStateOf(false) }
    var showSelection by remember { mutableStateOf(false) }

    val initialSelection = remember(sourceLines, initialIndex) {
        if (sourceLines.isEmpty()) {
            emptySet()
        } else {
            val anchor = sourceLines
                .indexOfFirst { it.sourceIndex >= initialIndex }
                .takeIf { it >= 0 }
                ?: 0
            val start = (anchor - 2).coerceAtLeast(0)
            val end = (anchor + 2).coerceAtMost(sourceLines.lastIndex)
            (start..end).toSet()
        }
    }
    var selected by remember(sourceLines) { mutableStateOf(initialSelection) }

    fun chooseStyle(next: LyricsCardStyle) {
        if (next == style) return
        style = next
        group = next.group
        config = LyricsCardConfig(LyricsCardStyleStore.font(context, next))
        LyricsCardStyleStore.saveStyle(context, next)
    }

    fun chooseFont(font: LyricsCardFont) {
        config = LyricsCardConfig(font)
        LyricsCardStyleStore.saveFont(context, style, font)
        fontMenuOpen = false
    }

    val selectedLines = remember(sourceLines, selected) {
        sourceLines.filterIndexed { index, _ -> index in selected }.map { it.card }
    }
    val cardData = remember(title, artist, artwork, accent, selectedLines) {
        LyricsCardData(
            title = title,
            artist = artist,
            cover = artwork,
            accent = accent,
            lines = selectedLines,
        )
    }
    val cardLayer = rememberGraphicsLayer()
    val hasSelection = selectedLines.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = M3EShape.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.97f)
                .navigationBarsPadding(),
        ) {
            ShareHeader(
                title = title,
                artist = artist,
                fontMenuOpen = fontMenuOpen,
                onOpenFontMenu = { fontMenuOpen = true },
                onDismissFontMenu = { fontMenuOpen = false },
                onChooseFont = ::chooseFont,
                selectedFont = config.font,
                onClose = onDismiss,
            )

            CardPreview(
                style = style,
                data = cardData,
                config = config,
                hasSelection = hasSelection,
                cardLayer = cardLayer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = M3ESpacing.large),
            )

            // The lyric picker is a row that opens a sub-sheet, not a button that competes with
            // the export actions: it is a destination, and destinations are rows in M3.
            M3EListItem(
                headline = stringResource(R.string.lyrics_card_select_lyrics),
                supportingText = stringResource(
                    R.string.lyrics_card_selected_count,
                    selectedLines.size,
                    sourceLines.size,
                ),
                // Same leading-icon treatment as the settings rows: a bare secondary-tinted icon,
                // no container, so it reads as part of the row rather than a badge.
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Checklist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(M3ESize.iconMedium),
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(M3ESize.iconMedium),
                    )
                },
                shape = M3EShape.large,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                enabled = sourceLines.isNotEmpty(),
                onClick = { showSelection = true },
                modifier = Modifier.padding(
                    start = M3ESpacing.large,
                    end = M3ESpacing.large,
                    top = M3ESpacing.medium,
                ),
            )

            Text(
                text = stringResource(R.string.lyrics_card_style),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = M3ESpacing.large,
                    end = M3ESpacing.large,
                    top = M3ESpacing.medium,
                    bottom = M3ESpacing.small,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = M3ESpacing.large),
                horizontalArrangement = Arrangement.spacedBy(M3ESpacing.small),
            ) {
                LyricsCardGroup.entries.forEach { candidate ->
                    M3EFilterChip(
                        label = stringResource(candidate.labelRes),
                        selected = candidate == group,
                        onClick = { group = candidate },
                        leadingIcon = if (candidate == group) Icons.Rounded.Check else null,
                    )
                }
            }
            key(group) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StyleTilePreviewHeight + StyleTileLabelReserve + StyleTileVerticalPadding * 2),
                    contentPadding = PaddingValues(
                        start = M3ESpacing.large,
                        end = M3ESpacing.large,
                        top = StyleTileVerticalPadding,
                        bottom = StyleTileVerticalPadding,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(M3ESpacing.small),
                ) {
                    items(LyricsCardStyle.entries.filter { it.group == group }, key = { it.key }) { candidate ->
                        StyleTile(
                            style = candidate,
                            data = cardData,
                            selected = candidate == style,
                            onClick = { chooseStyle(candidate) },
                        )
                    }
                }
            }

            // One connected action bar instead of two floating pills: the two exports act on the
            // same card, so they share a silhouette and are told apart by emphasis, not borders.
            M3EConnectedActions(
                actions = listOf(
                    M3EAction(
                        label = stringResource(R.string.lyrics_card_save),
                        onClick = {
                            scope.launch {
                                val ok = LyricsCardExporter.save(context, cardLayer)
                                showMessage(
                                    context.getString(
                                        if (ok) {
                                            R.string.lyrics_card_saved
                                        } else {
                                            R.string.lyrics_card_save_failed
                                        }
                                    )
                                )
                            }
                        },
                        icon = Icons.Rounded.Download,
                        emphasis = M3EActionEmphasis.Tonal,
                        enabled = hasSelection,
                    ),
                    M3EAction(
                        label = stringResource(R.string.lyrics_card_share),
                        onClick = {
                            scope.launch {
                                val ok = LyricsCardExporter.share(
                                    context = context,
                                    layer = cardLayer,
                                    chooserTitle = context.getString(
                                        R.string.lyrics_card_share_chooser
                                    ),
                                )
                                if (!ok) {
                                    showMessage(context.getString(R.string.lyrics_card_share_failed))
                                }
                            }
                        },
                        icon = Icons.Rounded.Share,
                        emphasis = M3EActionEmphasis.Filled,
                        enabled = hasSelection,
                    ),
                ),
                orientation = M3EConnectedOrientation.Horizontal,
                // The bottom inset is the sheet's own padding, so it matches the 16dp the other
                // sheets close with instead of hugging the navigation bar.
                modifier = Modifier.padding(
                    start = M3ESpacing.large,
                    end = M3ESpacing.large,
                    top = M3ESpacing.small,
                    bottom = M3ESpacing.large,
                ),
            )
        }

        if (showSelection) {
            LyricsSelectionSheet(
                lines = sourceLines,
                selected = selected,
                onToggle = { index ->
                    selected = if (index in selected) selected - index else selected + index
                },
                onToggleAll = {
                    selected = if (selected.size == sourceLines.size) {
                        emptySet()
                    } else {
                        sourceLines.indices.toSet()
                    }
                },
                onDismiss = { showSelection = false },
            )
        }
    }
}

/**
 * Sheet header.
 *
 * The close affordance leads because the sheet is nearly full height — the same place a full-screen
 * dialog puts it — and the font picker is a sample pill on the trailing edge: the glyph is rendered
 * in the *active* family, so the control states its own value without borrowing a text label that
 * a CJK face cannot preview anyway.
 */
@Composable
private fun ShareHeader(
    title: String,
    artist: String,
    selectedFont: LyricsCardFont,
    fontMenuOpen: Boolean,
    onOpenFontMenu: () -> Unit,
    onDismissFontMenu: () -> Unit,
    onChooseFont: (LyricsCardFont) -> Unit,
    onClose: () -> Unit,
) {
    val fontDescription = "${stringResource(R.string.lyrics_card_font)}: " +
        stringResource(selectedFont.labelRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = M3ESpacing.extraSmall,
                end = M3ESpacing.large,
                top = M3ESpacing.extraSmall,
                bottom = M3ESpacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.lyrics_card_close),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = M3ESpacing.extraSmall),
        ) {
            Text(
                text = stringResource(R.string.lyrics_card_title),
                style = MaterialTheme.typography.emphasized.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            M3ESurface(
                level = M3ESurfaceLevel.ContainerHigh,
                shape = M3EShape.full,
                onClick = onOpenFontMenu,
                role = Role.DropdownList,
                modifier = Modifier.semantics { contentDescription = fontDescription },
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = M3ESize.minimumTarget)
                        .padding(horizontal = M3ESpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(M3ESpacing.extraSmall),
                ) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = selectedFont.family,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        // Decorative: the pill's own description already names the family, so the
                        // sample should not be read out as two stray letters.
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(M3ESize.iconSmall),
                    )
                }
            }
            FontFamilyMenu(
                expanded = fontMenuOpen,
                selected = selectedFont,
                onDismissRequest = onDismissFontMenu,
                onChooseFont = onChooseFont,
            )
        }
    }
}

/**
 * Font menu.
 *
 * Built from the expressive menu group rather than the default popup: one rounded container whose
 * items take the connected item shapes, and every row is set in the face it applies so the choice
 * can be read before it is made.
 */
@Composable
private fun FontFamilyMenu(
    expanded: Boolean,
    selected: LyricsCardFont,
    onDismissRequest: () -> Unit,
    onChooseFont: (LyricsCardFont) -> Unit,
) {
    val count = LyricsCardFont.entries.size
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 0, count = 1),
            containerColor = MenuDefaults.groupStandardContainerColor,
        ) {
            LyricsCardFont.entries.forEachIndexed { index, font ->
                DropdownMenuItem(
                    onClick = { onChooseFont(font) },
                    text = {
                        Text(
                            text = stringResource(font.labelRes),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = font.family,
                            ),
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "Aa",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = font.family,
                            ),
                        )
                    },
                    trailingContent = if (font == selected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(M3ESize.iconSmall),
                            )
                        }
                    } else {
                        null
                    },
                    shape = MenuDefaults.itemShape(index = index, count = count).shape,
                    colors = MenuDefaults.itemColors(),
                )
            }
        }
    }
}

/**
 * The preview well.
 *
 * `surfaceContainerHighest` is the M3 container for content that must stay legible against
 * unpredictable media — here, 25 templates that range from pure white to pure black — and the
 * extra-large corners mark it as a distinct surface rather than part of the sheet.
 */
@Composable
private fun CardPreview(
    style: LyricsCardStyle,
    data: LyricsCardData,
    config: LyricsCardConfig,
    hasSelection: Boolean,
    cardLayer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    M3ESurface(
        modifier = modifier,
        level = M3ESurfaceLevel.ContainerHighest,
        shape = M3EShape.extraLarge,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(M3ESpacing.medium),
            contentAlignment = Alignment.Center,
        ) {
            ScaledLyricsCard(
                style = style,
                data = data,
                config = config,
                fit = true,
                cardModifier = Modifier.drawWithContent {
                    cardLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(cardLayer)
                },
            )
            if (!hasSelection) {
                M3ESurface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = M3EShape.full,
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_card_selection_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(
                            horizontal = M3ESpacing.large,
                            vertical = M3ESpacing.medium,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * One template thumbnail.
 *
 * The ring is the selection state and the badge is its redundant, colour-independent twin; the
 * ring colour animates on the effects spec so switching templates feels like a state change
 * instead of a repaint.
 */
@Composable
private fun StyleTile(
    style: LyricsCardStyle,
    data: LyricsCardData,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val ringColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.outlineVariant,
        animationSpec = M3EMotion.effects(),
        label = "styleTileRing",
    )
    Column(
        modifier = Modifier.width(StyleTileWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            M3ESurface(
                modifier = Modifier.size(width = StyleTileWidth, height = StyleTilePreviewHeight),
                color = style.swatch,
                shape = M3EShape.medium,
                border = BorderStroke(
                    width = if (selected) SelectedRingWidth else IdleRingWidth,
                    color = ringColor,
                ),
                onClick = onClick,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = M3ESpacing.small, vertical = M3ESpacing.small),
                    contentAlignment = Alignment.Center,
                ) {
                    ScaledLyricsCard(
                        style = style,
                        data = data,
                        config = LyricsCardConfig(style.defaultFont),
                        fit = true,
                    )
                }
            }
            if (selected) {
                M3ESurface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(M3ESpacing.extraSmall),
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                    shape = M3EShape.full,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(2.dp)
                            .size(12.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(M3ESpacing.extraSmall))
        Text(
            text = stringResource(style.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = M3ESpacing.extraSmall),
        )
    }
}

/**
 * Lyric row picker.
 *
 * Rows are a connected list so the eye can run down the column. Selection is carried the M3 way —
 * the container turns into `secondaryContainer` and a check mark appears on the trailing edge —
 * instead of by a checkbox on every row, which is a control the sheet does not need: the whole row
 * is the switch, exposed as `Role.Checkbox` so the state is still announced.
 *
 * The header keeps one text action (select all / clear) and the running count lives under the
 * title, so the footer holds a single confirm that hugs its label instead of a third full-width
 * pill competing with the export bar underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsSelectionSheet(
    lines: List<ShareLine>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    onToggleAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allSelected = lines.isNotEmpty() && selected.size == lines.size
    BackHandler { onDismiss() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = M3EShape.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = M3ESpacing.extraSmall,
                        end = M3ESpacing.large,
                        top = M3ESpacing.extraSmall,
                        bottom = M3ESpacing.small,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(M3ESpacing.extraSmall),
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.lyrics_card_close),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = M3ESpacing.extraSmall),
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_card_select_lyrics),
                        style = MaterialTheme.typography.emphasized.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(
                            R.string.lyrics_card_selected_count,
                            selected.size,
                            lines.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onToggleAll, enabled = lines.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            if (allSelected) {
                                R.string.lyrics_card_clear_selection
                            } else {
                                R.string.lyrics_card_select_all
                            }
                        ),
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = M3ESpacing.large,
                    end = M3ESpacing.large,
                    top = M3ESpacing.extraSmall,
                    bottom = M3ESpacing.large,
                ),
                verticalArrangement = Arrangement.spacedBy(GroupedRowGap),
            ) {
                itemsIndexed(lines) { index, line ->
                    val checked = index in selected
                    // Reserved, not conditional: a mark that appears from nowhere would reflow the
                    // row's text on every tap.
                    val checkProgress by animateFloatAsState(
                        targetValue = if (checked) 1f else 0f,
                        animationSpec = M3EMotion.effects(),
                        label = "lyricRowCheck",
                    )
                    M3EListItem(
                        headline = line.card.text.ifBlank { "· · ·" },
                        supportingText = line.card.secondary?.takeIf { it.isNotBlank() },
                        trailingContent = {
                            Box(
                                modifier = Modifier.size(M3ESize.iconMedium),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(M3ESize.iconMedium)
                                        .graphicsLayer {
                                            alpha = checkProgress
                                            val scale = CheckMinScale +
                                                (1f - CheckMinScale) * checkProgress
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                )
                            }
                        },
                        selected = checked,
                        role = Role.Checkbox,
                        shape = M3EShape.groupedCards(index, lines.size),
                        containerColor = if (checked) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        onClick = { onToggle(index) },
                    )
                }
            }
            // A lone confirm hugs its label and sits on the trailing edge, so it reads as the end
            // of the list rather than a bar competing with the export buttons underneath.
            M3EConnectedActions(
                actions = listOf(
                    M3EAction(
                        label = stringResource(R.string.lyrics_card_done),
                        onClick = onDismiss,
                        emphasis = M3EActionEmphasis.Filled,
                    ),
                ),
                // The bottom inset is the sheet's own padding, so the confirm clears the
                // navigation bar by the same 16dp the other sheets use.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = M3ESpacing.large,
                        end = M3ESpacing.large,
                        top = M3ESpacing.medium,
                        bottom = M3ESpacing.large,
                    ),
                height = M3ESize.minimumTarget,
                loneActionAlignment = Alignment.End,
            )
        }
    }
}

/** 2dp between rows, matching the gap a settings subsection puts between its items. */
private val GroupedRowGap = 2.dp

/** Scale a row's check mark starts from, so it grows into place instead of popping in. */
private const val CheckMinScale = 0.6f

/** Thumbnail width: three tiles plus their gaps fit a compact window. */
private val StyleTileWidth = 96.dp

/** Thumbnail height, keeping the tall portrait ratio most templates are authored at. */
private val StyleTilePreviewHeight = 104.dp

/** Space the caption needs under a thumbnail, so the lazy row needs no magic height. */
private val StyleTileLabelReserve = 28.dp

/** Vertical padding inside the gallery scroll, counted into the row height below. */
private val StyleTileVerticalPadding = M3ESpacing.extraSmall

private val IdleRingWidth = 1.dp
private val SelectedRingWidth = 2.dp

private fun buildShareLines(
    lyrics: Lyrics?,
    showBackground: Boolean,
    showTranslation: Boolean,
    showRomanization: Boolean,
): List<ShareLine> {
    val synced = lyrics?.synced.orEmpty()
    if (synced.isNotEmpty()) {
        return synced.mapIndexedNotNull { index, line ->
            val text = LyricsClipboardText.syncedLine(line, showBackground)
            val secondary = buildList {
                if (showTranslation) line.translation?.takeIf { it.isNotBlank() }?.let(::add)
                if (showRomanization) line.romanization?.takeIf { it.isNotBlank() }?.let(::add)
            }.joinToString("\n").ifBlank { null }
            if (text.isBlank() && secondary == null) {
                null
            } else {
                ShareLine(LyricsCardLine(text, secondary), index)
            }
        }
    }
    return lyrics?.plain.orEmpty().mapIndexedNotNull { index, entry ->
        LyricsClipboardText.plainLine(entry)
            .takeIf { it.isNotBlank() }
            ?.let { ShareLine(LyricsCardLine(it), index) }
    }
}
