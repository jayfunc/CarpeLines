package io.github.jayfunc.carpelines.presentation.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.worker.SyncProgress
import io.github.jayfunc.carpelines.presentation.settings.search.settingHighlight
import io.github.jayfunc.carpelines.presentation.viewmodel.SettingsViewModel
import io.github.jayfunc.carpelines.ui.m3e.M3ESurface
import io.github.jayfunc.carpelines.ui.m3e.M3ESurfaceLevel
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing

/**
 * The device library that the local lyrics sources read from.
 *
 * CarpeLines plays nothing itself, so "importing music" means letting the scanner index the
 * audio already on the phone: a track only gets its tag lyrics and its sibling .lrc file looked
 * at when the library holds a row for it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LocalMusicSettingsSection(
    settingsViewModel: SettingsViewModel,
    highlightKey: String?,
    onRebuildRequested: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val isSyncing by settingsViewModel.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by settingsViewModel.syncProgress.collectAsStateWithLifecycle()
    val songCount by settingsViewModel.librarySongCount.collectAsStateWithLifecycle()

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasAudioAccess by remember { mutableStateOf(context.hasPermission(audioPermission)) }
    // Access can be revoked in the system settings while the app is in the background, and the
    // scanner then fails silently: re-read it whenever this page comes back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasAudioAccess = context.hasPermission(audioPermission)
    }
    val permissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioAccess = granted
        if (granted) settingsViewModel.refreshLibrary()
    }

    SettingsSubsection(title = stringResource(R.string.local_music_section_library)) {
        M3ESurface(
            modifier = Modifier
                .fillMaxWidth()
                .settingHighlight("item_local_music_library", highlightKey),
            level = M3ESurfaceLevel.Container,
            shape = M3EShape.medium,
        ) {
            Column(
                modifier = Modifier.padding(M3ESpacing.large),
                verticalArrangement = Arrangement.spacedBy(M3ESpacing.small),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.size(M3ESpacing.medium))
                    Text(
                        text = if (songCount > 0) {
                            stringResource(R.string.local_music_song_count, songCount)
                        } else {
                            stringResource(R.string.local_music_song_count_empty)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.local_music_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isSyncing) {
                    Spacer(Modifier.height(M3ESpacing.small))
                    if (syncProgress.hasProgress) {
                        LinearWavyProgressIndicator(
                            progress = { syncProgress.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        text = syncProgressLabel(syncProgress),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    SettingsSubsection(title = stringResource(R.string.local_music_section_actions)) {
        SettingsItem(
            title = stringResource(R.string.local_music_scan_title),
            subtitle = stringResource(R.string.local_music_scan_subtitle),
            leadingIcon = { Icon(Icons.Outlined.Refresh, null) },
            supportingTextMaxLines = Int.MAX_VALUE,
            onClick = { settingsViewModel.refreshLibrary() },
        )
        SettingsItem(
            title = stringResource(R.string.local_music_rescan_title),
            subtitle = stringResource(R.string.local_music_rescan_subtitle),
            leadingIcon = { Icon(Icons.Outlined.Sync, null) },
            supportingTextMaxLines = Int.MAX_VALUE,
            onClick = { settingsViewModel.fullSyncLibrary() },
        )
        SettingsItem(
            title = stringResource(R.string.local_music_rebuild_title),
            subtitle = stringResource(R.string.local_music_rebuild_subtitle),
            leadingIcon = { Icon(Icons.Outlined.DeleteForever, null) },
            supportingTextMaxLines = Int.MAX_VALUE,
            onClick = onRebuildRequested,
        )
        SwitchSettingItem(
            title = stringResource(R.string.local_music_auto_lrc_title),
            subtitle = stringResource(R.string.local_music_auto_lrc_subtitle),
            checked = uiState.autoScanLrcFiles,
            onCheckedChange = { settingsViewModel.setAutoScanLrcFiles(it) },
            modifier = Modifier.settingHighlight("item_local_music_auto_lrc", highlightKey),
        )
    }

    if (!hasAudioAccess) {
        SettingsSubsection(title = stringResource(R.string.local_music_section_permission)) {
            M3ESurface(
                modifier = Modifier.fillMaxWidth(),
                level = M3ESurfaceLevel.Container,
                shape = M3EShape.medium,
            ) {
                Column(
                    modifier = Modifier.padding(M3ESpacing.large),
                    verticalArrangement = Arrangement.spacedBy(M3ESpacing.small),
                ) {
                    Text(
                        text = stringResource(R.string.local_music_permission_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.local_music_permission_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { permissionRequest.launch(audioPermission) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.local_music_permission_grant))
                    }
                }
            }
        }
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

@Composable
private fun syncProgressLabel(progress: SyncProgress): String {
    val phase = syncPhaseLabel(progress.phase)
    return if (progress.hasProgress) {
        stringResource(
            R.string.local_music_syncing,
            phase,
            (progress.progress * 100).toInt(),
        )
    } else {
        stringResource(R.string.local_music_syncing_indeterminate, phase)
    }
}
