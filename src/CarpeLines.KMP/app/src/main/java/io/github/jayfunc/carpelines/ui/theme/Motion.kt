package io.github.jayfunc.carpelines.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * True when the platform asked for reduced motion ("Remove animations" in Accessibility, i.e. an
 * animator duration scale of zero).
 *
 * M3 motion explains a state change; when the user has disabled animation the explanation has to
 * be carried by the static UI instead. Decorative, looping animations are the first thing to drop.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Reads the system animator scale. Call once at the theme root and provide [LocalReducedMotion]. */
@Composable
fun rememberSystemReducedMotion(): Boolean = !ValueAnimator.areAnimatorsEnabled()

/**
 * M3 motion tokens.
 *
 * Motion has two jobs in M3: explain a state change (effects) and explain a spatial
 * relationship (spatial). Use the token that matches the job instead of inventing a duration —
 * inconsistent timings are the most common reason a Material screen feels "almost right".
 *
 * Prefer the theme's [MotionScheme] through [M3EMotion.spatial] / [M3EMotion.effects] so the
 * whole app switches between the standard and expressive schemes together, and so a future
 * reduced-motion setting has one place to take effect.
 */
object M3EMotion {

    /** Duration tokens, in milliseconds. */
    object Duration {
        const val short1 = 50
        const val short2 = 100
        const val short3 = 150
        const val short4 = 200
        const val medium1 = 250
        const val medium2 = 300
        const val medium3 = 350
        const val medium4 = 400
        const val long1 = 450
        const val long2 = 500
        const val long3 = 550
        const val long4 = 600
        const val extraLong1 = 700
        const val extraLong2 = 800
        const val extraLong3 = 900
        const val extraLong4 = 1000
    }

    /**
     * Easing tokens. The emphasised curve family is for elements that enter or leave the
     * screen or change their geometry; the standard curve is for smaller, in-place changes.
     */
    object Easings {
        val linear: Easing = CubicBezierEasing(0f, 0f, 1f, 1f)
        val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val standardDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
        val standardAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
        val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }

    /** Spatial spec for movement and size change, from the active [MotionScheme]. */
    @Composable
    fun <T> spatial(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultSpatialSpec()

    /** Faster spatial spec for small, frequent movements such as press feedback. */
    @Composable
    fun <T> spatialFast(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.fastSpatialSpec()

    /** Slower spatial spec for large surfaces such as sheets and pane transitions. */
    @Composable
    fun <T> spatialSlow(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.slowSpatialSpec()

    /** Spec for colour, alpha and other non-spatial changes. */
    @Composable
    fun <T> effects(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultEffectsSpec()

    /**
     * Number of milliseconds a state-layer press fade should take. Motion in M3 is never the
     * only feedback for an action, so this is a polish value, not a timing contract.
     */
    const val pressFadeMillis = Duration.short3
}
