package io.github.jayfunc.carpelines.presentation.components.betterlyrics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MathUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BetterLyricsAmbientBackgroundTest {

    private val seed = Color(0xFFB3261E)
    private val partner = Color(0xFF8C1D18)
    private val fallback = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow)

    @Test
    fun darkPageStopsStayInsideTheDarkToneBand() {
        val stops = buildAmbientAccents(seed, partner, lightPage = false, fallback = fallback)

        assertThat(stops).hasSize(4)
        stops.forEach { stop ->
            val hct = Hct.fromInt(stop.toArgb())
            assertThat(hct.tone).isAtMost(42.0)
            assertThat(hct.chroma).isAtMost(42.5)
        }
    }

    @Test
    fun lightPageStopsStayInsideTheLightToneBand() {
        val stops = buildAmbientAccents(seed, partner, lightPage = true, fallback = fallback)

        stops.forEach { stop ->
            val hct = Hct.fromInt(stop.toArgb())
            assertThat(hct.tone).isAtLeast(80.0)
            assertThat(hct.chroma).isAtMost(30.5)
        }
    }

    @Test
    fun greySeedKeepsTheGradientNeutral() {
        val grey = Color(0xFF808080)
        val stops = buildAmbientAccents(grey, grey, lightPage = false, fallback = fallback)

        stops.forEach { stop ->
            assertThat(Hct.fromInt(stop.toArgb()).chroma).isAtMost(6.0)
        }
    }

    @Test
    fun missingSeedUsesTheThemeFallback() {
        assertThat(buildAmbientAccents(null, null, lightPage = false, fallback = fallback))
            .isEqualTo(fallback)
    }

    @Test
    fun stopsStayNearTheSeedHue() {
        val stops = buildAmbientAccents(seed, partner, lightPage = false, fallback = fallback)
        val seedHue = Hct.fromInt(seed.toArgb()).hue

        stops.forEach { stop ->
            val distance = MathUtils.differenceDegrees(
                seedHue,
                Hct.fromInt(stop.toArgb()).hue,
            )
            assertThat(distance).isAtMost(15.0)
        }
    }
}
