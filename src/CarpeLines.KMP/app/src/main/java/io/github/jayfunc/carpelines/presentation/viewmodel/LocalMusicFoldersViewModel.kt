package io.github.jayfunc.carpelines.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jayfunc.carpelines.data.database.LyricsDao
import io.github.jayfunc.carpelines.data.database.LocalFolderStatsRow
import io.github.jayfunc.carpelines.data.database.MusicDao
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.data.worker.SyncManager
import io.github.jayfunc.carpelines.utils.LocalLibraryFolderFilter
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One imported local music root, with what the last scan found inside it. */
data class LocalMusicFolderUi(
    val path: String,
    val trackCount: Int,
    val embeddedLyricsCount: Int,
    val embeddedArtworkCount: Int,
    val lrcLyricsCount: Int,
) {
    val displayName: String
        get() = path.substringAfterLast('/').ifBlank { path }
}

/**
 * Imported local music folders and their deep-scan statistics.
 *
 * The folders scope where the scanner reads embedded lyrics, embedded album art and sibling
 * .lrc files; the song list itself still comes from the whole-device MediaStore scan. Importing
 * schedules a full rescan so existing tracks in the new folder are read once; removing a folder
 * only drops it from this list and never touches the library or the files.
 */
@HiltViewModel
class LocalMusicFoldersViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val musicDao: MusicDao,
    private val lyricsDao: LyricsDao,
    private val syncManager: SyncManager,
) : ViewModel() {

    val folders: StateFlow<List<LocalMusicFolderUi>> = combine(
        userPreferencesRepository.localLibraryFoldersFlow,
        musicDao.observeLocalFolderStatsRows(),
        lyricsDao.observeSongIdsWithLocalFileLyrics(),
    ) { roots, rows, lrcSongIds ->
        val lrcIds = lrcSongIds.toHashSet()
        roots
            .map { root -> buildFolder(root, rows, lrcIds) }
            .sortedBy { it.displayName.lowercase() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun importFolder(path: String) {
        val normalized = LocalLibraryFolderFilter.normalize(path)
        if (normalized.isBlank()) return
        viewModelScope.launch {
            val current = userPreferencesRepository.localLibraryFoldersFlow.first()
            if (normalized in current) return@launch
            userPreferencesRepository.setLocalLibraryFolders(current + normalized)
            syncManager.fullSync()
        }
    }

    fun removeFolder(path: String) {
        viewModelScope.launch {
            val current = userPreferencesRepository.localLibraryFoldersFlow.first()
            if (path !in current) return@launch
            userPreferencesRepository.setLocalLibraryFolders(current - path)
        }
    }

    private fun buildFolder(
        root: String,
        rows: List<LocalFolderStatsRow>,
        lrcSongIds: Set<Long>,
    ): LocalMusicFolderUi {
        val songs = rows.filter { LocalLibraryFolderFilter.isUnder(it.filePath, root) }
        return LocalMusicFolderUi(
            path = root,
            trackCount = songs.size,
            embeddedLyricsCount = songs.count { it.hasEmbeddedLyrics },
            embeddedArtworkCount = songs.count { it.hasEmbeddedAlbumArt },
            lrcLyricsCount = songs.count { it.id in lrcSongIds },
        )
    }
}
