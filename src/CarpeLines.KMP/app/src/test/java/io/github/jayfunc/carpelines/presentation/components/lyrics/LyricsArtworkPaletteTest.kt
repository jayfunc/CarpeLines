package io.github.jayfunc.carpelines.presentation.components.lyrics

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

    @Test
    fun artworkMode_matchesTheSongIdentityColour() {
        assertThat(resolveLyricContentColor(LyricsTextColorMode.ARTWORK, dark))
            .isEqualTo(dark.primary)
        assertThat(contrastWith(dark.primary, dark.surface)).isAtLeast(4.5f)
    }

    @Test
    fun artworkMode_followsTheSchemeOnALightPage() {
        assertThat(resolveLyricContentColor(LyricsTextColorMode.ARTWORK, light))
            .isEqualTo(light.primary)
        assertThat(contrastWith(light.primary, light.surface)).isAtLeast(4.5f)
    }

    @Test
    fun neutralMode_ignoresTheSchemeHue() {
        val tint = resolveLyricContentColor(LyricsTextColorMode.NEUTRAL, dark)

        assertThat(tint).isEqualTo(neutralDarkText)
        assertThat(contrastWith(tint, dark.surface)).isAtLeast(4.5f)
    }

    @Test
    fun storedThemePreference_fallsBackToArtwork() {
        assertThat(LyricsTextColorMode.fromStorage("theme"))
            .isEqualTo(LyricsTextColorMode.ARTWORK)
    }

    @Test
    fun fixedModes_ignoreTheScheme() {
        assertThat(resolveLyricContentColor(LyricsTextColorMode.LIGHT, light))
            .isEqualTo(Color.White)
        assertThat(resolveLyricContentColor(LyricsTextColorMode.DARK, dark))
            .isEqualTo(Color(0xFF101014))
    }

    private fun contrastWith(color: Color, background: Color): Float {
        val lighter = maxOf(color.luminance(), background.luminance())
        val darker = minOf(color.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private companion object {
        val neutralDarkText = Color(0xFFF4F4F7)
    }
}
