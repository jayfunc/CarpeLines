package io.github.jayfunc.carpelines.ui.glancewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.jayfunc.carpelines.R

class BarWidget4x1 : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = WidgetMediaStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<WidgetMediaState>()
            GlanceTheme {
                BarWidget4x1Content(state = state, context = context)
            }
        }
    }

    @Composable
    private fun BarWidget4x1Content(
        state: WidgetMediaState,
        context: Context,
    ) {
        val colors = rememberWidgetColors(state.artworkSeedArgb)
        val title = state.title.ifBlank { context.getString(R.string.app_name) }
        val subtitle = state.lyricLine.ifBlank {
            state.artist.ifBlank { context.getString(R.string.widget_tap_to_open) }
        }

        Box(
            modifier = GlanceModifier
                .background(colors.container)
                .cornerRadius(28.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clickable(openLyricsAction()),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WidgetArtwork(
                    artworkUri = state.artworkUri,
                    size = 40.dp,
                    cornerRadius = 14.dp,
                    modifier = GlanceModifier.size(40.dp),
                    fallbackBackground = colors.accentContainer,
                    fallbackIconColor = colors.onAccentContainer,
                )

                Spacer(GlanceModifier.width(12.dp))

                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onContainer,
                        ),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = if (state.lyricLine.isNotBlank()) colors.accent else colors.muted,
                        ),
                        maxLines = 1,
                    )
                }

                Spacer(GlanceModifier.width(8.dp))

                if (state.hasSession) {
                    PreviousButton(
                        modifier = GlanceModifier.size(30.dp),
                        backgroundColor = ColorProvider(Color.Transparent),
                        iconColor = colors.muted,
                        cornerRadius = 15.dp,
                        iconSize = 16.dp,
                    )
                    Spacer(GlanceModifier.width(2.dp))
                    PlayPauseButton(
                        modifier = GlanceModifier.size(38.dp),
                        isPlaying = state.isPlaying,
                        backgroundColor = colors.accent,
                        iconColor = colors.onAccent,
                        cornerRadius = 19.dp,
                        iconSize = 18.dp,
                    )
                    Spacer(GlanceModifier.width(2.dp))
                    NextButton(
                        modifier = GlanceModifier.size(30.dp),
                        backgroundColor = ColorProvider(Color.Transparent),
                        iconColor = colors.muted,
                        cornerRadius = 15.dp,
                        iconSize = 16.dp,
                    )
                } else {
                    WidgetOpenChevron(colors.muted)
                }
            }
        }
    }
}
