package io.github.jayfunc.carpelines.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor
import io.github.jayfunc.carpelines.data.external.ExternalMediaState
import io.github.jayfunc.carpelines.data.model.Song
import io.github.jayfunc.carpelines.data.preferences.CollagePattern
import io.github.jayfunc.carpelines.presentation.components.AlbumArtCollage
import io.github.jayfunc.carpelines.presentation.components.MiniPlayerHeight
import io.github.jayfunc.carpelines.presentation.components.PlayedPlayersRow
import io.github.jayfunc.carpelines.presentation.components.RecentlyPlayedSection
import io.github.jayfunc.carpelines.presentation.components.RecentlyPlayedSectionMinSongsToShow
import io.github.jayfunc.carpelines.presentation.components.SmartImage
import io.github.jayfunc.carpelines.presentation.components.StatsOverviewCard
import io.github.jayfunc.carpelines.presentation.model.collectRecentlyPlayedSongIds
import io.github.jayfunc.carpelines.presentation.model.mapRecentlyPlayedSongs
import io.github.jayfunc.carpelines.presentation.navigation.Screen
import io.github.jayfunc.carpelines.presentation.navigation.navigateSafely
import io.github.jayfunc.carpelines.presentation.navigation.navigateToTopLevelSafely
import io.github.jayfunc.carpelines.presentation.recap.RecapHomeCard
import io.github.jayfunc.carpelines.presentation.viewmodel.PlayedPlayersViewModel
import io.github.jayfunc.carpelines.presentation.viewmodel.PlayerViewModel
import io.github.jayfunc.carpelines.presentation.viewmodel.RecapEntryViewModel
import io.github.jayfunc.carpelines.presentation.viewmodel.StatsViewModel
import io.github.jayfunc.carpelines.presentation.viewmodel.sharedPlayerViewModel
import io.github.jayfunc.carpelines.ui.m3e.M3EContentFrame
import io.github.jayfunc.carpelines.ui.theme.LocalReducedMotion
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

/**
 * Home is a window on what the phone is playing and what it played before.
 *
 * The hero card follows the *system* media session, which is the only playback CarpeLines has: it
 * used to read the in-app player and therefore sat on "Ready" while music was clearly playing.
 * The rest of the page is preview-only — the collage and the recently played pills do not start
 * playback — but tapping the hero opens the lyrics page, because that is what the card is about.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    paddingValuesParent: PaddingValues,
    playerViewModel: PlayerViewModel = sharedPlayerViewModel(),
    playedPlayersViewModel: PlayedPlayersViewModel = hiltViewModel(),
) {
    val statsViewModel: StatsViewModel = hiltViewModel()
    val recapEntryViewModel: RecapEntryViewModel = hiltViewModel()

    val curatedMixSongs by playerViewModel.yourMixSongs.collectAsStateWithLifecycle()
    val dailyMixSongs by playerViewModel.dailyMixSongs.collectAsStateWithLifecycle()
    val homeMixPreviewSongs by playerViewModel.homeMixPreviewSongs.collectAsStateWithLifecycle()
    val playbackHistory by playerViewModel.playbackHistory.collectAsStateWithLifecycle()

    val externalMedia by ExternalMediaMonitor.state.collectAsStateWithLifecycle()
    val playedPlayers by playedPlayersViewModel.players.collectAsStateWithLifecycle()
    val playedTrackCount by playedPlayersViewModel.playedTrackCount.collectAsStateWithLifecycle()

    val recentSongIds = remember(playbackHistory) {
        collectRecentlyPlayedSongIds(playbackHistory = playbackHistory, maxItems = 64)
    }
    val recentSourceSongs by remember(recentSongIds, playerViewModel) {
        playerViewModel.observeSongs(recentSongIds)
            .map<List<Song>, List<Song>?> { it }
    }.collectAsStateWithLifecycle(
        initialValue = if (recentSongIds.isEmpty()) persistentListOf<Song>() else null
    )
    val recentlyPlayedSongs = remember(playbackHistory, recentSourceSongs) {
        val sourceSongs = recentSourceSongs ?: return@remember emptyList()
        mapRecentlyPlayedSongs(
            playbackHistory = playbackHistory,
            songs = sourceSongs,
            maxItems = 64
        )
    }

    // Album covers follow what was actually played, then fill up with the daily mix so a short
    // history still gives the collage a full set. Deduplicated by cover, not by song: six tracks
    // from one album should read as one tile.
    val collagePool = remember(
        recentlyPlayedSongs,
        curatedMixSongs,
        dailyMixSongs,
        homeMixPreviewSongs,
    ) {
        buildList {
            addAll(recentlyPlayedSongs.map { it.song })
            addAll(curatedMixSongs)
            addAll(dailyMixSongs)
            addAll(homeMixPreviewSongs)
        }.distinctBy { it.collageAlbumKey() }
    }
    val collagePoolKey = remember(collagePool) {
        collagePool.joinToString(separator = "|") { it.id }
    }

    val currentSong by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong }
    }.collectAsStateWithLifecycle(initialValue = null)
    val homeStatsOverviewState by statsViewModel.homeOverviewState.collectAsStateWithLifecycle()
    val recapHomeCard by recapEntryViewModel.homeCard.collectAsStateWithLifecycle()

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // The collage no longer rotates on tap. Entering Home is the trigger now: the first visit keeps
    // the default arrangement and every return crossfades to the next one, so the page looks
    // different without asking the user for a gesture. Both indices are saveable, so the rotation
    // carries on from where it stopped instead of restarting whenever the state is restored.
    val collagePatterns = remember { CollagePattern.entries }
    var collagePatternIndex by rememberSaveable { mutableIntStateOf(0) }
    var collageCoverOffset by rememberSaveable(collagePoolKey) { mutableIntStateOf(0) }
    var collageHasBeenShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (collageHasBeenShown) {
            collagePatternIndex = (collagePatternIndex + 1) % collagePatterns.size
            collageCoverOffset = nextCollageOffset(collagePool.size, collageCoverOffset)
        } else {
            collageHasBeenShown = true
        }
    }
    val collagePattern = collagePatterns[collagePatternIndex % collagePatterns.size]
    val collageSongs = remember(collagePool, collageCoverOffset) {
        collageWindow(collagePool, collageCoverOffset).toImmutableList()
    }

    val bottomPadding = if (currentSong != null) MiniPlayerHeight else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            M3EContentFrame {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    // The floating navigation pill is drawn outside the scaffold's content, so the
                    // list reserves its height plus one section gap.
                    bottom = paddingValuesParent.calculateBottomPadding() +
                        M3ESpacing.extraExtraLarge + bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(M3ESpacing.extraLarge)
            ) {
                item(key = "home_header", contentType = "home_header") {
                    HomeHeaderCard(
                        externalMedia = externalMedia,
                        song = currentSong,
                        onOpenLyrics = {
                            navController.navigateToTopLevelSafely(Screen.NowPlayingLyrics.route)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = M3ESpacing.extraLarge,
                                top = M3ESpacing.medium,
                                end = M3ESpacing.extraLarge,
                            )
                            .animateItem()
                    )
                }

                if (playedPlayers.isNotEmpty()) {
                    item(key = "played_players", contentType = "played_players") {
                        PlayedPlayersRow(
                            players = playedPlayers,
                            playedTrackCount = playedTrackCount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                        )
                    }
                }

                if (collagePool.isNotEmpty()) {
                    item(key = "album_art_collage", contentType = "album_art_collage") {
                        AlbumArtCollage(
                            modifier = Modifier.fillMaxWidth(),
                            songs = collageSongs,
                            padding = M3ESpacing.medium,
                            height = 360.dp,
                            pattern = collagePattern
                        )
                    }
                }

                recapHomeCard?.let { entry ->
                    item(key = "recap_home_card", contentType = "recap_home_card") {
                        RecapHomeCard(
                            entry = entry,
                            onOpen = {
                                navController.navigateSafely(Screen.Recap.createRoute(entry.year))
                            },
                            onDismiss = { recapEntryViewModel.dismissHomeCard(entry.year) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                if (recentlyPlayedSongs.size >= RecentlyPlayedSectionMinSongsToShow) {
                    item(key = "recently_played_section", contentType = "recently_played_section") {
                        RecentlyPlayedSection(
                            songs = remember(recentlyPlayedSongs) {
                                recentlyPlayedSongs.toImmutableList()
                            },
                            themeStateHolder = playerViewModel.themeStateHolder,
                            currentSongId = currentSong?.id,
                            contentPadding = PaddingValues(start = 8.dp, end = 24.dp)
                        )
                    }
                }

                item(key = "listening_stats_preview", contentType = "listening_stats_preview") {
                    StatsOverviewCard(
                        state = homeStatsOverviewState,
                        onClick = { navController.navigateToTopLevelSafely(Screen.Stats.route) }
                    )
                }
            }
            }
        }
    }
}

/**
 * Hero card: the current system media session, or a quiet introduction when nothing is playing.
 *
 * The two states crossfade rather than swapping, so the card never blinks when a player connects
 * or the user pauses.
 */
@Composable
private fun HomeHeaderCard(
    externalMedia: ExternalMediaState?,
    song: Song?,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playing = externalMedia?.hasMetadata == true || song != null

    AnimatedContent(
        targetState = playing,
        transitionSpec = {
            (fadeIn(tween(320, easing = FastOutSlowInEasing)) +
                scaleIn(tween(320, easing = FastOutSlowInEasing), initialScale = 0.97f))
                .togetherWith(fadeOut(tween(140)))
        },
        label = "homeHeroState",
        modifier = modifier,
    ) { hasTrack ->
        if (hasTrack) {
            val title = externalMedia?.title?.takeIf { it.isNotBlank() } ?: song?.title.orEmpty()
            val artist = externalMedia?.artist?.takeIf { it.isNotBlank() } ?: song?.displayArtist.orEmpty()
            val album = externalMedia?.album?.takeIf { it.isNotBlank() } ?: song?.album.orEmpty()
            val artwork = externalMedia?.artworkBitmap
                ?: externalMedia?.artworkUri
                ?: song?.albumArtUriString

            val isPlaying = externalMedia?.isPlaying == true
            val hasExternal = externalMedia?.hasMetadata == true
            val appLabel = externalMedia?.appLabel?.takeIf { it.isNotBlank() }
            // "… · Now playing" must not survive a pause: the eyebrow follows the session's
            // playback state, not merely the presence of metadata.
            val eyebrow = when {
                !hasExternal -> stringResource(R.string.home_now_playing_eyebrow)
                isPlaying -> appLabel?.let {
                    stringResource(R.string.home_now_playing_in_app, it)
                } ?: stringResource(R.string.home_now_playing_eyebrow)
                else -> appLabel?.let {
                    stringResource(R.string.home_paused_in_app, it)
                } ?: stringResource(R.string.home_paused_eyebrow)
            }

            HeroCard(
                eyebrow = eyebrow,
                title = title,
                subtitle = listOfNotNull(
                    artist.takeIf { it.isNotBlank() },
                    album.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                artworkModel = artwork,
                isPlaying = isPlaying,
                hasTrack = true,
                onClick = onOpenLyrics,
            )
        } else {
            HeroCard(
                eyebrow = stringResource(R.string.home_hero_title),
                title = stringResource(R.string.home_idle_title),
                subtitle = stringResource(R.string.home_idle_subtitle),
                artworkModel = null,
                isPlaying = false,
                hasTrack = false,
                onClick = onOpenLyrics,
            )
        }
    }
}

@Composable
private fun HeroCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    artworkModel: Any?,
    isPlaying: Boolean,
    hasTrack: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = M3EShape.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(M3ESpacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = M3EShape.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(72.dp)
            ) {
                if (artworkModel != null) {
                    SmartImage(
                        model = artworkModel,
                        contentDescription = null,
                        shape = M3EShape.large,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    IdleArtwork()
                }
            }
            Spacer(Modifier.width(M3ESpacing.medium))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(M3ESpacing.extraSmall)
            ) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            when {
                isPlaying -> {
                    Spacer(Modifier.width(M3ESpacing.small))
                    PlayingBars()
                }
                hasTrack -> {
                    Spacer(Modifier.width(M3ESpacing.small))
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** The placeholder cover, breathing gently so the empty state does not read as a dead card. */
@Composable
private fun IdleArtwork() {
    val transition = rememberInfiniteTransition(label = "idleArtwork")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idleArtworkScale",
    )
    // The breathing is pure decoration, so it stops at a still frame when motion is reduced.
    val artworkScale = if (LocalReducedMotion.current) 1f else scale
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            modifier = Modifier
                .size(30.dp)
                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                },
        )
    }
}

/** Three bars that rise and fall while a session reports itself as playing. */
@Composable
private fun PlayingBars() {
    val transition = rememberInfiniteTransition(label = "playingBars")
    val weights = listOf(0, 1, 2).map { index ->
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 520 + index * 180,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "playingBar$index",
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(24.dp),
    ) {
        val reducedMotion = LocalReducedMotion.current
        weights.forEach { weight ->
            // Freeze the bars at different heights instead of animating them when motion is off,
            // so the "playing" state is still legible as a static glyph.
            val barFraction = if (reducedMotion) 0.6f else weight.value
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(barFraction)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

/** Covers are deduplicated by artwork so one album does not fill several collage tiles. */
private fun Song.collageAlbumKey(): String =
    albumArtUriString?.takeIf { it.isNotBlank() } ?: "${album.orEmpty()}|$title"

/** Up to [size] covers starting at [offset], wrapping around so every pool entry can appear. */
private fun collageWindow(pool: List<Song>, offset: Int, size: Int = COLLAGE_SIZE): List<Song> {
    if (pool.size <= size) return pool
    val start = offset.mod(pool.size)
    return List(size) { pool[(start + it) % pool.size] }
}

private fun nextCollageOffset(poolSize: Int, currentOffset: Int): Int =
    if (poolSize <= COLLAGE_SIZE) 0 else (currentOffset + COLLAGE_SIZE) % poolSize

private const val COLLAGE_SIZE = 6
