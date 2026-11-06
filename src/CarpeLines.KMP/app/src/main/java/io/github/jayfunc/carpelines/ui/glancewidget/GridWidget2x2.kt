package io.github.jayfunc.carpelines.ui.glancewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.min
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.jayfunc.carpelines.R

class GridWidget2x2 : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = WidgetMediaStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<WidgetMediaState>()
            GlanceTheme {
                GridWidget2x2Content(state = state, context = context)
            }
        }
    }

    @Composable
    private fun GridWidget2x2Content(
        state: WidgetMediaState,
        context: Context,
    ) {
        val colors = GlanceTheme.colors
        val size = LocalSize.current
        val side = min(size.width, size.height)
        val title = state.title.ifBlank { context.getString(R.string.app_name) }
        val line = state.lyricLine.ifBlank {
            state.artist.ifBlank { context.getString(R.string.widget_tap_to_open) }
        }

        Box(
            modifier = GlanceModifier
                .size(side)
                .cornerRadius(28.dp)
                .clickable(openLyricsAction()),
        ) {
            WidgetArtwork(
                artworkUri = state.artworkUri,
                size = side,
                cornerRadius = 28.dp,
                modifier = GlanceModifier.fillMaxSize(),
            )
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xB3000000))),
            ) {
            }
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalAlignment = Alignment.Vertical.Bottom,
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White),
                    ),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.size(2.dp))
                Text(
                    text = line,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(Color(0xE6FFFFFF)),
                    ),
                    maxLines = 2,
                )
                Spacer(GlanceModifier.size(8.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.hasSession) {
                        PlayPauseButton(
                            modifier = GlanceModifier.size(36.dp),
                            isPlaying = state.isPlaying,
                            backgroundColor = colors.primaryContainer,
                            iconColor = colors.onPrimaryContainer,
                            cornerRadius = 18.dp,
                            iconSize = 18.dp,
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        NextButton(
                            modifier = GlanceModifier.size(36.dp),
                            backgroundColor = ColorProvider(Color(0x33FFFFFF)),
                            iconColor = ColorProvider(Color.White),
                            cornerRadius = 18.dp,
                            iconSize = 18.dp,
                        )
                    } else {
                        Spacer(GlanceModifier.defaultWeight())
                        WidgetOpenChevron(ColorProvider(Color.White))
                    }
                }
            }
        }
    }
}
