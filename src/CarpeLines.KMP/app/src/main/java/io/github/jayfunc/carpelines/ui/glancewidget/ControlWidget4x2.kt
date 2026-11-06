package io.github.jayfunc.carpelines.ui.glancewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.jayfunc.carpelines.R
import kotlin.math.roundToLong

class ControlWidget4x2 : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = WidgetMediaStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<WidgetMediaState>()
            GlanceTheme {
                ControlWidget4x2Content(state = state, context = context)
            }
        }
    }

    @Composable
    private fun ControlWidget4x2Content(
        state: WidgetMediaState,
        context: Context,
    ) {
        val colors = GlanceTheme.colors
        val title = state.title.ifBlank { context.getString(R.string.app_name) }
        val artist = state.artist.ifBlank {
            state.appLabel.ifBlank { context.getString(R.string.widget_tap_to_open) }
        }
        val lyricLine = state.lyricLine.ifBlank { state.album.ifBlank { artist } }
        val progress = when {
            state.durationMs <= 0L -> 0f
            else -> (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        }

        Box(
            modifier = GlanceModifier
                .background(colors.surface)
                .cornerRadius(28.dp)
                .padding(16.dp)
                .clickable(openLyricsAction()),
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetArtwork(
                        artworkUri = state.artworkUri,
                        size = 64.dp,
                        cornerRadius = 16.dp,
                        modifier = GlanceModifier.size(64.dp),
                    )

                    Spacer(GlanceModifier.width(12.dp))

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = title,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                            ),
                            maxLines = 1,
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = artist,
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = colors.onSurfaceVariant,
                            ),
                            maxLines = 1,
                        )
                        if (state.appLabel.isNotBlank() && state.artist.isNotBlank()) {
                            Spacer(GlanceModifier.height(2.dp))
                            Text(
                                text = state.appLabel,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = colors.primary,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }

                Spacer(GlanceModifier.height(10.dp))

                Text(
                    text = lyricLine,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (state.lyricLine.isNotBlank()) colors.onSurface else colors.onSurfaceVariant,
                    ),
                    maxLines = 2,
                )

                if (state.lyricTranslation.isNotBlank()) {
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = state.lyricTranslation,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant,
                        ),
                        maxLines = 1,
                    )
                }

                Spacer(GlanceModifier.defaultWeight())

                if (state.durationMs > 0L) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = GlanceModifier.fillMaxWidth().height(3.dp),
                        color = colors.primary,
                        backgroundColor = colors.secondaryContainer,
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = formatDuration(state.positionMs),
                            style = TextStyle(fontSize = 10.sp, color = colors.onSurfaceVariant),
                        )
                        Spacer(GlanceModifier.defaultWeight())
                        Text(
                            text = formatDuration(state.durationMs),
                            style = TextStyle(fontSize = 10.sp, color = colors.onSurfaceVariant),
                        )
                    }
                }

                Spacer(GlanceModifier.height(8.dp))

                if (state.hasSession) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PreviousButton(
                            modifier = GlanceModifier.size(40.dp),
                            backgroundColor = colors.secondaryContainer,
                            iconColor = colors.onSecondaryContainer,
                            cornerRadius = 16.dp,
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        PlayPauseButton(
                            modifier = GlanceModifier.size(48.dp),
                            isPlaying = state.isPlaying,
                            backgroundColor = colors.primaryContainer,
                            iconColor = colors.onPrimaryContainer,
                            cornerRadius = 24.dp,
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        NextButton(
                            modifier = GlanceModifier.size(40.dp),
                            backgroundColor = colors.secondaryContainer,
                            iconColor = colors.onSecondaryContainer,
                            cornerRadius = 16.dp,
                        )
                    }
                } else {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.End,
                    ) {
                        WidgetOpenChevron(colors.onSurfaceVariant)
                    }
                }
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val totalSeconds = (ms / 1_000.0).roundToLong()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
