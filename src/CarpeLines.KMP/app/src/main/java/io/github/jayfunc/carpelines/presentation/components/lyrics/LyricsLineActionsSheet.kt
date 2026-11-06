package io.github.jayfunc.carpelines.presentation.components.lyrics

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.model.LyricsSourcePreference
import io.github.jayfunc.carpelines.presentation.components.rememberSyncedSliderState
import io.github.jayfunc.carpelines.ui.m3e.M3ELeadingIconContainer
import io.github.jayfunc.carpelines.ui.m3e.M3EListItem
import io.github.jayfunc.carpelines.ui.m3e.M3ESurface
import io.github.jayfunc.carpelines.ui.m3e.M3ESurfaceLevel
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import io.github.jayfunc.carpelines.ui.theme.emphasized
import kotlin.math.roundToInt

/**
 * Long-press actions for one lyric line.
 *
 * The sheet answers one question — "what can I do with *this* line" — so the layout is ordered by
 * proximity to that line: header → the actions that act on the line itself → the sync calibration
 * that fixes this line's timing → the lyrics-level operations that are not about the line at all.
 *
 * Rows deliberately reuse the settings page's language: `surfaceContainer` grouped rows separated
 * by a 2dp gap with the full corner radius only on the group's outer rows, a `primary`-tinted
 * subsection label above the group, and a plain `secondary` leading icon. An action therefore
 * looks the same whether it is reached from this sheet or from settings, and the sheet does not
 * invent a second row style for the same content.
 *
 * The lyric source is a mutually exclusive choice, so it keeps radio semantics: the selected row
 * takes the settings picker's treatment — `primaryContainer` fill plus a check mark — instead of
 * communication by container tint alone.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsLineActionsSheet(
    lineText: String,
    offsetMs: Int,
    onDismiss: () -> Unit,
    onCopyLine: () -> Unit,
    onSharePage: () -> Unit,
    onOffsetChange: (Int) -> Unit,
    onResetOffset: () -> Unit,
    onRefreshLyrics: () -> Unit,
    onManualSearch: () -> Unit,
    onOpenLyricsSettings: () -> Unit,
    sourcePreference: LyricsSourcePreference,
    onSourcePreferenceChange: (LyricsSourcePreference) -> Unit,
    providerLabel: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Same pair the settings page uses: rows are `surfaceContainer` cards on `surface`, so a
        // row here and a row there read as the same component rather than as two tonal systems.
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = M3EShape.extraLarge,
    ) {
        // The scrollable body and the sheet's own drag both answer a downward swipe. On Android
        // 12+ the stretch overscroll then competes with the sheet, making the card bounce up and
        // down under the finger. The sheet already provides the dismissal gesture, so the inner
        // scroll keeps no overscroll of its own.
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = M3ESpacing.small)
            ) {
                SheetHeader(
                    lineText = lineText.ifBlank { stringResource(R.string.lyrics_line_actions_instrumental) },
                    providerLabel = providerLabel,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = M3ESpacing.large,
                            end = M3ESpacing.large,
                            top = M3ESpacing.medium,
                        )
                ) {
                    // No section label on the first group: the header already names the line, and
                    // these three rows are all about that line — copying it, sharing it, or fixing
                    // its timing.
                    RowGroup {
                        ActionRow(
                            icon = Icons.Rounded.ContentCopy,
                            title = stringResource(R.string.lyrics_line_actions_copy),
                            onClick = onCopyLine,
                        )
                        ActionRow(
                            icon = Icons.Rounded.Checklist,
                            title = stringResource(R.string.lyrics_line_actions_share_page),
                            onClick = onSharePage,
                        )
                        OffsetSlider(
                            offsetMs = offsetMs,
                            onOffsetChange = onOffsetChange,
                            onResetOffset = onResetOffset,
                        )
                    }
                    Spacer(Modifier.height(SectionGap))

                    SheetSection(title = stringResource(R.string.setcat_lyrics_source_priority_label)) {
                        LyricsSourcePreference.entries.forEach { preference ->
                            ChoiceRow(
                                title = stringResource(preference.labelRes()),
                                selected = preference == sourcePreference,
                                onClick = { onSourcePreferenceChange(preference) },
                            )
                        }
                    }

                    SheetSection(title = stringResource(R.string.lyrics_line_actions_group_lyrics)) {
                        ActionRow(
                            icon = Icons.Rounded.Refresh,
                            title = stringResource(R.string.lyrics_line_actions_refresh),
                            onClick = onRefreshLyrics,
                        )
                        ActionRow(
                            icon = Icons.Rounded.Search,
                            title = stringResource(R.string.lyrics_line_actions_manual_search),
                            onClick = onManualSearch,
                        )
                        ActionRow(
                            icon = Icons.Rounded.Tune,
                            title = stringResource(R.string.lyrics_line_actions_settings),
                            onClick = onOpenLyricsSettings,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sheet header: what the sheet is about, then what it is acting on.
 *
 * The line itself owns the emphasised role — it is the subject of the sheet — while the sheet
 * title stays an overline. The offset is *not* repeated here; it belongs to the calibration
 * control below, and showing it twice made the header look like a status bar.
 */
@Composable
private fun SheetHeader(lineText: String, providerLabel: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = M3ESpacing.large,
                end = M3ESpacing.large,
                top = M3ESpacing.small,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        M3ELeadingIconContainer(
            size = 52.dp,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_lyrics_24),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(M3ESpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.lyrics_line_actions_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = lineText,
                style = MaterialTheme.typography.emphasized.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = M3ESpacing.extraSmall)
                    .semantics { heading() },
            )
            if (!providerLabel.isNullOrBlank()) {
                // Provenance is metadata, not a heading: a tonal pill separates it from the lyric
                // text without competing with it for the hierarchy.
                M3ESurface(
                    level = M3ESurfaceLevel.ContainerHigh,
                    shape = M3EShape.full,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = M3ESpacing.medium),
                ) {
                    Text(
                        text = stringResource(R.string.external_lyrics_provider, providerLabel),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            horizontal = M3ESpacing.medium,
                            vertical = M3ESpacing.extraSmall,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Subsection label plus its group.
 *
 * This is the settings page's section rhythm: a `primary`-tinted `labelMedium` label inset a
 * little past the rows, then the contiguous rows, then the gap that separates this group from the
 * next one. Reusing the rhythm, not just the colours, is what makes the sheet scan like settings.
 */
@Composable
private fun SheetSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = SectionTitleInset,
            top = M3ESpacing.small,
            bottom = M3ESpacing.extraSmall,
        ),
    )
    RowGroup(content = content)
    Spacer(Modifier.height(SectionGap))
}

/**
 * Rows sit 2dp apart, each one its own rounded card — the settings subsection rhythm. The group is
 * clipped at the subsection container step, so the first and last row end on the same corner the
 * settings pages give a subsection's ends instead of a single card's radius.
 */
@Composable
private fun RowGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(M3EShape.extraLarge),
        verticalArrangement = Arrangement.spacedBy(RowGroupGap),
        content = content,
    )
}

/**
 * Action row.
 *
 * Same anatomy as a settings row: a bare 24dp icon tinted `secondary`, a `bodyLarge` label, and a
 * `surfaceContainer` container. The row is the target.
 */
@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    M3EListItem(
        headline = title,
        onClick = onClick,
        shape = M3EShape.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        leadingContent = { RowIcon(icon) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Calibration card, shaped like a settings slider: leading icon, title, the current value as a
 * tonal pill, and the slider underneath.
 *
 * The slider keeps a local draft while the thumb is down and commits once the gesture ends, so the
 * preference store sees one write per adjustment instead of one per frame; the same pattern the
 * lyrics appearance sliders use. Its ticks are one [LyricsOffsetStore.STEP_MS] apart rather than
 * free-running: a tenth of a second is the finest shift the ear can judge, and visible stops are
 * what make a settings slider readable instead of an analog stripe.
 *
 * The range is deliberately narrower than the store's clamp: offsets beyond a few seconds mean the
 * lyrics belong to a different recording, and a ±15s track would make a 0.1s nudge cost one pixel.
 *
 * Reset is disabled at zero because the action would be a no-op — M3 keeps an unavailable action
 * visible but inert so the control does not change width as the value crosses zero.
 */
@Composable
private fun OffsetSlider(
    offsetMs: Int,
    onOffsetChange: (Int) -> Unit,
    onResetOffset: () -> Unit,
) {
    val label = stringResource(R.string.lyrics_line_actions_offset_label)
    var draftMs by remember(offsetMs) { mutableFloatStateOf(offsetMs.toFloat()) }
    val snappedMs = (draftMs / LyricsOffsetStore.STEP_MS).roundToInt() * LyricsOffsetStore.STEP_MS

    M3ESurface(
        level = M3ESurfaceLevel.Container,
        shape = M3EShape.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(M3ESpacing.large),
            verticalArrangement = Arrangement.spacedBy(M3ESpacing.medium),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(M3ESpacing.medium),
            ) {
                RowIcon(Icons.Rounded.Timer)
                // Title and value share one line: the pill trails the title instead of being
                // pushed to the far edge, which is how the settings sliders present their value.
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = M3ESpacing.small),
                    )
                    M3ESurface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = M3EShape.full,
                    ) {
                        Text(
                            text = formatOffset(snappedMs),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            modifier = Modifier.padding(
                                horizontal = M3ESpacing.small,
                                vertical = M3ESpacing.extraSmall,
                            ),
                        )
                    }
                }
                IconButton(
                    onClick = onResetOffset,
                    enabled = offsetMs != 0,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = stringResource(R.string.lyrics_line_actions_reset_offset),
                    )
                }
            }
            Slider(
                state = rememberSyncedSliderState(
                    value = snappedMs.toFloat(),
                    steps = OffsetSliderSteps,
                    valueRange = -OffsetSliderRangeMs..OffsetSliderRangeMs,
                ),
                onValueChange = { draftMs = it },
                onValueChangeFinished = { onOffsetChange(snappedMs) },
                // Without a label the reader only announces a number, not which control it is.
                modifier = Modifier.semantics {
                    contentDescription = label
                    stateDescription = formatOffset(snappedMs)
                },
            )
        }
    }
}

/**
 * Single-choice row.
 *
 * The lyric source is mutually exclusive, so it reads as a radio group: role and selected state
 * are announced, and the selected row takes the settings picker's treatment — `primaryContainer`
 * fill plus a check mark — rather than relying on container tint alone.
 */
@Composable
private fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    M3EListItem(
        headline = title,
        onClick = onClick,
        selected = selected,
        role = Role.RadioButton,
        shape = M3EShape.medium,
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        trailingContent = if (selected) {
            { Icon(imageVector = Icons.Rounded.Check, contentDescription = null) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Settings rows carry a bare icon tinted `secondary`; a tinted container would out-shout them. */
@Composable
private fun RowIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
    )
}

/** 2dp between rows, matching the gap a settings subsection puts between its items. */
private val RowGroupGap = 2.dp

/** Section labels sit one 12dp step inside the 16dp row inset, as they do on the settings page. */
private val SectionTitleInset = 12.dp

/** The settings page closes every subsection with a 10dp gap; not a full 24dp group break. */
private val SectionGap = 10.dp

/**
 * ±3s covers what a listener can judge by ear on a per-track offset. Anything wider is a wrong
 * recording, and it would also shrink a 0.1s nudge to a single pixel of slider travel.
 */
private const val OffsetSliderRangeMs = 3_000f

/**
 * One tick per [LyricsOffsetStore.STEP_MS] across the range, so the track shows the same discrete
 * stops a settings slider does instead of reading as a continuous stripe.
 */
private val OffsetSliderSteps = (OffsetSliderRangeMs * 2 / LyricsOffsetStore.STEP_MS).toInt() - 1

/** The three legacy presets keep their existing translations from the settings page. */
private fun LyricsSourcePreference.labelRes(): Int = when (this) {
    LyricsSourcePreference.EMBEDDED_FIRST -> R.string.setcat_lyrics_embedded_first
    LyricsSourcePreference.API_FIRST -> R.string.setcat_lyrics_online_first
    LyricsSourcePreference.LOCAL_FIRST -> R.string.setcat_lyrics_local_first
}

/** `+0.3 s` reads better than `+300 ms` next to a nudge button. */
internal fun formatOffset(offsetMs: Int): String {
    val sign = if (offsetMs > 0) "+" else ""
    return "$sign${"%.1f".format(offsetMs / 1000f)} s"
}
