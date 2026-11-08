package io.github.jayfunc.carpelines.ui.glancewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import io.github.jayfunc.carpelines.R

fun openLyricsAction(): Action = actionRunCallback<OpenLyricsActionCallback>()

@Composable
fun WidgetOpenChevron(color: ColorProvider) {
    val context = LocalContext.current
    Image(
        provider = ImageProvider(R.drawable.rounded_chevron_right_24),
        contentDescription = context.getString(R.string.widget_tap_to_open),
        modifier = GlanceModifier.size(24.dp),
        colorFilter = ColorFilter.tint(color),
    )
}

@Composable
fun WidgetArtwork(
    artworkUri: String?,
    size: Dp,
    cornerRadius: Dp,
    modifier: GlanceModifier = GlanceModifier,
    fallbackBackground: ColorProvider? = null,
    fallbackIconColor: ColorProvider? = null,
) {
    val context = LocalContext.current
    val imageProvider = artworkUri?.let { uri ->
        val cacheKey = "uri:$uri"
        var bitmap = AlbumArtBitmapCache.getBitmap(cacheKey)
        if (bitmap == null) {
            bitmap = decodeWidgetAlbumArtBitmap(
                context = context,
                rawUri = uri,
                targetWidthPx = size.toPx(context),
                targetHeightPx = size.toPx(context),
            )
            bitmap?.let { AlbumArtBitmapCache.putBitmap(cacheKey, it) }
        }
        bitmap?.let { ImageProvider(it) }
    }

    Box(modifier = modifier) {
        if (imageProvider != null) {
            Image(
                provider = imageProvider,
                contentDescription = context.getString(R.string.widget_album_art),
                modifier = GlanceModifier.fillMaxSize().cornerRadius(cornerRadius),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(cornerRadius)
                    .background(fallbackBackground ?: GlanceTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_music_placeholder),
                    contentDescription = context.getString(R.string.widget_album_art_placeholder),
                    modifier = GlanceModifier.size(size * 0.55f),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(
                        fallbackIconColor ?: GlanceTheme.colors.onSurfaceVariant
                    ),
                )
            }
        }
    }
}

private fun Dp.toPx(context: Context): Int =
    (value * context.resources.displayMetrics.density).toInt().coerceAtLeast(64)

@Composable
fun WidgetIconButton(
    modifier: GlanceModifier,
    action: Action,
    backgroundColor: ColorProvider,
    iconColor: ColorProvider,
    imageProvider: ImageProvider,
    contentDescription: String,
    iconSize: Dp = 20.dp,
    cornerRadius: Dp,
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .cornerRadius(cornerRadius)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = imageProvider,
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(iconColor),
        )
    }
}

@Composable
fun PreviousButton(
    modifier: GlanceModifier,
    backgroundColor: ColorProvider,
    iconColor: ColorProvider,
    cornerRadius: Dp,
    iconSize: Dp = 20.dp,
) {
    val context = LocalContext.current
    WidgetIconButton(
        modifier = modifier,
        action = actionRunCallback<PlayerControlActionCallback>(
            actionParametersOf(PlayerActions.key to PlayerActions.PREVIOUS)
        ),
        backgroundColor = backgroundColor,
        iconColor = iconColor,
        imageProvider = ImageProvider(R.drawable.rounded_skip_previous_filled_24),
        contentDescription = context.getString(R.string.previous_track),
        iconSize = iconSize,
        cornerRadius = cornerRadius,
    )
}

@Composable
fun NextButton(
    modifier: GlanceModifier,
    backgroundColor: ColorProvider,
    iconColor: ColorProvider,
    cornerRadius: Dp,
    iconSize: Dp = 20.dp,
) {
    val context = LocalContext.current
    WidgetIconButton(
        modifier = modifier,
        action = actionRunCallback<PlayerControlActionCallback>(
            actionParametersOf(PlayerActions.key to PlayerActions.NEXT)
        ),
        backgroundColor = backgroundColor,
        iconColor = iconColor,
        imageProvider = ImageProvider(R.drawable.rounded_skip_next_filled_24),
        contentDescription = context.getString(R.string.next_track),
        iconSize = iconSize,
        cornerRadius = cornerRadius,
    )
}

@Composable
fun PlayPauseButton(
    modifier: GlanceModifier,
    isPlaying: Boolean,
    backgroundColor: ColorProvider,
    iconColor: ColorProvider,
    cornerRadius: Dp,
    iconSize: Dp = 22.dp,
) {
    val context = LocalContext.current
    WidgetIconButton(
        modifier = modifier,
        action = actionRunCallback<PlayerControlActionCallback>(
            actionParametersOf(PlayerActions.key to PlayerActions.PLAY_PAUSE)
        ),
        backgroundColor = backgroundColor,
        iconColor = iconColor,
        imageProvider = ImageProvider(
            if (isPlaying) R.drawable.rounded_pause_filled_24
            else R.drawable.rounded_play_arrow_filled_24
        ),
        contentDescription = context.getString(
            if (isPlaying) R.string.cd_pause else R.string.cd_play
        ),
        iconSize = iconSize,
        cornerRadius = cornerRadius,
    )
}
