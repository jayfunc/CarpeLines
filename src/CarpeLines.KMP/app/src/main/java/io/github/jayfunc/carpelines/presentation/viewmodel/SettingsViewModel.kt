package io.github.jayfunc.carpelines.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jayfunc.carpelines.data.backup.BackupManager
import io.github.jayfunc.carpelines.data.backup.format.BackupEncryptedException
import io.github.jayfunc.carpelines.data.backup.format.BackupWrongPassphraseException
import io.github.jayfunc.carpelines.data.backup.model.BackupSection
import io.github.jayfunc.carpelines.data.backup.model.BackupOperationType
import io.github.jayfunc.carpelines.data.backup.model.BackupTransferProgressUpdate
import io.github.jayfunc.carpelines.data.backup.model.BackupHistoryEntry
import io.github.jayfunc.carpelines.data.backup.model.RestorePlan
import io.github.jayfunc.carpelines.data.backup.model.RestoreResult
import io.github.jayfunc.carpelines.data.backup.model.ValidationError
import io.github.jayfunc.carpelines.data.model.AudioOutputMode
import io.github.jayfunc.carpelines.data.preferences.AppLanguage
import io.github.jayfunc.carpelines.data.preferences.AppThemeMode
import io.github.jayfunc.carpelines.data.preferences.LibraryNavigationMode
import io.github.jayfunc.carpelines.data.preferences.PreviousButtonBehavior
import io.github.jayfunc.carpelines.data.preferences.DEFAULT_LYRICS_DUET_LAYOUT_ENABLED
import io.github.jayfunc.carpelines.data.preferences.DEFAULT_LYRICS_FONT_SIZE_SP
import io.github.jayfunc.carpelines.data.preferences.DEFAULT_SHOW_LYRICS_BACKGROUND_VOCALS
import io.github.jayfunc.carpelines.data.preferences.ThemePreference
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.utils.FolderArtworkSettingsCoordinator
import io.github.jayfunc.carpelines.data.preferences.AlbumArtQuality
import io.github.jayfunc.carpelines.data.preferences.CollagePattern
import io.github.jayfunc.carpelines.data.preferences.FullPlayerLoadingTweaks
import io.github.jayfunc.carpelines.data.preferences.ThemePreferencesRepository
import io.github.jayfunc.carpelines.data.repository.LyricsRepository
import io.github.jayfunc.carpelines.data.repository.MusicRepository
import io.github.jayfunc.carpelines.data.model.LyricsSourcePreference
import io.github.jayfunc.carpelines.data.worker.SyncManager
import io.github.jayfunc.carpelines.data.worker.SyncProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import io.github.jayfunc.carpelines.di.AppScope
import javax.inject.Inject

import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.preferences.NavBarStyle
import io.github.jayfunc.carpelines.data.preferences.LaunchTab
import io.github.jayfunc.carpelines.data.model.Song
import io.github.jayfunc.carpelines.data.service.player.HiFiCapabilityChecker
import io.github.jayfunc.carpelines.utils.AppLocaleManager
import java.io.File
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

data class SettingsUiState(
    val isLoadingDirectories: Boolean = false,
    val appLanguageTag: String = AppLanguage.SYSTEM.tag,
    val appThemeMode: String = AppThemeMode.FOLLOW_SYSTEM,
    val globalNowPlayingThemeEnabled: Boolean = false,
    val playerThemePreference: String = ThemePreference.ALBUM_ART,
    val mockGenresEnabled: Boolean = false,
    val navBarCornerRadius: Int = 32,
    val navBarStyle: String = NavBarStyle.DEFAULT,
    val navBarCompactMode: Boolean = false,
    val libraryNavigationMode: String = LibraryNavigationMode.TAB_ROW,
    val launchTab: String = LaunchTab.HOME,
    val keepPlayingInBackground: Boolean = true,
    val keepScreenAwakeWhilePlaying: Boolean = false,
    val resumeOnHeadsetReconnect: Boolean = false,
    val showQueueHistory: Boolean = true,
    val isCrossfadeEnabled: Boolean = false,
    val smartCrossfadeEnabled: Boolean = false,
    val audioOutputMode: AudioOutputMode = AudioOutputMode.SYSTEM_DEFAULT,
    val pcmFloatOutputSupported: Boolean = true,
    val crossfadeDuration: Int = 2000,
    val persistentShuffleEnabled: Boolean = false,
    val folderBackGestureNavigation: Boolean = true,
    val lyricsSourcePreference: LyricsSourcePreference = LyricsSourcePreference.EMBEDDED_FIRST,
    val autoScanLrcFiles: Boolean = false,
    val externalLyricsEnabled: Boolean = true,
    val externalArtistImagesEnabled: Boolean = false,
    val blockedDirectories: Set<String> = emptySet(),
    val appRebrandDialogShown: Boolean = false,
    val fullPlayerLoadingTweaks: FullPlayerLoadingTweaks = FullPlayerLoadingTweaks(),
    val showPlayerFileInfo: Boolean = true,
    val showScrollbar: Boolean = true,
    val albumArtQuality: AlbumArtQuality = AlbumArtQuality.MEDIUM,
    val albumArtCacheLimitMb: Int = 200,
    val tapBackgroundClosesPlayer: Boolean = false,
    val miniPlayerSwipeToSkip: Boolean = false,
    val autoplayOnSkip: Boolean = true,
    val previousButtonBehavior: PreviousButtonBehavior = PreviousButtonBehavior.DEFAULT,
    val hapticsEnabled: Boolean = true,
    val immersiveLyricsEnabled: Boolean = false,
    val immersiveLyricsTimeout: Long = 4000L,
    val animatedLyricsBlurEnabled: Boolean = true,
    val animatedLyricsBlurStrength: Float = 5f,
    val lyricsFontSizeSp: Float = DEFAULT_LYRICS_FONT_SIZE_SP,
    val lyricsDuetLayoutEnabled: Boolean = DEFAULT_LYRICS_DUET_LAYOUT_ENABLED,
    val showLyricsBackgroundVocals: Boolean = DEFAULT_SHOW_LYRICS_BACKGROUND_VOCALS,
    val backupInfoDismissed: Boolean = false,
    val isDataTransferInProgress: Boolean = false,
    val restorePlan: RestorePlan? = null,
    val backupHistory: ImmutableList<BackupHistoryEntry> = persistentListOf(),
    val backupValidationErrors: List<ValidationError> = emptyList(),
    val isInspectingBackup: Boolean = false,
    // Encrypted backup password prompt state
    val pendingEncryptedBackupUri: String? = null,
    val wrongBackupPassword: Boolean = false,
    val collagePattern: CollagePattern = CollagePattern.default,
    val collageAutoRotate: Boolean = false,
    val minSongDuration: Int = 10000,
    val minTracksPerAlbum: Int = 1,
    val replayGainEnabled: Boolean = false,
    val replayGainUseAlbumGain: Boolean = false
)

data class FailedSongInfo(
    val id: String,
    val title: String,
    val artist: String
)

data class LyricsRefreshProgress(
    val totalSongs: Int = 0,
    val currentCount: Int = 0,
    val savedCount: Int = 0,
    val notFoundCount: Int = 0,
    val skippedCount: Int = 0,
    val isComplete: Boolean = false,
    val failedSongs: List<FailedSongInfo> = emptyList()
) {
    val hasProgress: Boolean get() = totalSongs > 0
    val progress: Float get() = if (totalSongs > 0) currentCount.toFloat() / totalSongs else 0f
    val hasFailedSongs: Boolean get() = failedSongs.isNotEmpty()
}

private sealed interface SettingsUiUpdate {
    data class Group1(
        val appRebrandDialogShown: Boolean,
        val appThemeMode: String,
        val playerThemePreference: String,
        val mockGenresEnabled: Boolean,
        val navBarCornerRadius: Int,
        val navBarStyle: String,
        val navBarCompactMode: Boolean,
        val libraryNavigationMode: String,
        val launchTab: String,
        val showPlayerFileInfo: Boolean,
        val globalNowPlayingThemeEnabled: Boolean
    ) : SettingsUiUpdate
    
    data class Group2(
        val keepPlayingInBackground: Boolean,
        val resumeOnHeadsetReconnect: Boolean,
        val showQueueHistory: Boolean,
        val isCrossfadeEnabled: Boolean,
        val audioOutputMode: AudioOutputMode,
        val crossfadeDuration: Int,
        val persistentShuffleEnabled: Boolean,
        val folderBackGestureNavigation: Boolean,
        val lyricsSourcePreference: LyricsSourcePreference,
        val autoScanLrcFiles: Boolean,
        val blockedDirectories: Set<String>,
        val hapticsEnabled: Boolean,
        val immersiveLyricsEnabled: Boolean,
        val immersiveLyricsTimeout: Long,
        val animatedLyricsBlurEnabled: Boolean,
        val animatedLyricsBlurStrength: Float
    ) : SettingsUiUpdate
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val syncManager: SyncManager,
    private val lyricsRepository: LyricsRepository,
    private val musicRepository: MusicRepository,
    private val backupManager: BackupManager,
    private val folderArtworkSettingsCoordinator: FolderArtworkSettingsCoordinator,
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
    private val betterLyricsRepository: io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val pcmFloatOutputSupported = HiFiCapabilityChecker.isSupported()

    private val fileExplorerStateHolder = FileExplorerStateHolder(userPreferencesRepository, viewModelScope, context)

    val currentPath = fileExplorerStateHolder.currentPath
    val currentDirectoryChildren = fileExplorerStateHolder.currentDirectoryChildren
    val blockedDirectories = fileExplorerStateHolder.blockedDirectories
    val availableStorages = fileExplorerStateHolder.availableStorages
    val selectedStorageIndex = fileExplorerStateHolder.selectedStorageIndex
    val isLoadingDirectories = fileExplorerStateHolder.isLoading
    val isExplorerPriming = fileExplorerStateHolder.isPrimingExplorer
    val isExplorerReady = fileExplorerStateHolder.isExplorerReady
    val isCurrentDirectoryResolved = fileExplorerStateHolder.isCurrentDirectoryResolved
    val allAudioFolders = fileExplorerStateHolder.allAudioFolders
    val isLoadingAllAudioFolders = fileExplorerStateHolder.isLoadingAllAudioFolders
    private var hasPendingDirectoryRuleChanges = false
    private var latestDirectoryRuleUpdateJob: Job? = null

    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val syncProgress: StateFlow<SyncProgress> = syncManager.syncProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncProgress()
        )

    /**
     * Local tracks in the scanned library. The local lyrics sources need a row to reach a file,
     * so this is the number that tells the user whether tag and .lrc lyrics can be found at all.
     */
    val librarySongCount: StateFlow<Int> = musicRepository.getSongCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _dataTransferEvents = Channel<String>(Channel.BUFFERED)
    val dataTransferEvents: Flow<String> = _dataTransferEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            backupManager.getBackupHistory().collect { history ->
                _uiState.update { it.copy(backupHistory = history.toImmutableList()) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.collagePatternFlow.collect { pattern ->
                _uiState.update { it.copy(collagePattern = pattern) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.collageAutoRotateFlow.collect { autoRotate ->
                _uiState.update { it.copy(collageAutoRotate = autoRotate) }
            }
        }
    }

    private val _dataTransferProgress = MutableStateFlow<BackupTransferProgressUpdate?>(null)
    val dataTransferProgress: StateFlow<BackupTransferProgressUpdate?> = _dataTransferProgress.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                pcmFloatOutputSupported = pcmFloatOutputSupported,
                appLanguageTag = AppLocaleManager.currentLanguageTag(context)
            )
        }

        viewModelScope.launch {
            combine<Any?, SettingsUiUpdate.Group1>(
                userPreferencesRepository.appRebrandDialogShownFlow,
                themePreferencesRepository.appThemeModeFlow,
                themePreferencesRepository.playerThemePreferenceFlow,
                userPreferencesRepository.mockGenresEnabledFlow,
                userPreferencesRepository.navBarCornerRadiusFlow,
                userPreferencesRepository.navBarStyleFlow,
                userPreferencesRepository.navBarCompactModeFlow,
                userPreferencesRepository.libraryNavigationModeFlow,
                userPreferencesRepository.launchTabFlow,
                userPreferencesRepository.showPlayerFileInfoFlow,
                themePreferencesRepository.globalNowPlayingThemeEnabledFlow
            ) { values ->
                SettingsUiUpdate.Group1(
                    appRebrandDialogShown = values[0] as Boolean,
                    appThemeMode = values[1] as String,
                    playerThemePreference = values[2] as String,
                    mockGenresEnabled = values[3] as Boolean,
                    navBarCornerRadius = values[4] as Int,
                    navBarStyle = values[5] as String,
                    navBarCompactMode = values[6] as Boolean,
                    libraryNavigationMode = values[7] as String,
                    launchTab = values[8] as String,
                    showPlayerFileInfo = values[9] as Boolean,
                    globalNowPlayingThemeEnabled = values[10] as Boolean
                )
            }.collect { update ->
                _uiState.update { state ->
                    state.copy(
                        appRebrandDialogShown = update.appRebrandDialogShown,
                        appThemeMode = update.appThemeMode,
                        playerThemePreference = update.playerThemePreference,
                        mockGenresEnabled = update.mockGenresEnabled,
                        navBarCornerRadius = update.navBarCornerRadius,
                        navBarStyle = update.navBarStyle,
                        navBarCompactMode = update.navBarCompactMode,
                        libraryNavigationMode = update.libraryNavigationMode,
                        launchTab = update.launchTab,
                        showPlayerFileInfo = update.showPlayerFileInfo,
                        globalNowPlayingThemeEnabled = update.globalNowPlayingThemeEnabled
                    )
                }
            }
        }
        
        viewModelScope.launch {
            combine<Any?, SettingsUiUpdate.Group2>(
                userPreferencesRepository.keepPlayingInBackgroundFlow,
                userPreferencesRepository.resumeOnHeadsetReconnectFlow,
                userPreferencesRepository.showQueueHistoryFlow,
                userPreferencesRepository.isCrossfadeEnabledFlow,
                userPreferencesRepository.audioOutputModeFlow,
                userPreferencesRepository.crossfadeDurationFlow,
                userPreferencesRepository.persistentShuffleEnabledFlow,
                userPreferencesRepository.folderBackGestureNavigationFlow,
                userPreferencesRepository.lyricsSourcePreferenceFlow,
                userPreferencesRepository.autoScanLrcFilesFlow,
                userPreferencesRepository.blockedDirectoriesFlow,
                userPreferencesRepository.hapticsEnabledFlow,
                userPreferencesRepository.immersiveLyricsEnabledFlow,
                userPreferencesRepository.immersiveLyricsTimeoutFlow,
                userPreferencesRepository.animatedLyricsBlurEnabledFlow,
                userPreferencesRepository.animatedLyricsBlurStrengthFlow
            ) { values ->
                val requestedAudioOutputMode = values[4] as AudioOutputMode
                SettingsUiUpdate.Group2(
                    keepPlayingInBackground = values[0] as Boolean,
                    resumeOnHeadsetReconnect = values[1] as Boolean,
                    showQueueHistory = values[2] as Boolean,
                    isCrossfadeEnabled = values[3] as Boolean,
                    audioOutputMode = if (
                        requestedAudioOutputMode == AudioOutputMode.PCM_FLOAT &&
                        !pcmFloatOutputSupported
                    ) {
                        AudioOutputMode.SYSTEM_DEFAULT
                    } else {
                        requestedAudioOutputMode
                    },
                    crossfadeDuration = values[5] as Int,
                    persistentShuffleEnabled = values[6] as Boolean,
                    folderBackGestureNavigation = values[7] as Boolean,
                    lyricsSourcePreference = values[8] as LyricsSourcePreference,
                    autoScanLrcFiles = values[9] as Boolean,
                    blockedDirectories = @Suppress("UNCHECKED_CAST") (values[10] as Set<String>),
                    hapticsEnabled = values[11] as Boolean,
                    immersiveLyricsEnabled = values[12] as Boolean,
                    immersiveLyricsTimeout = values[13] as Long,
                    animatedLyricsBlurEnabled = values[14] as Boolean,
                    animatedLyricsBlurStrength = values[15] as Float
                )
            }.collect { update ->
                _uiState.update { state ->
                    state.copy(
                        keepPlayingInBackground = update.keepPlayingInBackground,
                        resumeOnHeadsetReconnect = update.resumeOnHeadsetReconnect,
                        showQueueHistory = update.showQueueHistory,
                        isCrossfadeEnabled = update.isCrossfadeEnabled,
                        audioOutputMode = update.audioOutputMode,
                        crossfadeDuration = update.crossfadeDuration,
                        persistentShuffleEnabled = update.persistentShuffleEnabled,
                        folderBackGestureNavigation = update.folderBackGestureNavigation,
                        lyricsSourcePreference = update.lyricsSourcePreference,
                        autoScanLrcFiles = update.autoScanLrcFiles,
                        blockedDirectories = update.blockedDirectories,
                        hapticsEnabled = update.hapticsEnabled,
                        immersiveLyricsEnabled = update.immersiveLyricsEnabled,
                        immersiveLyricsTimeout = update.immersiveLyricsTimeout,
                        animatedLyricsBlurEnabled = update.animatedLyricsBlurEnabled,
                        animatedLyricsBlurStrength = update.animatedLyricsBlurStrength
                    )
                }
            }
        }
        
        viewModelScope.launch {
            userPreferencesRepository.smartCrossfadeEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(smartCrossfadeEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.keepScreenAwakeWhilePlayingFlow.collect { enabled ->
                _uiState.update { it.copy(keepScreenAwakeWhilePlaying = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.fullPlayerLoadingTweaksFlow.collect { tweaks ->
                _uiState.update { it.copy(fullPlayerLoadingTweaks = tweaks) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.lyricsFontSizeFlow.collect { fontSizeSp ->
                _uiState.update { it.copy(lyricsFontSizeSp = fontSizeSp) }
            }
        }

        viewModelScope.launch {
            combine(
                userPreferencesRepository.lyricsDuetLayoutEnabledFlow,
                userPreferencesRepository.showLyricsBackgroundVocalsFlow
            ) { duetLayoutEnabled, showBackgroundVocals ->
                duetLayoutEnabled to showBackgroundVocals
            }.collect { (duetLayoutEnabled, showBackgroundVocals) ->
                _uiState.update {
                    it.copy(
                        lyricsDuetLayoutEnabled = duetLayoutEnabled,
                        showLyricsBackgroundVocals = showBackgroundVocals
                    )
                }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.externalLyricsEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(externalLyricsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.externalArtistImagesEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(externalArtistImagesEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.backupInfoDismissedFlow.collect { dismissed ->
                _uiState.update { it.copy(backupInfoDismissed = dismissed) }
            }
        }

        viewModelScope.launch {
            fileExplorerStateHolder.isLoading.collect { loading ->
                _uiState.update { it.copy(isLoadingDirectories = loading) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.albumArtQualityFlow.collect { quality ->
                _uiState.update { it.copy(albumArtQuality = quality) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.albumArtCacheLimitMbFlow.collect { limitMb ->
                _uiState.update { it.copy(albumArtCacheLimitMb = limitMb) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.tapBackgroundClosesPlayerFlow.collect { enabled ->
                _uiState.update { it.copy(tapBackgroundClosesPlayer = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.miniPlayerSwipeToSkipFlow.collect { enabled ->
                _uiState.update { it.copy(miniPlayerSwipeToSkip = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.autoplayOnSkipFlow.collect { enabled ->
                _uiState.update { it.copy(autoplayOnSkip = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.previousButtonBehaviorFlow.collect { behavior ->
                _uiState.update { it.copy(previousButtonBehavior = behavior) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.minSongDurationFlow.collect { duration ->
                _uiState.update { it.copy(minSongDuration = duration) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.minTracksPerAlbumFlow.collect { minTracks ->
                _uiState.update { it.copy(minTracksPerAlbum = minTracks) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.replayGainEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(replayGainEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.replayGainUseAlbumGainFlow.collect { useAlbum ->
                _uiState.update { it.copy(replayGainUseAlbumGain = useAlbum) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.showScrollbarFlow.collect { enabled ->
                _uiState.update { it.copy(showScrollbar = enabled) }
            }
        }
    }

    fun setAppRebrandDialogShown(wasShown: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAppRebrandDialogShown(wasShown)
        }
    }

    // Directory rules are committed as the user leaves the excluded-directories screen,
    // which pops that destination and clears this view model in the same frame. Running the
    // write and the follow-up rescan on the application scope keeps them from being
    // cancelled halfway, which would drop the user's exclusions.
    fun toggleDirectoryAllowed(file: File) {
        hasPendingDirectoryRuleChanges = true
        latestDirectoryRuleUpdateJob = appScope.launch(Dispatchers.Main.immediate) {
            fileExplorerStateHolder.toggleDirectoryAllowed(file)
        }
    }

    fun applyPendingDirectoryRuleChanges() {
        if (!hasPendingDirectoryRuleChanges) return
        hasPendingDirectoryRuleChanges = false
        val pendingUpdate = latestDirectoryRuleUpdateJob
        appScope.launch {
            pendingUpdate?.join()
            syncManager.forceRefresh()
        }
    }

    // A system Back or predictive-back swipe at the explorer root pops the destination
    // without going through Done; committing here keeps that exit path from losing changes.
    override fun onCleared() {
        applyPendingDirectoryRuleChanges()
        super.onCleared()
    }

    fun loadDirectory(file: File) {
        fileExplorerStateHolder.loadDirectory(file)
    }

    fun primeExplorer() {
        fileExplorerStateHolder.primeExplorerRoot()
    }

    fun openExplorer() {
        fileExplorerStateHolder.openExplorerRoot()
    }

    fun navigateUp() {
        fileExplorerStateHolder.navigateUp()
    }

    fun refreshExplorer() {
        fileExplorerStateHolder.refreshCurrentDirectory()
    }

    fun loadAllAudioFolders(forceRefresh: Boolean = false) {
        fileExplorerStateHolder.loadAllAudioFolders(forceRefresh)
    }

    fun selectStorage(index: Int) {
        fileExplorerStateHolder.selectStorage(index)
    }

    fun refreshAvailableStorages() {
        fileExplorerStateHolder.refreshAvailableStorages()
    }

    fun isAtRoot(): Boolean = fileExplorerStateHolder.isAtRoot()

    fun explorerRoot(): File = fileExplorerStateHolder.rootDirectory()

    fun setPlayerThemePreference(preference: String) {
        viewModelScope.launch {
            themePreferencesRepository.setPlayerThemePreference(preference)
        }
    }

    fun setGlobalNowPlayingThemeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themePreferencesRepository.setGlobalNowPlayingThemeEnabled(enabled)
        }
    }

    fun setCollagePattern(pattern: CollagePattern) {
        viewModelScope.launch {
            userPreferencesRepository.setCollagePattern(pattern)
        }
    }

    fun setCollageAutoRotate(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setCollageAutoRotate(enabled)
        }
    }

    fun setAppLanguage(languageTag: String) {
        val normalized = AppLanguage.normalize(languageTag)
        AppLocaleManager.applyLanguage(context, normalized)
        _uiState.update { it.copy(appLanguageTag = normalized) }
    }

    fun setAppThemeMode(mode: String) {
        viewModelScope.launch {
            themePreferencesRepository.setAppThemeMode(mode)
        }
    }

    fun setNavBarStyle(style: String) {
        viewModelScope.launch {
            userPreferencesRepository.setNavBarStyle(style)
        }
    }

    fun setNavBarCompactMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNavBarCompactMode(enabled)
        }
    }

    fun setShowScrollbar(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowScrollbar(enabled)
        }
    }

    fun setLibraryNavigationMode(mode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setLibraryNavigationMode(mode)
        }
    }

    fun setShowPlayerFileInfo(show: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowPlayerFileInfo(show)
        }
    }

    fun setLaunchTab(tab: String) {
        viewModelScope.launch {
            userPreferencesRepository.setLaunchTab(tab)
        }
    }

    fun setKeepPlayingInBackground(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setKeepPlayingInBackground(enabled)
        }
    }

    fun setKeepScreenAwakeWhilePlaying(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setKeepScreenAwakeWhilePlaying(enabled)
        }
    }

    fun setResumeOnHeadsetReconnect(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setResumeOnHeadsetReconnect(enabled)
        }
    }

    fun setAudioOutputMode(mode: AudioOutputMode) {
        viewModelScope.launch {
            val supportedMode = if (
                mode == AudioOutputMode.PCM_FLOAT && !pcmFloatOutputSupported
            ) {
                AudioOutputMode.SYSTEM_DEFAULT
            } else {
                mode
            }
            userPreferencesRepository.setAudioOutputMode(supportedMode)
        }
    }

    fun setShowQueueHistory(show: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowQueueHistory(show)
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setCrossfadeEnabled(enabled)
        }
    }

    fun setCrossfadeDuration(duration: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setCrossfadeDuration(duration)
        }
    }

    fun setSmartCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSmartCrossfadeEnabled(enabled)
        }
    }

    val playbackSpeed: StateFlow<Float> = userPreferencesRepository.playbackSpeedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setPlaybackSpeed(speed)
        }
    }

    fun setPersistentShuffleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPersistentShuffleEnabled(enabled)
        }
    }

    fun setFolderBackGestureNavigation(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFolderBackGestureNavigation(enabled)
        }
    }

    fun setLyricsSourcePreference(preference: LyricsSourcePreference) {
        // The reorderable list is the single source of truth, so the preset rewrites its order.
        betterLyricsRepository.applyPreset(preference)
        viewModelScope.launch {
            userPreferencesRepository.setLyricsSourcePreference(preference)
        }
    }

    fun setAutoScanLrcFiles(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoScanLrcFiles(enabled)
        }
    }

    fun setExternalLyricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setExternalLyricsEnabled(enabled)
        }
    }

    fun setExternalArtistImagesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setExternalArtistImagesEnabled(enabled)
        }
    }

    fun setDelayAllFullPlayerContent(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDelayAllFullPlayerContent(enabled)
        }
    }

    fun setDelayAlbumCarousel(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDelayAlbumCarousel(enabled)
        }
    }

    fun setDelaySongMetadata(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDelaySongMetadata(enabled)
        }
    }

    fun setDelayProgressBar(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDelayProgressBar(enabled)
        }
    }

    fun setDelayControls(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDelayControls(enabled)
        }
    }

    fun setFullPlayerPlaceholders(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFullPlayerPlaceholders(enabled)
            if (!enabled) {
                userPreferencesRepository.setTransparentPlaceholders(false)
            }
        }
    }

    fun setTransparentPlaceholders(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTransparentPlaceholders(enabled)
        }
    }

    fun setFullPlayerPlaceholdersOnClose(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFullPlayerPlaceholdersOnClose(enabled)
        }
    }

    fun setFullPlayerSwitchOnDragRelease(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFullPlayerSwitchOnDragRelease(enabled)
        }
    }

    fun setFullPlayerAppearThreshold(thresholdPercent: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setFullPlayerAppearThreshold(thresholdPercent)
        }
    }

    fun setFullPlayerCloseThreshold(thresholdPercent: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setFullPlayerCloseThreshold(thresholdPercent)
        }
    }

    fun setAnimatedLyricsBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAnimatedLyricsBlurEnabled(enabled)
        }
    }

    fun setLyricsFontSize(fontSizeSp: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setLyricsFontSize(fontSizeSp)
        }
    }

    fun setAnimatedLyricsBlurStrength(strength: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setAnimatedLyricsBlurStrength(strength)
        }
    }

    fun setLyricsDuetLayoutEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setLyricsDuetLayoutEnabled(enabled)
        }
    }

    fun setShowLyricsBackgroundVocals(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowLyricsBackgroundVocals(enabled)
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            if (isSyncing.value) return@launch
            syncManager.forceRefresh()
        }
    }




    /**
     * Performs a full library rescan - rescans all files from scratch.
     * Use when songs are missing or metadata is incorrect.
     */
    fun fullSyncLibrary() {
        viewModelScope.launch {
            if (isSyncing.value) return@launch
            syncManager.fullSync()
        }
    }

    fun setMinSongDuration(durationMs: Int) {
        viewModelScope.launch {
            if (durationMs == _uiState.value.minSongDuration) return@launch
            userPreferencesRepository.setMinSongDuration(durationMs)
            syncManager.fullSync(deepScan = false)
        }
    }

    fun setMinTracksPerAlbum(minTracks: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setMinTracksPerAlbum(minTracks)
        }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setReplayGainEnabled(enabled)
        }
    }

    fun setReplayGainUseAlbumGain(useAlbumGain: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setReplayGainUseAlbumGain(useAlbumGain)
        }
    }

    fun setImmersiveLyricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setImmersiveLyricsEnabled(enabled)
        }
    }

    fun setImmersiveLyricsTimeout(timeout: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setImmersiveLyricsTimeout(timeout)
        }
    }

    /**
     * Rebuilds local MediaStore-backed songs from scratch while preserving cloud sources.
     * Local imported lyrics, favorites, and user metadata edits are removed for rebuilt songs.
     * Use when local library data is corrupted or as a last resort.
     */
    fun rebuildDatabase() {
        viewModelScope.launch {
            if (isSyncing.value) return@launch
            syncManager.rebuildDatabase()
        }
    }

    fun setNavBarCornerRadius(radius: Int) {
        viewModelScope.launch { userPreferencesRepository.setNavBarCornerRadius(radius) }
    }
    /**
     * Triggers a test crash to verify the crash handler is working correctly.
     * This should only be used for testing in Developer Options.
     */
    fun triggerTestCrash() {
        throw RuntimeException(context.getString(R.string.dev_test_crash_message))
    }

    fun resetSetupFlow() {
        viewModelScope.launch {
            userPreferencesRepository.setInitialSetupDone(false)
        }
    }

    val albumArtQuality: StateFlow<AlbumArtQuality> = userPreferencesRepository.albumArtQualityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumArtQuality.MEDIUM)

    val useSmoothCorners: StateFlow<Boolean> = userPreferencesRepository.useSmoothCornersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val tapBackgroundClosesPlayer: StateFlow<Boolean> = userPreferencesRepository.tapBackgroundClosesPlayerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val useFolderAlbumArt: StateFlow<Boolean> = userPreferencesRepository.useFolderAlbumArtFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAlbumArtQuality(quality: AlbumArtQuality) {
        viewModelScope.launch {
            userPreferencesRepository.setAlbumArtQuality(quality)
        }
    }

    fun setAlbumArtCacheLimitMb(limitMb: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setAlbumArtCacheLimitMb(limitMb)
            io.github.jayfunc.carpelines.utils.AlbumArtCacheManager.configuredCacheLimitMb = limitMb.toLong()
        }
    }

    /**
     * Folder covers outrank embedded artwork, so flipping this in either direction invalidates
     * every cached cover and every "no art" marker: they were all resolved under the old
     * precedence. The rescan then rebuilds each song's stored artwork URI.
     */
    fun setUseFolderAlbumArt(enabled: Boolean) {
        viewModelScope.launch {
            folderArtworkSettingsCoordinator.setEnabled(enabled)
        }
    }

    fun setUseSmoothCorners(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUseSmoothCorners(enabled)
        }
    }

    fun setMiniPlayerSwipeToSkip(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setMiniPlayerSwipeToSkip(enabled)
        }
    }

    fun setAutoplayOnSkip(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoplayOnSkip(enabled)
        }
    }

    fun setPreviousButtonBehavior(behavior: PreviousButtonBehavior) {
        viewModelScope.launch {
            userPreferencesRepository.setPreviousButtonBehavior(behavior)
        }
    }

    fun setTapBackgroundClosesPlayer(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTapBackgroundClosesPlayer(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHapticsEnabled(enabled)
        }
    }

    fun setBackupInfoDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupInfoDismissed(dismissed)
        }
    }

    fun exportAppData(uri: Uri, sections: Set<BackupSection>, passphrase: String? = null) {
        if (sections.isEmpty() || _uiState.value.isDataTransferInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDataTransferInProgress = true) }
            _dataTransferProgress.value = BackupTransferProgressUpdate(
                operation = BackupOperationType.EXPORT,
                step = 0,
                totalSteps = 1,
                title = context.getString(R.string.backup_progress_preparing_backup),
                detail = context.getString(R.string.backup_progress_starting_backup_task),
            )
            val result = backupManager.export(uri, sections, passphrase) { progress ->
                _dataTransferProgress.value = progress
            }
            result.fold(
                onSuccess = { _dataTransferEvents.send(context.getString(R.string.data_exported_successfully)) },
                onFailure = {
                    _dataTransferEvents.send(
                        context.getString(
                            R.string.export_failed_format,
                            it.localizedMessage ?: context.getString(R.string.error_unknown),
                        ),
                    )
                },
            )
            delay(300)
            _uiState.update { it.copy(isDataTransferInProgress = false) }
            _dataTransferProgress.value = null
        }
    }

    fun inspectBackupFile(uri: Uri, passphrase: String? = null) {
        if (_uiState.value.isInspectingBackup) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInspectingBackup = true,
                    backupValidationErrors = emptyList(),
                    restorePlan = null,
                    wrongBackupPassword = false
                )
            }
            val result = backupManager.inspectBackup(uri, passphrase)
            result.fold(
                onSuccess = { plan ->
                    _uiState.update {
                        it.copy(
                            restorePlan = plan,
                            isInspectingBackup = false,
                            pendingEncryptedBackupUri = null,
                            wrongBackupPassword = false
                        )
                    }
                },
                onFailure = { error ->
                    when (error) {
                        is BackupEncryptedException -> _uiState.update {
                            it.copy(isInspectingBackup = false, pendingEncryptedBackupUri = uri.toString())
                        }
                        is BackupWrongPassphraseException -> _uiState.update {
                            it.copy(
                                isInspectingBackup = false,
                                pendingEncryptedBackupUri = uri.toString(),
                                wrongBackupPassword = true
                            )
                        }
                        else -> {
                            _dataTransferEvents.send(
                                context.getString(
                                    R.string.backup_invalid_format,
                                    error.localizedMessage ?: context.getString(R.string.error_unknown),
                                ),
                            )
                            _uiState.update { it.copy(isInspectingBackup = false) }
                        }
                    }
                }
            )
        }
    }

    /** Called when the user dismisses the encrypted-backup password prompt. */
    fun dismissEncryptedBackupPrompt() {
        _uiState.update {
            it.copy(
                pendingEncryptedBackupUri = null,
                wrongBackupPassword = false
            )
        }
    }

    fun updateRestorePlanSelection(selectedModules: Set<BackupSection>) {
        _uiState.update { state ->
            state.restorePlan?.let { plan ->
                state.copy(restorePlan = plan.copy(selectedModules = selectedModules))
            } ?: state
        }
    }

    fun restoreFromPlan(uri: Uri) {
        val plan = _uiState.value.restorePlan ?: return
        if (plan.selectedModules.isEmpty() || _uiState.value.isDataTransferInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDataTransferInProgress = true) }
            _dataTransferProgress.value = BackupTransferProgressUpdate(
                operation = BackupOperationType.IMPORT,
                step = 0,
                totalSteps = 1,
                title = context.getString(R.string.backup_progress_preparing_restore),
                detail = context.getString(R.string.backup_progress_starting_task),
            )
            val result = backupManager.restore(uri, plan) { progress ->
                _dataTransferProgress.value = progress
            }
            when (result) {
                is RestoreResult.Success -> {
                    _dataTransferEvents.send(context.getString(R.string.data_restored_successfully))
                    syncManager.sync()
                }
                is RestoreResult.PartialFailure -> {
                    val failedNames = result.failed.entries.joinToString { "${context.getString(it.key.labelRes)}: ${it.value}" }
                    _dataTransferEvents.send(
                        context.getString(R.string.restore_partial_unresolved_format, failedNames),
                    )
                    if (result.succeeded.isNotEmpty() || !result.rolledBack) {
                        syncManager.sync()
                    }
                }
                is RestoreResult.TotalFailure -> {
                    _dataTransferEvents.send(context.getString(R.string.restore_failed_format, result.error))
                }
            }
            delay(300)
            _uiState.update { it.copy(isDataTransferInProgress = false, restorePlan = null) }
            _dataTransferProgress.value = null
        }
    }

    fun clearRestorePlan() {
        _uiState.value.restorePlan?.backupUri?.let { planUri ->
            runCatching { backupManager.discardDecryptedBackup(Uri.parse(planUri)) }
        }
        _uiState.update {
            it.copy(
                restorePlan = null,
                backupValidationErrors = emptyList(),
                pendingEncryptedBackupUri = null,
                wrongBackupPassword = false
            )
        }
    }

    fun removeBackupHistoryEntry(entry: BackupHistoryEntry) {
        viewModelScope.launch {
            backupManager.removeBackupHistoryEntry(entry.uri)
        }
    }

}
