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
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.google.android.material.color.utilities.Blend
import com.google.android.material.color.utilities.Hct
import io.github.jayfunc.carpelines.data.external.decodeArtworkBitmap
import io.github.jayfunc.carpelines.ui.theme.extractSeedColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Colours for the lyric text on top of the ambient background.
 *
 * Polarity is read from the *page* behind the words (the theme surface, which the now-playing
 * colour scheme already derived from the cover), not from the cover itself: a bright cover under
 * a dark scrim still needs light text, and getting that backwards is how lyrics become
 * unreadable. Only the hue comes from the song palette; [legibleTint] re-lights it for the page so
 * the tinted mode stays readable.
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

/**
 * The song's colour identity, sampled once and shared by the lyrics text and the ambient
 * background. [seed] is the same colour the app-wide Material scheme is built from, so the page
 * cannot drift into a different hue family than the rest of the UI.
 */
@Immutable
data class ArtworkPalette(
    val dominant: Color?,
    val vibrant: Color?,
    val seed: Color?,
    val isLight: Boolean,
)

private val paletteCache = LruCache<String, ArtworkPalette>(8)

/**
 * The cover bitmap to sample. Media sessions usually publish the bitmap, but a player that only
 * exposes an artwork URI used to leave this page without a palette while the app-wide scheme
 * decoded that URI and coloured every other component. Decoding it here as well keeps the gradient,
 * the lyrics and the chrome on the same image.
 */
@Composable
fun rememberArtworkBitmap(bitmap: Bitmap?, uri: String?): Bitmap? {
    if (bitmap != null) return bitmap
    if (uri.isNullOrBlank()) return null
    val context = LocalContext.current.applicationContext
    val decoded = produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { decodeArtworkBitmap(context, uri) }
    }
    return decoded.value
}

/**
 * Extracts the palette off the main thread and caches it per artwork identity, so scrolling the
 * lyrics never re-runs the quantizer. While the next cover is still being sampled — or the new
 * track publishes no artwork at all — the last sampled palette is returned so the ambient
 * background does not drop to the theme colours in the middle of a track change.
 *
 * The artwork swatches are harmonised towards [extractSeedColor]'s seed, which is what the app-wide
 * scheme uses. A cover with a small high-chroma logo used to give the gradient a different hue than
 * every button in the app; keeping them within Material's 15° harmonisation window removes that
 * clash without flattening the artwork to one tone.
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
                val seed = extractSeedColor(bitmap)
                val palette = Palette.from(bitmap).clearFilters().maximumColorCount(24).generate()
                val dominant = palette.dominantSwatch?.rgb?.let(::Color)?.harmonizedTo(seed)
                val vibrant = (palette.vibrantSwatch ?: palette.mutedSwatch)?.rgb
                    ?.let(::Color)
                    ?.harmonizedTo(seed)
                val base = vibrant ?: dominant ?: seed
                ArtworkPalette(
                    dominant = dominant,
                    vibrant = vibrant,
                    seed = seed,
                    isLight = base.luminance() > 0.55f,
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
 * The page-level lyric palette: polarity, accents and the neutral reading colour. The accent is
 * the scheme's own primary, so the playing line and the rest of the app stay on one hue.
 */
fun resolveLyricsTextColors(colorScheme: ColorScheme): LyricsTextColors {
    val light = isLightPage(colorScheme)
    val base = neutralLyricColor(light)
    return LyricsTextColors(
        played = base,
        unplayed = base.copy(alpha = WinLyricsVisualDefaults.UNPLAYED_LINE_OPACITY),
        translation = base.copy(alpha = WinLyricsVisualDefaults.TRANSLATION_OPACITY),
        romanization = base.copy(alpha = WinLyricsVisualDefaults.ROMANIZATION_OPACITY),
        accent = colorScheme.primary,
        isLightBackground = light,
    )
}

/**
 * The lyric text colour for the "Lyric colour" setting. `ARTWORK` takes the harmonised cover hue,
 * `NEUTRAL` follows the page polarity without a hue, and the fixed modes ignore both. The tinted
 * mode is re-lit for the page so a dark cover tone cannot disappear into a dark page.
 */
fun resolveLyricContentColor(
    mode: LyricsTextColorMode,
    palette: ArtworkPalette?,
    colorScheme: ColorScheme,
): Color {
    val light = isLightPage(colorScheme)
    return when (mode) {
        LyricsTextColorMode.ARTWORK ->
            (palette?.vibrant ?: palette?.seed)?.let { legibleTint(it, light) }
                ?: neutralLyricColor(light)

        LyricsTextColorMode.NEUTRAL -> neutralLyricColor(light)
        LyricsTextColorMode.LIGHT -> Color.White
        LyricsTextColorMode.DARK -> Color(0xFF101014)
    }
}

internal fun isLightPage(colorScheme: ColorScheme): Boolean =
    colorScheme.surface.luminance() > 0.5f

/** The reading colour used when the chosen source carries no hue of its own. */
private fun neutralLyricColor(lightPage: Boolean): Color =
    if (lightPage) Color(0xFF101014) else Color(0xFFF4F4F7)

/**
 * Keeps the source hue but pins its Material tone to the page: a cover's mid-tone can be a dark
 * navy on a dark page or a pale yellow on a light one, and either disappears into the scrim. The
 * chroma ceiling keeps a highly saturated cover from turning every word into a highlighter.
 */
private fun legibleTint(source: Color, lightPage: Boolean): Color {
    val hct = Hct.fromInt(source.toArgb())
    if (hct.chroma < MIN_TINT_CHROMA) return neutralLyricColor(lightPage)
    hct.chroma = hct.chroma.coerceIn(MIN_TINT_CHROMA, MAX_TINT_CHROMA)
    hct.tone = if (lightPage) LIGHT_PAGE_TINT_TONE else DARK_PAGE_TINT_TONE
    return Color(hct.toInt())
}

/** Aligns an artwork swatch to the scheme seed without losing its own tone or chroma. */
private fun Color.harmonizedTo(reference: Color): Color =
    Color(Blend.harmonize(toArgb(), reference.toArgb()))

/** HCT chroma below ~12 reads as grey; the same cutoff the artwork sampler uses. */
private const val MIN_TINT_CHROMA = 12.0
private const val MAX_TINT_CHROMA = 28.0
private const val DARK_PAGE_TINT_TONE = 84.0
private const val LIGHT_PAGE_TINT_TONE = 24.0

/** Stable identity for the palette cache, without hashing the whole bitmap. */
private val Bitmap.generationId: Int
    get() = System.identityHashCode(this)

/** Argb helper used by the share card renderer, which draws with the platform canvas. */
fun Color.toArgbCompat(): Int = toArgb()
