package io.github.jayfunc.carpelines.presentation.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.external.ExternalPlaybackStore
import io.github.jayfunc.carpelines.ui.theme.LocalReducedMotion
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The players this device has really played music with — a small, living cluster of logos rather
 * than a catalogue of "supported" apps.
 *
 * It is deliberately not a headline section: it sits under the hero card as a quiet signature of
 * the user's own listening. Nothing here is clickable; CarpeLines follows playback, it does not
 * launch other apps.
 */
@Composable
fun PlayedPlayersRow(
    players: List<ExternalPlaybackStore.PlayerRecord>,
    playedTrackCount: Int,
    modifier: Modifier = Modifier,
) {
    if (players.isEmpty()) return

    val context = LocalContext.current
    val icons by produceState(
        initialValue = emptyMap<String, Drawable?>(),
        key1 = players.map { it.packageName }
    ) {
        value = withContext(Dispatchers.IO) {
            players.associate { it.packageName to loadIcon(context, it.packageName) }
        }
    }

    Column(
        modifier = modifier.padding(
            top = M3ESpacing.small,
            start = M3ESpacing.extraLarge,
            end = M3ESpacing.extraLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(M3ESpacing.small)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(M3ESpacing.small),
        ) {
            Text(
                text = stringResource(R.string.home_played_players_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.home_played_players_count, playedTrackCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
            players.forEachIndexed { index, player ->
                PlayerBadge(
                    player = player,
                    icon = icons[player.packageName],
                    index = index,
                )
            }
        }
    }
}

@Composable
private fun PlayerBadge(
    player: ExternalPlaybackStore.PlayerRecord,
    icon: Drawable?,
    index: Int,
) {
    // Staggered entrance: the cluster assembles itself instead of appearing as a static row.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(player.packageName) {
        delay(index * 55L)
        visible = true
    }
    val entrance = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 380, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "playerBadgeEntrance",
    )

    // A slow, per-badge drift keeps the cluster from looking like a row of stickers. It is
    // decoration, so it is skipped entirely when the user asked for reduced motion.
    val reducedMotion = LocalReducedMotion.current
    val drift = rememberInfiniteTransition(label = "playerBadgeDrift")
    val animatedPhase by drift.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_600 + index * 240, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "playerBadgePhase",
    )
    val phase = if (reducedMotion) 0f else animatedPhase

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                val eased = entrance.value
                alpha = eased
                scaleX = 0.7f + 0.3f * eased
                scaleY = 0.7f + 0.3f * eased
                translationY = kotlin.math.sin(phase) * 2.5f * density
            }
            // The ring is the page colour, so overlapping logos read as separate objects.
            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(
                painter = rememberDrawablePainter(drawable = icon),
                contentDescription = player.appLabel,
                modifier = Modifier
                    .padding(7.dp)
                    .size(24.dp),
            )
        } else {
            Text(
                text = player.appLabel.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun loadIcon(context: Context, packageName: String): Drawable? = runCatching {
    val info = context.packageManager.getApplicationInfo(packageName, 0)
    context.packageManager.getApplicationIcon(info)
}.getOrNull()
