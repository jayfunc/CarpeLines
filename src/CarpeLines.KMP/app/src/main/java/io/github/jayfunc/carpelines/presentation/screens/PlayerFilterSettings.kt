package io.github.jayfunc.carpelines.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.presentation.settings.search.settingHighlight
import io.github.jayfunc.carpelines.presentation.viewmodel.PlayerFilterViewModel
import io.github.jayfunc.carpelines.ui.theme.M3EShape

/**
 * The player filter controls which media sessions the app follows. Ignored players are not read,
 * so their tracks never reach the lyrics screen or the listening statistics.
 */
@Composable
internal fun PlayerFilterSettingsSection(
    viewModel: PlayerFilterViewModel,
    highlightKey: String? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Text(
        text = stringResource(R.string.player_filter_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
            .settingHighlight("item_player_filter", highlightKey),
    )

    SettingsSubsection(title = stringResource(R.string.settings_category_player_filter_title)) {
        SwitchSettingItem(
            title = stringResource(R.string.player_filter_enabled_title),
            subtitle = stringResource(
                if (state.filterEnabled) {
                    R.string.player_filter_enabled_subtitle_on
                } else {
                    R.string.player_filter_enabled_subtitle_off
                }
            ),
            checked = state.filterEnabled,
            onCheckedChange = viewModel::setFilterEnabled,
            leadingIcon = {
                Icon(Icons.Rounded.FilterAlt, null, tint = MaterialTheme.colorScheme.secondary)
            },
            shape = M3EShape.groupedCards(index = 0, count = 2),
        )
        SettingsItem(
            title = stringResource(R.string.player_filter_reset_title),
            subtitle = stringResource(R.string.player_filter_reset_subtitle),
            leadingIcon = {
                Icon(Icons.Rounded.RestartAlt, null, tint = MaterialTheme.colorScheme.secondary)
            },
            trailingIcon = {
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = viewModel::reset,
            shape = M3EShape.groupedCards(index = 1, count = 2),
        )
    }

    SettingsSubsectionHeader(stringResource(R.string.player_filter_detected_header))

    if (state.players.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.player_filter_empty_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.player_filter_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            state.players.forEachIndexed { index, player ->
                SwitchSettingItem(
                    title = player.appLabel,
                    subtitle = player.packageName,
                    // With the filter off every player is followed, so the switches read as on
                    // and stay inert until the master switch makes the list meaningful.
                    checked = if (state.filterEnabled) {
                        player.packageName in state.allowedPackages
                    } else {
                        true
                    },
                    enabled = state.filterEnabled,
                    onCheckedChange = { checked ->
                        viewModel.setPlayerAllowed(player.packageName, checked)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.PlayCircle,
                            null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    shape = M3EShape.groupedCards(index = index, count = state.players.size),
                )
            }
        }
    }

    if (state.hasNoSelectionWarning) {
        Text(
            text = stringResource(R.string.player_filter_none_selected_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }

    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.player_filter_unknown_notice),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
