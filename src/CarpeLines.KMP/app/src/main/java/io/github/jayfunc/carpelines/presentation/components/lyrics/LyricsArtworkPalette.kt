package io.github.jayfunc.carpelines.presentation.components.lyrics

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Colours for the lyric text on top of the ambient background.
 *
 * BetterLyrics keeps the lyrics a neutral grey and lets the background carry the colour. Polarity
 * is read from the *page* behind the words (the theme surface, which the now-playing colour scheme
 * already derived from the cover), not from the cover itself: a bright cover under a dark scrim
 * still needs light text, and getting that backwards is how lyrics become unreadable.
 */
@Immutable
data class LyricsTextColors(
    val played: Color,
    val unplayed: Color,
    val translation: Color,
    val romanization: Color,
    val accent: Color,
    val isLightBackground: Boolean,
)

/** The two things the lyrics page needs from the album art: its mood and its brightness. */
@Immutable
data class ArtworkPalette(
    val dominant: Color?,
    val vibrant: Color?,
    val isLight: Boolean,
)

private val paletteCache = LruCache<String, ArtworkPalette>(8)

/**
 * Extracts the palette off the main thread and caches it per artwork identity, so scrolling the
 * lyrics never re-runs the quantizer. While the next cover is still being sampled — or the new
 * track publishes no artwork at all — the last sampled palette is returned so the ambient
 * background does not drop to the theme colours in the middle of a track change.
 */
@Composable
fun rememberArtworkPalette(bitmap: Bitmap?): ArtworkPalette? {
    var lastPalette by remember { mutableStateOf<ArtworkPalette?>(null) }
    if (bitmap == null) return lastPalette
    val cacheKey = "${bitmap.generationId}:${bitmap.width}x${bitmap.height}"
    // produceState keeps its value when the key changes, so the previous track's palette is what
    // `value` holds until this producer runs. Re-sampling must therefore key off the cache, not
    // `value == null`; otherwise a new cover would never be sampled while the lyrics stay open.
    val state = produceState(initialValue = paletteCache[cacheKey], cacheKey) {
        val cached = paletteCache[cacheKey]
        if (cached != null) {
            value = cached
            return@produceState
        }
        val sampled = withContext(Dispatchers.Default) {
            runCatching {
                val palette = Palette.from(bitmap).clearFilters().maximumColorCount(24).generate()
                val dominant = palette.dominantSwatch?.rgb?.let(::Color)
                val vibrant = (palette.vibrantSwatch ?: palette.mutedSwatch)?.rgb?.let(::Color)
                val base = dominant ?: vibrant
                ArtworkPalette(
                    dominant = dominant,
                    vibrant = vibrant,
                    isLight = base?.luminance()?.let { it > 0.55f } ?: false,
                )
            }.getOrNull()
        }
        if (sampled != null) {
            paletteCache.put(cacheKey, sampled)
            value = sampled
        }
    }
    val sampled = state.value
    LaunchedEffect(sampled) {
        if (sampled != null) lastPalette = sampled
    }
    return sampled ?: lastPalette
}

/**
 * The lyric palette. The polarity comes from the page, the accent from the cover: that is the one
 * thing the artwork is actually good for, and it is what makes the page feel tinted by the song
 * without ever putting dark words on a dark scrim.
 */
fun resolveLyricsTextColors(
    palette: ArtworkPalette?,
    colorScheme: ColorScheme,
): LyricsTextColors {
    val light = colorScheme.surface.luminance() > 0.5f
    val base = if (light) Color(0xFF101014) else Color(0xFFF4F4F7)
    return LyricsTextColors(
        played = base,
        unplayed = base.copy(alpha = WinLyricsVisualDefaults.UNPLAYED_LINE_OPACITY),
        translation = base.copy(alpha = WinLyricsVisualDefaults.TRANSLATION_OPACITY),
        romanization = base.copy(alpha = WinLyricsVisualDefaults.ROMANIZATION_OPACITY),
        accent = palette?.vibrant ?: palette?.dominant ?: colorScheme.primary,
        isLightBackground = light,
    )
}

/** Stable identity for the palette cache, without hashing the whole bitmap. */
private val Bitmap.generationId: Int
    get() = System.identityHashCode(this)

/** Argb helper used by the share card renderer, which draws with the platform canvas. */
fun Color.toArgbCompat(): Int = toArgb()
