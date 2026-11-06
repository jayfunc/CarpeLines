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
        val colors = GlanceTheme.colors
        val title = state.title.ifBlank { context.getString(R.string.app_name) }
        val subtitle = state.lyricLine.ifBlank {
            state.artist.ifBlank { context.getString(R.string.widget_tap_to_open) }
        }

        Box(
            modifier = GlanceModifier
                .background(colors.surface)
                .cornerRadius(28.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .clickable(openLyricsAction()),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WidgetArtwork(
                    artworkUri = state.artworkUri,
                    size = 48.dp,
                    cornerRadius = 14.dp,
                    modifier = GlanceModifier.size(48.dp),
                )

                Spacer(GlanceModifier.width(10.dp))

                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        ),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = if (state.lyricLine.isNotBlank()) colors.primary else colors.onSurfaceVariant,
                        ),
                        maxLines = 1,
                    )
                }

                Spacer(GlanceModifier.width(8.dp))

                if (state.hasSession) {
                    PreviousButton(
                        modifier = GlanceModifier.size(36.dp),
                        backgroundColor = colors.secondaryContainer,
                        iconColor = colors.onSecondaryContainer,
                        cornerRadius = 14.dp,
                        iconSize = 18.dp,
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    PlayPauseButton(
                        modifier = GlanceModifier.size(40.dp),
                        isPlaying = state.isPlaying,
                        backgroundColor = colors.primaryContainer,
                        iconColor = colors.onPrimaryContainer,
                        cornerRadius = 20.dp,
                        iconSize = 20.dp,
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    NextButton(
                        modifier = GlanceModifier.size(36.dp),
                        backgroundColor = colors.secondaryContainer,
                        iconColor = colors.onSecondaryContainer,
                        cornerRadius = 14.dp,
                        iconSize = 18.dp,
                    )
                } else {
                    WidgetOpenChevron(colors.onSurfaceVariant)
                }
            }
        }
    }
}
