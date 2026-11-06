package io.github.jayfunc.carpelines.presentation.components.lyrics

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MathUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LyricsArtworkPaletteTest {

    private val dark = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        surface = Color(0xFF141218),
    )
    private val light = lightColorScheme(
        primary = Color(0xFF6750A4),
        surface = Color(0xFFFEFBFF),
    )

    private val redCover = ArtworkPalette(
        dominant = Color(0xFF8C1D18),
        vibrant = Color(0xFFB3261E),
        isLight = false,
    )

    @Test
    fun artworkMode_takesTheCoverHueAndStaysLegibleOnThePage() {
        val tint = resolveLyricContentColor(LyricsTextColorMode.ARTWORK, redCover, dark)

        assertThat(hueDistance(tint, redCover.vibrant!!)).isAtMost(8.0)
        assertThat(tint).isNotEqualTo(neutralDarkText)
        assertThat(contrastWith(tint, dark.surface)).isAtLeast(4.5f)
    }

    @Test
    fun artworkMode_onALightPageDarkensTheCoverTint() {
        val tint = resolveLyricContentColor(LyricsTextColorMode.ARTWORK, redCover, light)

        assertThat(hueDistance(tint, redCover.vibrant!!)).isAtMost(8.0)
        assertThat(contrastWith(tint, light.surface)).isAtLeast(4.5f)
    }

    @Test
    fun themeMode_usesTheThemePrimaryInsteadOfItsOnSurfaceWhite() {
        val tint = resolveLyricContentColor(LyricsTextColorMode.THEME, palette = null, colorScheme = dark)

        assertThat(hueDistance(tint, dark.primary)).isAtMost(8.0)
        assertThat(tint).isNotEqualTo(neutralDarkText)
        assertThat(contrastWith(tint, dark.surface)).isAtLeast(4.5f)
    }

    @Test
    fun fixedModes_ignoreTheCover() {
        assertThat(resolveLyricContentColor(LyricsTextColorMode.LIGHT, redCover, light))
            .isEqualTo(Color.White)
        assertThat(resolveLyricContentColor(LyricsTextColorMode.DARK, redCover, dark))
            .isEqualTo(Color(0xFF101014))
    }

    @Test
    fun greyscaleArtwork_hasNoHueToFollowAndStaysNeutral() {
        val palette = ArtworkPalette(
            dominant = Color(0xFF808080),
            vibrant = Color(0xFF8A8A8A),
            isLight = false,
        )

        assertThat(resolveLyricContentColor(LyricsTextColorMode.ARTWORK, palette, dark))
            .isEqualTo(neutralDarkText)
    }

    @Test
    fun missingPalette_fallsBackToTheNeutralReadingColour() {
        assertThat(resolveLyricContentColor(LyricsTextColorMode.ARTWORK, palette = null, colorScheme = dark))
            .isEqualTo(neutralDarkText)
    }

    private fun hueDistance(first: Color, second: Color): Double =
        MathUtils.differenceDegrees(
            Hct.fromInt(first.toArgb()).hue,
            Hct.fromInt(second.toArgb()).hue,
        )

    private fun contrastWith(color: Color, background: Color): Float {
        val lighter = maxOf(color.luminance(), background.luminance())
        val darker = minOf(color.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private companion object {
        val neutralDarkText = Color(0xFFF4F4F7)
    }
}
