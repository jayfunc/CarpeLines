package io.github.jayfunc.carpelines.presentation.components.betterlyrics

import android.content.Context
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI

/**
 * The raindrop and snow overlays of BetterLyrics' `NowPlayingCanvas`, ported from
 * `Shaders/RaindropEffect.cs` and `Shaders/SnowEffect.cs`.
 *
 * Those two shaders evaluate roughly four cubic-noise gradients (raindrops) or 66 grid cells
 * (snow) per pixel, which is far past what a CPU pass can do at a useful resolution - the fluid
 * gradient ported alongside them settles for a 128 px sample because it is a smooth field, and
 * neither of these is. They run as AGSL runtime shaders instead, so the GPU executes the same
 * arithmetic at display resolution and the page keeps its frame budget.
 *
 * AGSL needs Android 13, while the app starts at Android 11. On older releases
 * [isAmbientShaderSupported] is false: the settings sheet shows both styles as unavailable and
 * the page keeps the backdrop underneath them, rather than silently drawing nothing somewhere
 * nobody would look for the reason.
 */

/** The shader defaults of BetterLyrics' `LyricsBackgroundSettings`. */
private val RAINDROP_UNIFORMS = mapOf(
    "uSpeed" to 1f, // RaindropSpeed = 100 %
    "uSize" to 1f, // RaindropSize = 100 %
    "uDensity" to 0.4f, // RaindropDensity = 40 %
    "uLightAngle" to (135f * PI / 180f).toFloat(), // RaindropLightAngle = 135 degrees
    "uShadowIntensity" to 0f, // RaindropShadowIntensity = 0 %
)

private val SNOW_UNIFORMS = mapOf(
    "uDensity" to 0.1f, // SnowFlakeOverlayAmount = 10 %
    "uSpeed" to 1f, // SnowFlakeOverlaySpeed = 1
)

private const val RAINDROP_ASSET = "shaders/betterlyrics_raindrop.agsl"
private const val SNOW_ASSET = "shaders/betterlyrics_snow.agsl"

/** Where the shader clock sits while playback is paused or system animations are disabled. */
internal const val AMBIENT_SHADER_STILL_TIME_SECONDS = 9f

private const val TAG = "BetterLyricsShader"

/** True when the AGSL ports of the two overlay shaders can run on this device. */
internal val isAmbientShaderSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@Composable
internal fun RaindropOverlay(motionEnabled: Boolean, modifier: Modifier = Modifier) {
    AmbientShaderOverlay(RAINDROP_ASSET, RAINDROP_UNIFORMS, motionEnabled, modifier)
}

@Composable
internal fun SnowOverlay(motionEnabled: Boolean, modifier: Modifier = Modifier) {
    AmbientShaderOverlay(SNOW_ASSET, SNOW_UNIFORMS, motionEnabled, modifier)
}

/**
 * Drives the shader clock. Neither shader has a natural loop, so an `infiniteRepeatable` 0..1
 * phase would snap on wrap; the clock accumulates real seconds and freezes when [motionEnabled]
 * goes false. 30 Hz is indistinguishable from the display rate for a field this slow and halves
 * the full-screen passes.
 */
@Composable
internal fun rememberAmbientClock(
    motionEnabled: Boolean,
    startSeconds: Float,
): MutableFloatState {
    val clock = remember { mutableFloatStateOf(startSeconds) }
    LaunchedEffect(motionEnabled) {
        if (!motionEnabled) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        var pendingSeconds = 0f
        while (true) {
            val frame = withFrameNanos { it }
            pendingSeconds += (frame - previousFrame) / 1_000_000_000f
            previousFrame = frame
            if (pendingSeconds >= 1f / 30f) {
                clock.floatValue += pendingSeconds
                pendingSeconds = 0f
            }
        }
    }
    return clock
}

@Composable
private fun AmbientShaderOverlay(
    asset: String,
    staticUniforms: Map<String, Float>,
    motionEnabled: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current.applicationContext
    val layer = remember(asset) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AmbientShaderLayer(context, asset, staticUniforms)
        } else {
            null
        }
    }
    val clock = rememberAmbientClock(motionEnabled, AMBIENT_SHADER_STILL_TIME_SECONDS)
    if (layer == null) return

    Canvas(modifier) {
        drawIntoCanvas { canvas ->
            layer.draw(
                nativeCanvas = canvas.nativeCanvas,
                widthPx = size.width,
                heightPx = size.height,
                timeSeconds = clock.floatValue,
            )
        }
    }
}

/**
 * One compiled shader plus the paint that draws it. A paint shader is used rather than a
 * `RenderEffect`: the effect would be applied to the layer's children, while these shaders take no
 * input at all, and the shader is re-evaluated on every draw, so updating its uniforms per frame
 * needs no extra invalidate.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AmbientShaderLayer(
    context: Context,
    asset: String,
    private val staticUniforms: Map<String, Float>,
) {
    private val source: String? = runCatching {
        context.assets.open(asset).bufferedReader().use { it.readText() }
    }.onFailure { Log.w(TAG, "Missing AGSL asset $asset", it) }.getOrNull()

    private val paint = Paint().apply { isAntiAlias = false }
    private var shader: RuntimeShader? = null
    private var shaderWidth = 0f
    private var shaderHeight = 0f

    fun draw(
        nativeCanvas: android.graphics.Canvas,
        widthPx: Float,
        heightPx: Float,
        timeSeconds: Float,
    ) {
        if (widthPx < 1f || heightPx < 1f) return
        val active = shaderFor(widthPx, heightPx) ?: return
        active.setFloatUniform("uTime", timeSeconds)
        nativeCanvas.drawRect(0f, 0f, widthPx, heightPx, paint)
    }

    private fun shaderFor(widthPx: Float, heightPx: Float): RuntimeShader? {
        val current = shader
        if (current != null && widthPx == shaderWidth && heightPx == shaderHeight) return current

        val program = source ?: return null
        // AGSL compiles when the shader is built; a syntax error throws here. Falling back to the
        // plain backdrop is better than crashing the page inside a decorative effect.
        val created = runCatching { RuntimeShader(program) }
            .onFailure { Log.e(TAG, "AGSL program rejected", it) }
            .getOrNull() ?: return null

        created.setFloatUniform("uResolution", widthPx, heightPx)
        created.setFloatUniform("uTime", 0f)
        staticUniforms.forEach { (name, value) -> created.setFloatUniform(name, value) }

        paint.shader = created
        shader = created
        shaderWidth = widthPx
        shaderHeight = heightPx
        return created
    }
}
