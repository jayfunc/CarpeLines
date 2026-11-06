package io.github.jayfunc.carpelines.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.model.Song
import io.github.jayfunc.carpelines.data.preferences.CollagePattern
import io.github.jayfunc.carpelines.ui.theme.M3EMotion
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Stable
data class Config(val size: Dp, val width: Dp, val height: Dp, val align: Alignment, val rot: Float, val shape: Shape, val offsetX: Dp, val offsetY: Dp)

/**
 * Displays up to 6 covers in a collage layout with simplified, rounded shapes.
 * The shapes are split into two groups (top and bottom) to avoid overlap.
 * Includes a central pill, circle, squircle, and star, with adjusted arrangement.
 * Tweaks sizes, rotations, and positions to create a dynamic look.
 * Uses BoxWithConstraints to adapt the dimensions to the container.
 */
@Composable
fun AlbumArtCollage(
    songs: ImmutableList<Song>,
    modifier: Modifier = Modifier,
    height: Dp = 400.dp,
    padding: Dp = 0.dp,
    pattern: CollagePattern = CollagePattern.default,
    onSongClick: ((Song) -> Unit)? = null,
) {
    val context = LocalContext.current
    val songsToShow = remember(songs) {
        (songs.take(6) + List(6 - songs.size.coerceAtMost(6)) { null }).toImmutableList()
    }

    val requests = remember(songsToShow) {
        songsToShow.map { song ->
            song?.albumArtUriString?.let {
                ImageRequest.Builder(context)
                    .data(it)
                    .dispatcher(Dispatchers.IO)
                    .crossfade(true)
                    .error(R.drawable.ic_music_placeholder)
                    .build()
            }
        }.toImmutableList()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(padding)
    ) {
        val collageMin = minOf(300.dp, maxHeight)
        // Shapes are polygons and paths, so build every arrangement once off the main thread
        // instead of stalling the frame that starts the crossfade.
        val configsByPattern by produceState<Map<CollagePattern, List<Config>>>(
            initialValue = emptyMap(),
            collageMin,
            maxHeight,
        ) {
            value = withContext(Dispatchers.Default) {
                CollagePattern.entries.associateWith { entry ->
                    buildCollageConfigs(entry, collageMin, maxHeight)
                }
            }
        }

        // Each pattern is one picture. Changing arrangements cross-dissolves the two pictures
        // instead of flying every tile to a new place, which read as noisy rather than calm.
        Crossfade(
            targetState = pattern,
            animationSpec = tween(
                durationMillis = M3EMotion.Duration.medium3,
                easing = M3EMotion.Easings.standard,
            ),
            label = "collagePattern",
            modifier = Modifier.fillMaxSize(),
        ) { activePattern ->
            CollageLayout(
                songsToShow = songsToShow,
                requests = requests,
                configs = configsByPattern[activePattern].orEmpty(),
                boxMaxHeight = maxHeight,
                onSongClick = onSongClick,
            )
        }

        if (songs.isEmpty()) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.rounded_music_note_24),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/** Where one cover sits in the collage, in container coordinates. */
private data class CollageTarget(
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp,
    val rotation: Float,
    val shape: Shape,
)

/** One static arrangement of the covers; the pattern change itself crossfades above this. */
@Composable
private fun CollageLayout(
    songsToShow: ImmutableList<Song?>,
    requests: ImmutableList<ImageRequest?>,
    configs: List<Config>,
    boxMaxHeight: Dp,
    onSongClick: ((Song) -> Unit)?,
) {
    if (configs.isEmpty()) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val containerWidthPx = constraints.maxWidth
        val boxHeightPx = with(density) { boxMaxHeight.roundToPx() }
        val targets = remember(configs, containerWidthPx, boxHeightPx, density) {
            buildCollageTargets(configs, containerWidthPx, boxHeightPx, density)
        }

        songsToShow.forEachIndexed { index, song ->
            if (song == null) return@forEachIndexed
            val target = targets.getOrNull(index) ?: return@forEachIndexed
            Box(
                modifier = Modifier
                    .offset(x = target.x, y = target.y)
                    .size(width = target.width, height = target.height)
                    .graphicsLayer { rotationZ = target.rotation }
                    .then(
                        if (onSongClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSongClick(song) }
                        } else {
                            Modifier
                        }
                    ),
            ) {
                SmartImage(
                    model = requests.getOrNull(index),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    shape = target.shape,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(shape = target.shape, color = MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        }
    }
}

private fun buildCollageTargets(
    configs: List<Config>,
    containerWidthPx: Int,
    boxHeightPx: Int,
    density: Density,
): List<CollageTarget> = configs.mapIndexed { index, config ->
    // The first three tiles live in the upper 60% band, the rest in the lower 40%, exactly as the
    // split layout did.
    val topBand = index < 3
    val bandTopPx = if (topBand) 0 else (boxHeightPx * 0.6f).roundToInt()
    val bandHeightPx = (boxHeightPx * if (topBand) 0.6f else 0.4f).roundToInt()
    val widthPx = with(density) { config.width.roundToPx() }
    val heightPx = with(density) { config.height.roundToPx() }
    val aligned = config.align.align(
        size = IntSize(widthPx, heightPx),
        space = IntSize(containerWidthPx, bandHeightPx),
        layoutDirection = LayoutDirection.Ltr,
    )
    val xPx = aligned.x + with(density) { config.offsetX.roundToPx() }
    val yPx = bandTopPx + aligned.y + with(density) { config.offsetY.roundToPx() }
    CollageTarget(
        x = with(density) { xPx.toDp() },
        y = with(density) { yPx.toDp() },
        width = config.width,
        height = config.height,
        rotation = config.rot,
        shape = config.shape,
    )
}
