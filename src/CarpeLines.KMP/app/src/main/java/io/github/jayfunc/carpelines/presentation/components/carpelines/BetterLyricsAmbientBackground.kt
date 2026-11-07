package io.github.jayfunc.carpelines.presentation.components.betterlyrics

import android.animation.ValueAnimator
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MathUtils
import io.github.jayfunc.carpelines.presentation.components.SmartImage
import io.github.jayfunc.carpelines.presentation.components.lyrics.ArtworkPalette
import io.github.jayfunc.carpelines.presentation.components.lyrics.isLightPage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Ambient backgrounds ported from BetterLyrics' Win2D renderers.
 *
 * `FLUID` is a port of BetterLyrics' `Shaders/FluidBackgroundEffect.cs` (itself derived from
 * Storyteller-Studios/Isolation's `effect.hlsl`): the same noise-driven domain rotation, the same
 * sine warp and the same four-colour two-layer smoothstep composite. The shader's optional
 * "light wave" HSV pass (`enableLightWave`) is not ported.
 * The original runs as a pixel shader; here the same arithmetic is evaluated on the CPU into a
 * small bitmap (see [FLUID_SAMPLE_LONG_SIDE]) that is then stretched over the page, which keeps it
 * on Android 11 where AGSL is unavailable and costs a fraction of a full-resolution pass. The
 * screen-space dithering is the one part left out: at this sample size it would be magnified into
 * a visible moving grid.
 *
 * `ALBUM_BLUR` keeps BetterLyrics' blurred-cover backdrop. [FLUID_STILL_TIME_SECONDS] is where the
 * shader time settles while playback is paused or system animations are disabled, so a paused page
 * shows a composed frame instead of the raw `time == 0` one.
 *
 * `RAINDROP` and `SNOW` are the app's two `<name>Overlay` ports of `Shaders/RaindropEffect.cs` and
 * `Shaders/SnowEffect.cs`, drawn as AGSL overlays on top of the backdrop (see
 * [RaindropOverlay]/[SnowOverlay] - they are far too expensive for the CPU pass the fluid gradient
 * gets away with).
 *
 * The background is purely decorative: it never reads playback position, so it cannot add jank to
 * lyric scrolling.
 */
enum class BetterLyricsAmbientStyle {
    FLUID,
    ALBUM_BLUR,
    RAINDROP,
    SNOW;

    companion object {
        fun fromStorage(value: String?): BetterLyricsAmbientStyle =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FLUID
    }
}

@Composable
fun BetterLyricsAmbientBackground(
    style: BetterLyricsAmbientStyle,
    albumArtModel: Any?,
    colorScheme: ColorScheme,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    palette: ArtworkPalette? = null,
) {
    val motionEnabled = remember(isPlaying) {
        isPlaying && ValueAnimator.areAnimatorsEnabled()
    }

    // The fluid shader takes four colours built from the same HCT seed as the app scheme, so the
    // gradient cannot drift into a different hue family than the lyrics or the buttons. Without a
    // palette they fall back to the theme roles so the page still works before artwork is sampled.
    val targetAccents = remember(palette, colorScheme) {
        buildAmbientAccents(
            seed = palette?.seed,
            partner = palette?.vibrant ?: palette?.dominant,
            lightPage = isLightPage(colorScheme),
            fallback = listOf(
                colorScheme.primary,
                colorScheme.secondary,
                colorScheme.tertiary,
                colorScheme.primaryContainer,
            ),
        )
    }
    // A track change swaps all four cover tones at once, which reads as a colour flash over the
    // page. Fading them keeps the background calm while the next cover is being sampled.
    val accentSpec = tween<Color>(durationMillis = 700)
    val accent0 by animateColorAsState(targetAccents[0], accentSpec, label = "ambientAccent0")
    val accent1 by animateColorAsState(targetAccents[1], accentSpec, label = "ambientAccent1")
    val accent2 by animateColorAsState(targetAccents[2], accentSpec, label = "ambientAccent2")
    val accent3 by animateColorAsState(targetAccents[3], accentSpec, label = "ambientAccent3")
    val accents = remember(accent0, accent1, accent2, accent3) {
        listOf(accent0, accent1, accent2, accent3)
    }

    val baseBrush = remember(colorScheme) {
        Brush.verticalGradient(
            listOf(
                colorScheme.surfaceContainerLow,
                colorScheme.surface,
                colorScheme.surfaceContainerLowest,
            ),
        )
    }

    // `RAINDROP` and `SNOW` are overlays in WinUI: the page draws them over whatever backdrop it
    // already has. They keep that shape here - the blurred cover when the track has artwork (rain
    // on glass reads as a lit cover), the fluid gradient otherwise - and stay under the lyric scrim
    // so the words keep their edge.
    Box(modifier = modifier.fillMaxSize().background(baseBrush)) {
        if (albumArtModel != null && style != BetterLyricsAmbientStyle.FLUID) {
            CoverBackdrop(albumArtModel, colorScheme, Modifier.fillMaxSize())
        } else {
            FluidGradientLayer(
                accents = accents,
                colorScheme = colorScheme,
                motionEnabled = motionEnabled,
                modifier = Modifier.fillMaxSize(),
            )
        }
        when (style) {
            BetterLyricsAmbientStyle.RAINDROP -> RaindropOverlay(
                motionEnabled = motionEnabled,
                modifier = Modifier.fillMaxSize(),
            )

            BetterLyricsAmbientStyle.SNOW -> SnowOverlay(
                motionEnabled = motionEnabled,
                modifier = Modifier.fillMaxSize(),
            )

            else -> Unit
        }
    }
}

/** BetterLyrics' blurred-cover backdrop, shared by `ALBUM_BLUR` and the two overlay styles. */
@Composable
private fun CoverBackdrop(albumArtModel: Any, colorScheme: ColorScheme, modifier: Modifier) {
    Box(modifier) {
        SmartImage(
            model = albumArtModel,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(48.dp)
                .graphicsLayerScale(scale = 1.25f),
            contentScale = ContentScale.Crop,
            alpha = 0.7f,
            allowHardware = true,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to colorScheme.surface.copy(alpha = 0.25f),
                        0.45f to colorScheme.surface.copy(alpha = 0.55f),
                        1.0f to colorScheme.surface.copy(alpha = 0.92f),
                    ),
                ),
        )
    }
}

private val DARK_PAGE_BACKGROUND_TONES = doubleArrayOf(22.0, 30.0, 40.0, 26.0)
private val LIGHT_PAGE_BACKGROUND_TONES = doubleArrayOf(96.0, 90.0, 84.0, 93.0)
private const val DARK_PAGE_MAX_BACKGROUND_CHROMA = 42.0
private const val LIGHT_PAGE_MAX_BACKGROUND_CHROMA = 30.0
private const val BACKGROUND_HUE_SPREAD_DEGREES = 10.0

/**
 * Builds the four shader stops in HCT instead of blending two swatches in linear RGB, which used to
 * send complementary pairs through a muddy grey. The tone ladder is fixed per page polarity so a
 * bright cover cannot wash the lyrics out, and the chroma is capped because this paints the whole
 * page. Both hues are already inside Material's 15° harmonisation window of the seed.
 */
internal fun buildAmbientAccents(
    seed: Color?,
    partner: Color?,
    lightPage: Boolean,
    fallback: List<Color>,
): List<Color> {
    if (seed == null) return fallback
    val seedHct = Hct.fromInt(seed.toArgb())
    val partnerHct = partner?.let { Hct.fromInt(it.toArgb()) } ?: seedHct
    val tones = if (lightPage) LIGHT_PAGE_BACKGROUND_TONES else DARK_PAGE_BACKGROUND_TONES
    val maxChroma = if (lightPage) {
        LIGHT_PAGE_MAX_BACKGROUND_CHROMA
    } else {
        DARK_PAGE_MAX_BACKGROUND_CHROMA
    }
    // A near-grey cover stays near-grey; the shader must not invent chroma the song does not have.
    val chroma = seedHct.chroma.coerceAtMost(maxChroma)
    return listOf(
        ambientStop(seedHct.hue, chroma * 0.85, tones[0]),
        ambientStop(partnerHct.hue, chroma, tones[1]),
        ambientStop(seedHct.hue + BACKGROUND_HUE_SPREAD_DEGREES, chroma * 0.75, tones[2]),
        ambientStop(partnerHct.hue - BACKGROUND_HUE_SPREAD_DEGREES, chroma * 0.9, tones[3]),
    )
}

private fun ambientStop(hue: Double, chroma: Double, tone: Double): Color = Color(
    Hct.from(
        MathUtils.sanitizeDegreesDouble(hue),
        chroma.coerceIn(0.0, 100.0),
        tone.coerceIn(0.0, 100.0),
    ).toInt(),
)

/**
 * The CPU pass of the fluid gradient. The shader has no natural loop — the noise rotation and the
 * sine warp keep evolving with `time` — so the shared clock accumulates real seconds instead of
 * wrapping a 0..1 phase; at 30 Hz it also halves the cost of the pixel loop plus the redraw it
 * triggers.
 */
@Composable
private fun FluidGradientLayer(
    accents: List<Color>,
    colorScheme: ColorScheme,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val clock = rememberAmbientClock(motionEnabled, FLUID_STILL_TIME_SECONDS)

    val surface = remember { FluidGradientSurface() }
    Canvas(modifier) {
        surface.draw(
            scope = this,
            accents = accents,
            time = clock.floatValue,
            targetWidth = max(1, size.width.roundToInt()),
            targetHeight = max(1, size.height.roundToInt()),
        )
        drawVignette(colorScheme)
    }
}

// 128 px on the long side: the shader's output is a smooth gradient, so stretching a small buffer
// with bilinear filtering looks identical to full resolution while keeping the CPU pass cheap.
private const val FLUID_SAMPLE_LONG_SIDE = 128
private const val FLUID_STILL_TIME_SECONDS = 9f

private val DEG_TO_RAD = (PI / 180.0).toFloat()

/** Holds the reusable pixel buffer the shader writes into, rebuilt only when the page resizes. */
private class FluidGradientSurface {
    private var bitmap: Bitmap? = null
    private var pixels = IntArray(0)
    private var sampleWidth = 0
    private var sampleHeight = 0

    private val colors = FloatArray(12)
    private val rgb = FloatArray(3)

    fun draw(
        scope: DrawScope,
        accents: List<Color>,
        time: Float,
        targetWidth: Int,
        targetHeight: Int,
    ) {
        val longSide = max(targetWidth, targetHeight)
        val scale = if (longSide > FLUID_SAMPLE_LONG_SIDE) {
            FLUID_SAMPLE_LONG_SIDE.toFloat() / longSide
        } else {
            1f
        }
        val width = max(8, (targetWidth * scale).roundToInt())
        val height = max(8, (targetHeight * scale).roundToInt())
        ensureBuffers(width, height)

        for (index in 0 until 4) {
            val color = accents[index]
            colors[index * 3] = color.red
            colors[index * 3 + 1] = color.green
            colors[index * 3 + 2] = color.blue
        }

        var index = 0
        for (y in 0 until height) {
            val uvY = (y + 0.5f) / height
            for (x in 0 until width) {
                pixels[index++] = fluidPixel(
                    uvX = (x + 0.5f) / width,
                    uvY = uvY,
                    time = time,
                    colors = colors,
                    rgb = rgb,
                )
            }
        }

        val target = bitmap ?: return
        target.setPixels(pixels, 0, width, 0, 0, width, height)
        scope.drawImage(
            image = target.asImageBitmap(),
            dstSize = IntSize(targetWidth, targetHeight),
            filterQuality = FilterQuality.Low,
        )
    }

    private fun ensureBuffers(width: Int, height: Int) {
        if (bitmap != null && width == sampleWidth && height == sampleHeight) return
        sampleWidth = width
        sampleHeight = height
        pixels = IntArray(width * height)
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
}

/**
 * One pixel of `FluidBackgroundEffect.Execute()`. The shader's constant buffer is fixed to the
 * values BetterLyrics' `FluidBackgroundRenderer` uses: `useHsvBlending = false` and
 * `randomValue1..3 = 0` (`enableDithering` is skipped, see below).
 */
private fun fluidPixel(
    uvX: Float,
    uvY: Float,
    time: Float,
    colors: FloatArray,
    rgb: FloatArray,
): Int {
    var x = uvX - 0.5f
    var y = uvY - 0.5f

    // Noise drives the rotation: the whole sheet slowly turns, which is where the "fluid" look
    // comes from before the sine warp is even applied.
    val degree = fNoise(time * 0.1f, x * y)
    val rotation = ((degree - 0.5f) * 720f + 180f) * DEG_TO_RAD
    val rotationCos = cos(rotation)
    val rotationSin = sin(rotation)
    val rotatedX0 = x * rotationCos - y * rotationSin
    val rotatedY0 = x * rotationSin + y * rotationCos
    x = rotatedX0
    y = rotatedY0

    val frequency = 5f
    val amplitude = 25f
    val speed = time * 0.75f

    // Domain warp, applied in order: the second line reads the already-warped x, as in the shader.
    x += sin(y * frequency + speed) / amplitude
    y += sin(x * frequency * 1.5f + speed) / (amplitude * 0.5f)

    val tiltedCos = cos(-5f * DEG_TO_RAD)
    val tiltedSin = sin(-5f * DEG_TO_RAD)
    val tiltedX = x * tiltedCos - y * tiltedSin

    val sideBlend = smoothStep(-0.3f, 0.2f, tiltedX)
    val layerBlend = smoothStep(0.5f, -0.3f, y)

    for (channel in 0 until 3) {
        val layer1 = lerp(colors[channel], colors[3 + channel], sideBlend)
        val layer2 = lerp(colors[6 + channel], colors[9 + channel], sideBlend)
        rgb[channel] = lerp(layer1, layer2, layerBlend)
    }

    // No dithering here on purpose: the shader's screen-space dither is meant for native
    // resolution, where it only shifts a channel by well under one 8-bit step. This buffer is a
    // coarse sample stretched over the page, so the same pattern would be magnified into a visible
    // grid of moving blocks. Bilinear upscaling already hides the quantisation.
    return packColor(rgb[0], rgb[1], rgb[2])
}

private fun fNoise(px: Float, py: Float): Float {
    val baseX = floor(px)
    val baseY = floor(py)
    val fx = px - baseX
    val fy = py - baseY
    val ux = fx * fx * (3f - 2f * fx)
    val uy = fy * fy * (3f - 2f * fy)

    val n00 = gradientDot(baseX, baseY, 0f, 0f, fx, fy)
    val n10 = gradientDot(baseX, baseY, 1f, 0f, fx, fy)
    val n01 = gradientDot(baseX, baseY, 0f, 1f, fx, fy)
    val n11 = gradientDot(baseX, baseY, 1f, 1f, fx, fy)

    val lower = lerp(n00, n10, ux)
    val upper = lerp(n01, n11, ux)
    return 0.5f + 0.5f * lerp(lower, upper, uy)
}

private fun gradientDot(
    baseX: Float,
    baseY: Float,
    offsetX: Float,
    offsetY: Float,
    fx: Float,
    fy: Float,
): Float {
    val hashX = baseX + offsetX
    val hashY = baseY + offsetY
    val gradientX = -1f + 2f * hashComponent(hashX, hashY, 2127.1f, 81.17f)
    val gradientY = -1f + 2f * hashComponent(hashX, hashY, 1269.5f, 283.37f)
    return gradientX * (fx - offsetX) + gradientY * (fy - offsetY)
}

private fun hashComponent(x: Float, y: Float, xScale: Float, yScale: Float): Float =
    frac(sin(x * xScale + y * yScale) * 43758.5453f)

private fun packColor(r: Float, g: Float, b: Float): Int =
    (0xFF shl 24) or (toColorByte(r) shl 16) or (toColorByte(g) shl 8) or toColorByte(b)

private fun toColorByte(value: Float): Int = (value * 255f + 0.5f).toInt().coerceIn(0, 255)

private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
    val t = saturate((value - edge0) / (edge1 - edge0))
    return t * t * (3f - 2f * t)
}

private fun lerp(from: Float, to: Float, amount: Float): Float = from + (to - from) * amount

private fun saturate(value: Float): Float = value.coerceIn(0f, 1f)

private fun frac(value: Float): Float = value - floor(value)

private fun DrawScope.drawVignette(colors: ColorScheme) {
    val edge = colors.surface.copy(alpha = 0.88f)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(edge.copy(alpha = 0f), edge.copy(alpha = 0f), edge),
            center = Offset(size.width / 2f, size.height * 0.42f),
            radius = max(size.width, size.height) * 0.95f,
        ),
        size = Size(size.width, size.height),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, colors.surface.copy(alpha = 0.55f)),
            startY = size.height * 0.62f,
            endY = size.height,
        ),
        size = Size(size.width, size.height),
    )
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier =
    this.graphicsLayer(scaleX = scale, scaleY = scale)
