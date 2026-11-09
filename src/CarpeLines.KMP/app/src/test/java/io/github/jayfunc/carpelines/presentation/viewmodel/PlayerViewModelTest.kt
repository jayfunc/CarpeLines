package io.github.jayfunc.carpelines.presentation.viewmodel

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import app.cash.turbine.test
import io.github.jayfunc.carpelines.data.database.AlbumArtThemeDao
import com.google.common.util.concurrent.ListenableFuture
import io.github.jayfunc.carpelines.data.model.SearchFilterType
import io.github.jayfunc.carpelines.data.model.SearchHistoryItem
import io.github.jayfunc.carpelines.data.model.SearchResultItem
import io.github.jayfunc.carpelines.data.model.Album
import io.github.jayfunc.carpelines.data.model.Artist
import io.github.jayfunc.carpelines.data.model.LibraryTabId
import io.github.jayfunc.carpelines.data.model.Song
import io.github.jayfunc.carpelines.data.model.SortOption
import io.github.jayfunc.carpelines.data.model.StorageFilter
import io.github.jayfunc.carpelines.data.preferences.ThemePreferencesRepository
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.data.repository.MusicRepository
import io.mockk.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.github.jayfunc.carpelines.MainCoroutineExtension
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.service.player.DualPlayerEngine
import io.github.jayfunc.carpelines.data.worker.SyncManager
import io.github.jayfunc.carpelines.di.DispatcherProvider
import io.github.jayfunc.carpelines.utils.MediaItemBuilder
import io.github.jayfunc.carpelines.utils.MediaStorePermissionHelper
import io.github.jayfunc.carpelines.presentation.viewmodel.*
import app.cash.turbine.Turbine

import androidx.core.content.ContextCompat

@ExperimentalCoroutinesApi
@ExtendWith(MainCoroutineExtension::class)
class PlayerViewModelTest {

    private lateinit var playerViewModel: PlayerViewModel
    private val mockMusicRepository: MusicRepository = mockk()
    private val mockUserPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val mockThemePreferencesRepository: ThemePreferencesRepository = mockk(relaxed = true)
    private val mockAlbumArtThemeDao: AlbumArtThemeDao = mockk(relaxed = true)
    private val mockContext: Context = mockk(relaxed = true)

    private val mockSyncManager: SyncManager = mockk(relaxed = true)
    private val mockDualPlayerEngine: DualPlayerEngine = mockk(relaxed = true)
    private val mockListeningStatsTracker: ListeningStatsTracker = mockk(relaxed = true)
    private val mockDailyMixStateHolder: DailyMixStateHolder = mockk(relaxed = true)
    private val mockLyricsStateHolder: LyricsStateHolder = mockk(relaxed = true)
    private val mockQueueStateHolder: QueueStateHolder = mockk(relaxed = true)
    private val mockQueueUndoStateHolder: QueueUndoStateHolder = mockk(relaxed = true)
    private val mockPlaybackStateHolder: PlaybackStateHolder = mockk(relaxed = true)
    private val mockConnectivityStateHolder: ConnectivityStateHolder = mockk(relaxed = true)
    private val mockSleepTimerStateHolder: SleepTimerStateHolder = mockk(relaxed = true)
    private val mockSearchStateHolder: SearchStateHolder = mockk(relaxed = true)
    private val mockLibraryStateHolder: LibraryStateHolder = mockk(relaxed = true)
    private val mockFolderNavigationStateHolder: FolderNavigationStateHolder = mockk(relaxed = true)
    private val mockLibraryTabsStateHolder: LibraryTabsStateHolder = mockk(relaxed = true)
    private val mockMetadataEditStateHolder: MetadataEditStateHolder = mockk(relaxed = true)
    private val mockSongRemovalStateHolder: SongRemovalStateHolder = mockk(relaxed = true)
    private val mockExternalMediaStateHolder: ExternalMediaStateHolder = mockk(relaxed = true)
    private val mockThemeStateHolder: ThemeStateHolder = mockk(relaxed = true)
    private val mockMultiSelectionStateHolder: MultiSelectionStateHolder = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    // Keeps the view model's background hops on the test scheduler: work left on a real pool
    // would resume through Dispatchers.Main after tearDown resets it and fail a later test.
    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    private val _allSongsFlow = MutableStateFlow<ImmutableList<Song>>(persistentListOf())
    private val _searchHistoryFlow = MutableStateFlow<ImmutableList<SearchHistoryItem>>(persistentListOf())
    private val _searchResultsFlow = MutableStateFlow<ImmutableList<SearchResultItem>>(persistentListOf())
    private val _selectedSearchFilterFlow = MutableStateFlow(SearchFilterType.ALL)
    private val _repeatModeFlow = MutableStateFlow(Player.REPEAT_MODE_OFF)
    private val folderSongSortFlow = MutableStateFlow<SortOption>(SortOption.FolderSongTitleAZ)
    private lateinit var stablePlayerStateFlow: MutableStateFlow<StablePlayerState>
    private lateinit var mockController: MediaController
    private val controllerRepeatModeWrites = mutableListOf<Int>()
    private var controllerRepeatMode = Player.REPEAT_MODE_OFF

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this)

        mockkStatic(ContextCompat::class)
        val directExecutor = java.util.concurrent.Executor { it.run() }
        every { ContextCompat.getMainExecutor(any()) } returns directExecutor
        coEvery { mockUserPreferencesRepository.songsSortOptionFlow } returns flowOf("SongTitleAZ")
        coEvery { mockUserPreferencesRepository.albumsSortOptionFlow } returns flowOf("AlbumTitleAZ")
        coEvery { mockUserPreferencesRepository.artistsSortOptionFlow } returns flowOf("ArtistNameAZ")
        coEvery { mockUserPreferencesRepository.navBarCornerRadiusFlow } returns flowOf(32)
        coEvery { mockUserPreferencesRepository.navBarStyleFlow } returns flowOf("Default")
        coEvery { mockUserPreferencesRepository.libraryNavigationModeFlow } returns flowOf("TabRow")
        coEvery { mockUserPreferencesRepository.carouselStyleFlow } returns flowOf("NoPeek")
        coEvery { mockUserPreferencesRepository.fullPlayerLoadingTweaksFlow } returns flowOf(io.github.jayfunc.carpelines.data.preferences.FullPlayerLoadingTweaks())
        coEvery { mockUserPreferencesRepository.tapBackgroundClosesPlayerFlow } returns flowOf(true)
        coEvery { mockUserPreferencesRepository.hapticsEnabledFlow } returns flowOf(true)
        coEvery { mockUserPreferencesRepository.foldersSortOptionFlow } returns flowOf("FolderNameAZ")
        coEvery { mockUserPreferencesRepository.folderSongsSortOptionFlow } returns flowOf(SortOption.FolderSongTitleAZ.storageKey)
        coEvery { mockUserPreferencesRepository.persistentShuffleEnabledFlow } returns flowOf(false)
        coEvery { mockUserPreferencesRepository.isShuffleOnFlow } returns flowOf(false)
        every { mockUserPreferencesRepository.repeatModeFlow } returns _repeatModeFlow
        coEvery { mockThemePreferencesRepository.playerThemePreferenceFlow } returns flowOf("Global")
        every { mockLibraryStateHolder.allSongs } returns _allSongsFlow
        every { mockLibraryStateHolder.isLoadingLibrary } returns MutableStateFlow(false)
        every { mockLibraryStateHolder.isLoadingCategories } returns MutableStateFlow(false)
        every { mockLibraryStateHolder.genres } returns MutableStateFlow(persistentListOf())
        every { mockLibraryStateHolder.albums } returns MutableStateFlow(persistentListOf())
        every { mockLibraryStateHolder.artists } returns MutableStateFlow(persistentListOf())
        every { mockLibraryStateHolder.musicFolders } returns MutableStateFlow(persistentListOf())
        every { mockLibraryStateHolder.currentSongSortOption } returns MutableStateFlow<SortOption>(SortOption.SongTitleAZ)
        every { mockLibraryStateHolder.currentAlbumSortOption } returns MutableStateFlow<SortOption>(SortOption.AlbumTitleAZ)
        every { mockLibraryStateHolder.currentArtistSortOption } returns MutableStateFlow<SortOption>(SortOption.ArtistNameAZ)
        every { mockLibraryStateHolder.currentFolderSortOption } returns MutableStateFlow<SortOption>(SortOption.FolderNameAZ)
        every { mockLibraryStateHolder.currentFolderSongSortOption } returns folderSongSortFlow
        every { mockLibraryStateHolder.currentStorageFilter } returns MutableStateFlow(StorageFilter.ALL)

        every { mockSearchStateHolder.searchHistory } returns _searchHistoryFlow
        every { mockSearchStateHolder.searchResults } returns _searchResultsFlow
        every { mockSearchStateHolder.selectedSearchFilter } returns _selectedSearchFilterFlow
        every { mockSearchStateHolder.loadSearchHistory(any()) } just runs
        every { mockSearchStateHolder.clearSearchHistory() } just runs
        every { mockSearchStateHolder.deleteSearchHistoryItem(any()) } just runs
        every { mockSearchStateHolder.updateSearchFilter(any()) } just runs
        every { mockSearchStateHolder.initialize(any()) } just runs

        every { mockConnectivityStateHolder.initialize(any()) } just runs

        stablePlayerStateFlow = MutableStateFlow(StablePlayerState(currentSong = null))
        every { mockPlaybackStateHolder.stablePlayerState } returns stablePlayerStateFlow
        every { mockPlaybackStateHolder.setMediaController(any()) } just runs

        every { mockSleepTimerStateHolder.initialize(any(), any(), any(), any()) } just runs
        every { mockLibraryStateHolder.initialize(any()) } just runs

        every { mockMusicRepository.getPaginatedSongs(any(), any()) } returns flowOf(androidx.paging.PagingData.empty())
        every { mockMusicRepository.getAudioFiles() } returns flowOf(emptyList())
        every { mockMusicRepository.getDistinctAlbumArtSongs() } returns flowOf(emptyList())
        every { mockMusicRepository.getHomeMixPreviewSongs(any()) } returns flowOf(emptyList())
        every { mockMusicRepository.getSongCountFlow() } returns flowOf(0)
        every { mockMusicRepository.searchSongs(any()) } returns flowOf(emptyList())
        every { mockMusicRepository.getMusicByGenre(any()) } returns flowOf(emptyList())
        every { mockMusicRepository.getSong(any()) } returns flowOf(null)
        coEvery { mockMusicRepository.getSongIdByContentUri(any()) } returns null
        coEvery { mockMusicRepository.getSongByPath(any()) } returns null
        coEvery { mockMusicRepository.getAllSongsOnce() } returns emptyList()
        coEvery { mockMusicRepository.getFirstPlayableSong() } returns null
        coEvery { mockMusicRepository.getRandomSongs(any()) } returns emptyList()
        coEvery { mockMusicRepository.getSongIdsSorted(any(), any()) } returns emptyList()
        every { mockLyricsStateHolder.songUpdates } returns MutableSharedFlow()

        mockController = mockk(relaxed = true)
        every { mockController.repeatMode } answers { controllerRepeatMode }
        every { mockController.repeatMode = any() } answers {
            controllerRepeatMode = firstArg()
            controllerRepeatModeWrites += controllerRepeatMode
        }
        
        playerViewModel = PlayerViewModel(
            mockContext,
            mockMusicRepository,
            mockUserPreferencesRepository,
            mockThemePreferencesRepository,
            mockAlbumArtThemeDao,
            mockSyncManager,
            mockDualPlayerEngine,
            mockListeningStatsTracker,
            mockDailyMixStateHolder,
            mockLyricsStateHolder,
            mockQueueStateHolder,
            mockQueueUndoStateHolder,
            mockPlaybackStateHolder,
            mockConnectivityStateHolder,
            mockSleepTimerStateHolder,
            mockSearchStateHolder,
            mockLibraryStateHolder,
            mockFolderNavigationStateHolder,
            mockLibraryTabsStateHolder,
            mockMetadataEditStateHolder,
            mockSongRemovalStateHolder,
            mockExternalMediaStateHolder,
            mockThemeStateHolder,
            mockMultiSelectionStateHolder,
            testDispatchers
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Nested
    @DisplayName("GetSongUrisForGenre Tests")
    inner class GetSongUrisForGenreTests {

        private val song1 = Song(id = "1", title = "Song 1", artist = "Artist A", genre = "Rock", albumArtUriString = "rock_cover1.png", artistId = 1L, albumId = 1L, contentUriString = "content://dummy/1", duration = 180000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")
        private val song2 = Song(id = "2", title = "Song 2", artist = "Artist B", genre = "Pop", albumArtUriString = "pop_cover1.png", artistId = 2L, albumId = 2L, contentUriString = "content://dummy/2", duration = 200000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")
        private val song3 = Song(id = "3", title = "Song 3", artist = "Artist A", genre = "Rock", albumArtUriString = "rock_cover2.png", artistId = 3L, albumId = 3L, contentUriString = "content://dummy/3", duration = 190000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")
        private val song4 = Song(id = "4", title = "Song 4", artist = "Artist C", genre = "Jazz", albumArtUriString = "jazz_cover1.png", artistId = 4L, albumId = 4L, contentUriString = "content://dummy/4", duration = 210000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")
        private val song5 = Song(id = "5", title = "Song 5", artist = "Artist A", genre = "Rock", albumArtUriString = "rock_cover3.png", artistId = 5L, albumId = 5L, contentUriString = "content://dummy/5", duration = 220000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")
        private val song6 = Song(id = "6", title = "Song 6", artist = "Artist A", genre = "Rock", albumArtUriString = "rock_cover4.png", artistId = 6L, albumId = 6L, contentUriString = "content://dummy/6", duration = 230000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")
        private val song7 = Song(id = "7", title = "Song 7", artist = "Artist A", genre = "Rock", albumArtUriString = "rock_cover5.png", artistId = 7L, albumId = 7L, contentUriString = "content://dummy/7", duration = 240000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")
        private val song8 = Song(id = "8", title = "Song 8", artist = "Artist A", genre = "Rock", albumArtUriString = "", artistId = 8L, albumId = 8L, contentUriString = "content://dummy/8", duration = 250000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")
        private val song9 = Song(id = "9", title = "Song 9", artist = "Artist D", genre = null, albumArtUriString = "null_genre_cover.png", artistId = 9L, albumId = 9L, contentUriString = "content://dummy/9", duration = 260000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")

        private fun setupViewModelWithSongs(songs: List<Song>) {
            _allSongsFlow.value = songs.toImmutableList()
            testDispatcher.scheduler.advanceUntilIdle()
            
            val genreSlot = slot<String>()
            every { mockMusicRepository.getMusicByGenre(capture(genreSlot)) } answers {
                val genre = genreSlot.captured
                val filtered = songs.filter { it.genre.equals(genre, ignoreCase = true) }
                flowOf(filtered)
            }
        }

        @Test
        fun `single song with genre returns its cover`() = runTest {
            val testSongs = listOf(song1)
            setupViewModelWithSongs(testSongs)

            val uris = playerViewModel.getSongUrisForGenre("Rock").first()
            assertEquals(listOf("rock_cover1.png"), uris)
        }

        @Test
        fun `multiple songs with same genre return all covers`() = runTest {
            val testSongs = listOf(song1, song3)
            setupViewModelWithSongs(testSongs)

            val uris = playerViewModel.getSongUrisForGenre("Rock").first()
            assertEquals(listOf("rock_cover1.png", "rock_cover2.png"), uris)
        }

        @Test
        fun `no songs with genre returns empty list`() = runTest {
            val testSongs = listOf(song2, song4)
            setupViewModelWithSongs(testSongs)

            val uris = playerViewModel.getSongUrisForGenre("Rock").first()
            assertTrue(uris.isEmpty())
        }
        
        @Test
        fun `genre with more than 3 songs returns first 4 valid covers`() = runTest {
            val testSongs = listOf(song1, song3, song5, song6, song7)
            setupViewModelWithSongs(testSongs)

            val uris = playerViewModel.getSongUrisForGenre("Rock").first()
            assertEquals(4, uris.size)
            assertEquals(listOf("rock_cover1.png", "rock_cover2.png", "rock_cover3.png", "rock_cover4.png"), uris)
        }

        @Test
        fun `songs with blank album art are ignored`() = runTest {
            val testSongs = listOf(song1, song8)
            setupViewModelWithSongs(testSongs)

            val uris = playerViewModel.getSongUrisForGenre("Rock").first()
            assertEquals(listOf("rock_cover1.png"), uris)
        }

        @Test
        fun `empty allSongs list returns empty list for any genre`() = runTest {
            setupViewModelWithSongs(emptyList())

            val uris = playerViewModel.getSongUrisForGenre("Rock").first()
            assertTrue(uris.isEmpty())
        }

        @Test
        fun `case insensitive genre matching`() = runTest {
            val testSongs = listOf(song1, song3)
            setupViewModelWithSongs(testSongs)

            val uris = playerViewModel.getSongUrisForGenre("rOcK").first()
            assertEquals(listOf("rock_cover1.png", "rock_cover2.png"), uris)
        }

        @Test
        fun `genre not found among songs with null genres`() = runTest {
            val testSongs = listOf(song9)
            setupViewModelWithSongs(testSongs)

            val uris = playerViewModel.getSongUrisForGenre("Rock").first()
            assertTrue(uris.isEmpty())
        }
         @Test
        fun `songs with null genre do not match specific genre query`() = runTest {
            val testSongs = listOf(song1, song9)
            setupViewModelWithSongs(testSongs)

            val uris = playerViewModel.getSongUrisForGenre("Rock").first()
            assertEquals(listOf("rock_cover1.png"), uris)
        }
    }

    @Test
    fun `repeat preference changes after startup are not pushed back into controller`() = runTest {
        advanceUntilIdle()
        controllerRepeatModeWrites.clear()

        _repeatModeFlow.value = Player.REPEAT_MODE_ONE
        advanceUntilIdle()

        assertTrue(controllerRepeatModeWrites.isEmpty())
        assertEquals(Player.REPEAT_MODE_OFF, controllerRepeatMode)
    }

    @Test
    fun `startup applies the stored folder song sort without writing it back`() = runTest {
        advanceUntilIdle()

        verify { mockLibraryStateHolder.sortFolderSongs(SortOption.FolderSongTitleAZ, false) }
        assertEquals(SortOption.FolderSongTitleAZ, playerViewModel.playerUiState.value.currentFolderSongSortOption)
    }

    @Test
    fun `folder song sort changes reach the ui state without touching the folder sort`() = runTest {
        advanceUntilIdle()

        folderSongSortFlow.value = SortOption.FolderSongAlbumArtistDesc
        advanceUntilIdle()

        val state = playerViewModel.playerUiState.value
        assertEquals(SortOption.FolderSongAlbumArtistDesc, state.currentFolderSongSortOption)
        assertEquals(SortOption.FolderNameAZ, state.currentFolderSortOption)
    }

    @Test
    fun `sorting folder songs is persisted through the library state holder`() = runTest {
        playerViewModel.sortFolderSongs(SortOption.FolderSongDuration)

        verify { mockLibraryStateHolder.sortFolderSongs(SortOption.FolderSongDuration, true) }
    }

    @Test
    fun `album navigation from player accepts synthetic negative album ids`() = runTest {
        playerViewModel.albumNavigationRequests.test {
            playerViewModel.triggerAlbumNavigationFromPlayer(-42L)
            advanceUntilIdle()

            assertEquals(-42L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `album navigation from player still ignores sentinel album id`() = runTest {
        playerViewModel.albumNavigationRequests.test {
            playerViewModel.triggerAlbumNavigationFromPlayer(-1L)
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Nested
    @DisplayName("Shuffle Functionality")
    inner class ShuffleFunctionalityTests {

        private val song1 = Song(id = "1", title = "Song 1", artist = "Artist A", genre = "Rock", albumArtUriString = "cover1.png", artistId = 1L, albumId = 1L, contentUriString = "content://dummy/1", duration = 180000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")
        private val song2 = Song(id = "2", title = "Song 2", artist = "Artist B", genre = "Pop", albumArtUriString = "cover2.png", artistId = 2L, albumId = 2L, contentUriString = "content://dummy/2", duration = 200000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")
        private val song3 = Song(id = "3", title = "Song 3", artist = "Artist C", genre = "Jazz", albumArtUriString = "cover3.png", artistId = 3L, albumId = 3L, contentUriString = "content://dummy/3", duration = 210000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")

        private fun stubShuffledPlayback(
            songs: List<Song>,
            startSong: Song = songs.first()
        ) {
            coEvery {
                mockQueueStateHolder.prepareShuffledQueueSuspending(songs, true)
            } returns Pair(songs, startSong)

            mockkObject(MediaItemBuilder)
            every { MediaItemBuilder.build(any()) } returns MediaItem.Builder()
                .setMediaId("test")
                .setUri("file:///tmp/test.mp3")
                .build()
            val mockedPlaybackUri = mockk<android.net.Uri>(relaxed = true)
            every { mockedPlaybackUri.scheme } returns "file"
            every { MediaItemBuilder.playbackUri(any<Song>()) } returns mockedPlaybackUri

            mockkStatic(android.net.Uri::class)
            every { android.net.Uri.parse(any()) } returns mockk(relaxed = true)
        }

        @Test
        fun `shuffleAllSongs calls prepareShuffledQueue with random songs at index zero`() = runTest {
            val randomSongs = listOf(song2, song3, song1)
            coEvery { mockMusicRepository.getRandomSongs(10_000) } returns randomSongs
            stubShuffledPlayback(randomSongs, startSong = song2)

            playerViewModel.shuffleAllSongs()
            advanceUntilIdle()

            coVerify { mockMusicRepository.getRandomSongs(10_000) }
            coVerify {
                mockQueueStateHolder.prepareShuffledQueueSuspending(
                    randomSongs,
                    true
                )
            }
        }

        @Test
        fun `playRandomSong calls prepareShuffledQueue with startAtZero`() = runTest {
            val randomSongs = listOf(song3, song1, song2)
            coEvery { mockMusicRepository.getRandomSongs(10_000) } returns randomSongs
            stubShuffledPlayback(randomSongs, startSong = song3)

            playerViewModel.playRandomSong()
            advanceUntilIdle()

            coVerify {
                mockQueueStateHolder.prepareShuffledQueueSuspending(
                    randomSongs,
                    true
                )
            }
        }

        @Test
        fun `playRandomAlbum plays the album in track order without shuffling`() = runTest {
            val album = Album(
                id = 77L,
                title = "Album Roulette",
                artist = "Artist A",
                year = 2024,
                dateAdded = 0L,
                albumArtUriString = null,
                songCount = 3
            )
            val track1 = song3.copy(trackNumber = 1)
            val track2 = song1.copy(trackNumber = 2)
            val track3 = song2.copy(trackNumber = 3)
            every { mockLibraryStateHolder.albums } returns MutableStateFlow(persistentListOf(album))
            every { mockMusicRepository.getSongsForAlbum(album.id) } returns flowOf(listOf(track2, track3, track1))
            stubShuffledPlayback(listOf(track1, track2, track3))

            playerViewModel.playRandomAlbum()
            advanceUntilIdle()

            verify {
                mockQueueStateHolder.setOriginalQueueOrder(listOf(track1, track2, track3))
            }
            coVerify(exactly = 0) { mockQueueStateHolder.prepareShuffledQueueSuspending(any(), any()) }
        }

        @Test
        fun `shuffleRandomArtist calls prepareShuffledQueue with startAtZero`() = runTest {
            val artist = Artist(id = 88L, name = "Artist Roulette", songCount = 3)
            every { mockLibraryStateHolder.artists } returns MutableStateFlow(persistentListOf(artist))
            every { mockMusicRepository.getSongsForArtist(artist.id) } returns flowOf(listOf(song3, song2, song1))
            stubShuffledPlayback(listOf(song3, song2, song1), startSong = song3)

            playerViewModel.shuffleRandomArtist()
            advanceUntilIdle()

            coVerify {
                mockQueueStateHolder.prepareShuffledQueueSuspending(
                    listOf(song3, song2, song1),
                    true
                )
            }
        }

        @Test
        fun `shuffling all songs names the queue in the app language`() = runTest {
            val randomSongs = listOf(song2, song3, song1)
            coEvery { mockMusicRepository.getRandomSongs(10_000) } returns randomSongs
            stubShuffledPlayback(randomSongs, startSong = song2)
            every { mockContext.getString(R.string.queue_name_all_songs_shuffled) } returns "全部歌曲（随机）"
            advanceUntilIdle()

            playerViewModel.shuffleAllSongs()
            advanceUntilIdle()

            assertEquals("全部歌曲（随机）", playerViewModel.playerUiState.value.currentQueueSourceName)
        }

        @Test
        fun `playing selected songs names the queue in the app language`() = runTest {
            val selectedSongs = listOf(song3, song1)
            stubShuffledPlayback(selectedSongs)
            every { mockContext.getString(R.string.queue_name_selected_songs) } returns "选中的歌曲"
            advanceUntilIdle()

            playerViewModel.playSelectedSongs(selectedSongs)
            advanceUntilIdle()

            assertEquals("选中的歌曲", playerViewModel.playerUiState.value.currentQueueSourceName)
        }

        @Test
        fun `playing a song from the library tab names the queue in the app language`() = runTest {
            val librarySongs = listOf(song1, song2, song3)
            every { mockLibraryStateHolder.allSongsById } returns MutableStateFlow(librarySongs.associateBy { it.id })
            coEvery { mockMusicRepository.getSongIdsSorted(any(), any()) } returns listOf(1L, 2L, 3L)
            stubShuffledPlayback(librarySongs)
            every { mockContext.getString(R.string.tab_library) } returns "音乐库"
            advanceUntilIdle()

            playerViewModel.showAndPlaySongFromLibrary(song2)
            advanceUntilIdle()

            val state = playerViewModel.playerUiState.value

            assertEquals("音乐库", state.currentQueueSourceName)
            assertEquals(librarySongs.map { it.id }, state.currentPlaybackQueue.map { it.id })
        }
    }

    @Nested
    @DisplayName("Locate current song")
    inner class LocateCurrentSongTests {

        private val currentSong = Song(id = "42", title = "Song 42", artist = "Artist", genre = "Rock", albumArtUriString = null, artistId = 1L, albumId = 1L, contentUriString = "content://dummy/42", duration = 180000L, bitrate = null, sampleRate = null, album = "Album", path = "/music/42.mp3", mimeType = "audio/mpeg")

        // The song sits at a different position in each list: 3 among all songs, 1 among favorites.
        private fun stubListOrders() {
            coEvery { mockMusicRepository.getSongIdsSorted(any(), any()) } returns listOf(1L, 2L, 3L, 42L)
            stablePlayerStateFlow.value = StablePlayerState(currentSong = currentSong)
        }

        @Test
        fun `a Songs locate emits the full library index tagged for Songs`() = runTest {
            advanceUntilIdle()
            stubListOrders()
            val songSort = playerViewModel.playerUiState.value.currentSongSortOption

            playerViewModel.scrollToIndexEvent.test {
                playerViewModel.requestLocateCurrentSong(LibraryTabId.SONGS)
                advanceUntilIdle()

                assertEquals(LocateScrollRequest(tab = LibraryTabId.SONGS, index = 3), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            coVerify { mockMusicRepository.getSongIdsSorted(songSort, any()) }
        }
    }

    @Nested
    @DisplayName("Clearing the view model")
    inner class ClearingTests {

        @Test
        fun `clearing does not stop progress updates outside the playback holder's owner check`() = runTest {
            advanceUntilIdle()
            val boundScope = slot<CoroutineScope>()
            verify { mockPlaybackStateHolder.initialize(capture(boundScope)) }
            // Only the calls made while clearing matter; keep the stubs.
            clearMocks(mockPlaybackStateHolder, answers = false)

            PlayerViewModel::class.java.getDeclaredMethod("onCleared")
                .apply { isAccessible = true }
                .invoke(playerViewModel)

            // A stale view model stopping progress here would freeze a newer owner's seek bar.
            verify(exactly = 0) { mockPlaybackStateHolder.stopProgressUpdates() }
            val clearedScope = slot<CoroutineScope>()
            verify(exactly = 1) { mockPlaybackStateHolder.onCleared(capture(clearedScope)) }
            assertSame(boundScope.captured, clearedScope.captured)
        }

        @Test
        fun `clearing releases every shared holder for the scope that bound it`() = runTest {
            advanceUntilIdle()
            val boundScope = slot<CoroutineScope>()
            verify { mockThemeStateHolder.initialize(capture(boundScope)) }
            val owner = boundScope.captured
            verify { mockDailyMixStateHolder.initialize(owner) }
            verify { mockLyricsStateHolder.initialize(owner, any(), any()) }
            verify { mockSearchStateHolder.initialize(owner) }
            verify { mockLibraryStateHolder.initialize(owner) }
            verify { mockSleepTimerStateHolder.initialize(owner, any(), any(), any()) }
            verify { mockConnectivityStateHolder.initialize(owner) }

            PlayerViewModel::class.java.getDeclaredMethod("onCleared")
                .apply { isAccessible = true }
                .invoke(playerViewModel)

            // Each holder falls back to whichever PlayerViewModel is still bound, so a cleared
            // view model has to say which binding it is giving up.
            verify(exactly = 1) { mockPlaybackStateHolder.onCleared(owner) }
            verify(exactly = 1) { mockDailyMixStateHolder.onCleared(owner) }
            verify(exactly = 1) { mockLyricsStateHolder.onCleared(owner) }
            verify(exactly = 1) { mockThemeStateHolder.onCleared(owner) }
            verify(exactly = 1) { mockSearchStateHolder.onCleared(owner) }
            verify(exactly = 1) { mockLibraryStateHolder.onCleared(owner) }
            verify(exactly = 1) { mockSleepTimerStateHolder.onCleared(owner) }
            verify(exactly = 1) { mockConnectivityStateHolder.onCleared(owner) }
        }
    }

    @Nested
    @DisplayName("Restoring interrupted saves")
    inner class InterruptedSaveRestoreTests {

        private val songsAwaitingWriteAccess = MutableStateFlow<List<Uri>>(emptyList())
        private val restoreIntentSender: IntentSender = mockk()
        private val awaitingUris = listOf<Uri>(mockk(relaxed = true), mockk(relaxed = true))

        @BeforeEach
        fun stubRestoreSources() {
            // The view model's init coroutine has not run yet, so it collects this flow.
            every { mockMetadataEditStateHolder.songsAwaitingWriteAccess } returns songsAwaitingWriteAccess
            mockkObject(MediaStorePermissionHelper)
            every {
                MediaStorePermissionHelper.createWriteRequestIntentSender(any(), any())
            } returns restoreIntentSender
        }

        /**
         * Marks a batch genre edit as waiting for its own write request. The public paths that do
         * this (batchEditGenre, editSongMetadata, saveLyricsToFile) only ask for permission when
         * Build.VERSION.SDK_INT >= R, which is 0 on the JVM, so the private field is set directly.
         */
        private fun startOtherWriteRequest() {
            PlayerViewModel::class.java.getDeclaredField("pendingBatchGenreEdit")
                .apply { isAccessible = true }
                .set(playerViewModel, emptyList<Song>() to "Rock")
        }

        @Test
        fun `restore request waits for another pending write request and asks once it is answered`() = runTest {
            playerViewModel.writePermissionRequest.test {
                startOtherWriteRequest()
                songsAwaitingWriteAccess.value = awaitingUris
                advanceUntilIdle()

                // Only one write request can be in flight, so the restore must not ask yet.
                expectNoEvents()
                verify(exactly = 0) { MediaStorePermissionHelper.createWriteRequestIntentSender(any(), any()) }

                playerViewModel.onWritePermissionResult(granted = false)
                advanceUntilIdle()

                verify(exactly = 1) { MediaStorePermissionHelper.createWriteRequestIntentSender(any(), any()) }
                verify(exactly = 1) {
                    MediaStorePermissionHelper.createWriteRequestIntentSender(any(), awaitingUris)
                }
                assertSame(restoreIntentSender, awaitItem())
                expectNoEvents()
            }
        }

        @Test
        fun `restore request is emitted right away when no other write request is pending`() = runTest {
            playerViewModel.writePermissionRequest.test {
                songsAwaitingWriteAccess.value = awaitingUris
                advanceUntilIdle()

                verify(exactly = 1) {
                    MediaStorePermissionHelper.createWriteRequestIntentSender(any(), awaitingUris)
                }
                assertSame(restoreIntentSender, awaitItem())
                expectNoEvents()
            }
        }
    }

    @Nested
    @DisplayName("Palette during skip bursts")
    inner class PaletteSkipBurstTests {

        private val queue = List(5) { index ->
            Song(
                id = "palette-$index",
                title = "Palette Song $index",
                artist = "Artist",
                artistId = 1L,
                album = "Album $index",
                albumId = index + 1L,
                path = "/storage/emulated/0/Music/palette-$index.mp3",
                contentUriString = "content://media/external/audio/media/$index",
                albumArtUriString = "content://media/external/audio/albumart/$index",
                duration = 180_000L,
                mimeType = "audio/mpeg",
                bitrate = null,
                sampleRate = null
            )
        }

        private fun artworkOf(index: Int): String = queue[index].albumArtUriString!!

        @BeforeEach
        fun mockUriParsing() {
            mockkStatic(Uri::class)
            every { Uri.parse(any()) } returns mockk(relaxed = true)
        }

        /** Starts playback of [songs] so the view model's playback queue holds them. */
        private fun TestScope.startPlaybackOf(songs: List<Song>) {
            mockkObject(MediaItemBuilder)
            every { MediaItemBuilder.build(any()) } returns MediaItem.Builder()
                .setMediaId("test")
                .setUri("file:///tmp/test.mp3")
                .build()
            val playbackUri = mockk<Uri>(relaxed = true)
            every { playbackUri.scheme } returns "file"
            every { MediaItemBuilder.playbackUri(any<Song>()) } returns playbackUri
            // Let the controller connection read its (empty) timeline first, or that read lands
            // after playSongs and clears the queue again.
            advanceUntilIdle()

            playerViewModel.playSongs(songsToPlay = songs, startSong = songs.first())
            advanceUntilIdle()
        }

        private fun moveToQueueIndex(index: Int) {
            stablePlayerStateFlow.value = StablePlayerState(currentMediaItemIndex = index)
        }

        @Test
        fun `skipping past a song cancels its unfinished palette extraction`() = runTest {
            var skippedExtractionCancelled = false
            coEvery {
                mockThemeStateHolder.extractAndGenerateColorScheme(any(), artworkOf(0), any())
            } coAnswers {
                try {
                    awaitCancellation()
                } finally {
                    skippedExtractionCancelled = true
                }
            }
            advanceUntilIdle()

            stablePlayerStateFlow.value = StablePlayerState(currentSong = queue[0])
            runCurrent()
            stablePlayerStateFlow.value = StablePlayerState(currentSong = queue[1])
            runCurrent()

            assertTrue(skippedExtractionCancelled)
            coVerify(exactly = 1) {
                mockThemeStateHolder.extractAndGenerateColorScheme(any(), artworkOf(1), false)
            }
        }

        @Test
        fun `neighbour palettes are preloaded only once the queue index holds still`() = runTest {
            startPlaybackOf(queue)

            moveToQueueIndex(2)
            runCurrent()
            advanceTimeBy(PALETTE_NEIGHBOR_PRELOAD_DEBOUNCE_MS - 1)
            runCurrent()

            verify(exactly = 0) { mockThemeStateHolder.ensureAlbumColorScheme(any()) }

            advanceTimeBy(1)
            runCurrent()

            verify(exactly = 1) { mockThemeStateHolder.ensureAlbumColorScheme(artworkOf(1)) }
            verify(exactly = 1) { mockThemeStateHolder.ensureAlbumColorScheme(artworkOf(3)) }
            verify(exactly = 2) { mockThemeStateHolder.ensureAlbumColorScheme(any()) }
        }

        @Test
        fun `a burst of skips only preloads the neighbours of the song it lands on`() = runTest {
            startPlaybackOf(queue)

            listOf(1, 2, 3).forEach { index ->
                moveToQueueIndex(index)
                runCurrent()
                advanceTimeBy(PALETTE_NEIGHBOR_PRELOAD_DEBOUNCE_MS / 2)
            }
            advanceTimeBy(PALETTE_NEIGHBOR_PRELOAD_DEBOUNCE_MS)
            runCurrent()

            verify(exactly = 1) { mockThemeStateHolder.ensureAlbumColorScheme(artworkOf(2)) }
            verify(exactly = 1) { mockThemeStateHolder.ensureAlbumColorScheme(artworkOf(4)) }
            verify(exactly = 2) { mockThemeStateHolder.ensureAlbumColorScheme(any()) }
        }
    }

    @Nested
    @DisplayName("Search History")
    inner class SearchHistoryTests {

        @Test
        fun `test_loadSearchHistory_updatesUiState`() = runTest {
            val historyItems = listOf(SearchHistoryItem(query = "q1", timestamp = 1L))
            every { mockSearchStateHolder.loadSearchHistory(any()) } answers {
                _searchHistoryFlow.value = historyItems.toImmutableList()
            }

            playerViewModel.playerUiState.test {
                val initialState = awaitItem()
                if (initialState.searchHistory == historyItems) {
                    cancelAndConsumeRemainingEvents()
                    return@test
                }

                playerViewModel.loadSearchHistory()
                
                val currentItem = awaitItem()
                assertEquals(historyItems, currentItem.searchHistory)
                cancelAndConsumeRemainingEvents()
            }
        }


        @Test
        fun `test_clearSearchHistory_callsRepository_andUpdatesUiState`() = runTest {
            val initialHistory = listOf(SearchHistoryItem(query = "q1", timestamp = 1L))
            _searchHistoryFlow.value = initialHistory.toImmutableList()
            
            every { mockSearchStateHolder.clearSearchHistory() } answers {
                _searchHistoryFlow.value = persistentListOf()
            }

            playerViewModel.playerUiState.test {
               var state = awaitItem()
               while (state.searchHistory != initialHistory) {
                   state = awaitItem()
               }

                playerViewModel.clearSearchHistory()

                var emitted = awaitItem()
                while (emitted.searchHistory.isNotEmpty()) {
                    emitted = awaitItem()
                }
                assertTrue(emitted.searchHistory.isEmpty())
                verify { mockSearchStateHolder.clearSearchHistory() }
                cancelAndConsumeRemainingEvents()
            }
        }

        @Test
        fun `test_deleteSearchHistoryItem_callsRepository_andRefreshesHistory`() = runTest {
            val queryToDelete = "delete me"
            val keepQuery = "keep me"
            val initialHistory = listOf(SearchHistoryItem(query = queryToDelete, timestamp = 1L), SearchHistoryItem(query = keepQuery, timestamp = 2L))
            val finalHistory = listOf(SearchHistoryItem(query = keepQuery, timestamp = 2L))

            _searchHistoryFlow.value = initialHistory.toImmutableList()
            
            every { mockSearchStateHolder.deleteSearchHistoryItem(queryToDelete) } answers {
                _searchHistoryFlow.value = finalHistory.toImmutableList()
            }

            playerViewModel.playerUiState.test {
               var state = awaitItem()
               while (state.searchHistory != initialHistory) {
                   state = awaitItem()
               }

                playerViewModel.deleteSearchHistoryItem(queryToDelete)

                var emitted = awaitItem()
                while (emitted.searchHistory != finalHistory) {
                    emitted = awaitItem()
                }
                assertEquals(finalHistory, emitted.searchHistory)
                
                cancelAndConsumeRemainingEvents()
            }
        }
        
        @Test
        fun `test_updateSearchFilter_updatesUiState`() = runTest {
             val newFilter = SearchFilterType.ARTISTS
             
             coEvery { mockSearchStateHolder.updateSearchFilter(newFilter) } answers {
                 _selectedSearchFilterFlow.value = newFilter
             }

            playerViewModel.playerUiState.test {
                skipItems(1)

                playerViewModel.updateSearchFilter(newFilter)
                
                val emittedItem = awaitItem()
                assertEquals(newFilter, emittedItem.selectedSearchFilter)
                cancelAndConsumeRemainingEvents()
            }
        }
    }

    private fun PlayerViewModel.updateAllSongs(songs: List<Song>) {
        _allSongsFlow.value = songs.toImmutableList()
    }
}
