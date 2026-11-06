package io.github.jayfunc.carpelines.ui.theme

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * M3 spacing scale, on a 4dp baseline grid.
 *
 * Spacing in M3 encodes grouping: a tighter step inside a group and a larger step between
 * groups is what makes a list read as sections instead of a single stream of rows. These are
 * the only gaps screens should use; a one-off `12.dp` inline is how layouts drift.
 */
object M3ESpacing {
    val none: Dp = 0.dp

    /** Inline gap between an icon and its label. */
    val extraSmall: Dp = 4.dp

    /** Gap inside a dense control, between icon and text. */
    val small: Dp = 8.dp

    /** Gap between items of the same group. */
    val medium: Dp = 12.dp

    /** Standard padding inside cards, list rows and sheets. */
    val large: Dp = 16.dp

    val largeIncreased: Dp = 20.dp

    /** Gap between groups/sections. */
    val extraLarge: Dp = 24.dp

    val extraExtraLarge: Dp = 32.dp

    /** Page top padding / hero breathing room. */
    val huge: Dp = 48.dp

    val massive: Dp = 64.dp
}

/**
 * Size and target tokens. Touch targets are a release requirement, not a visual preference:
 * every interactive element must present at least [minimumTarget].
 */
object M3ESize {
    /** Minimum interactive target on touch. */
    val minimumTarget: Dp = 48.dp

    /**
     * Expressive (medium) button container height.
     *
     * M3E buttons are taller than the 40dp M2-era control: the extra height is what lets a pill
     * carry an icon and a label without crowding, and it is the step this app's action bars use.
     */
    val buttonExpressive: Dp = 56.dp

    /** Single-line list row. */
    val listItemHeight: Dp = 56.dp

    /** Two-line list row. */
    val listItemTwoLineHeight: Dp = 72.dp

    /** Three-line list row. */
    val listItemThreeLineHeight: Dp = 88.dp

    /** Leading icon container in a list row. */
    val listItemIcon: Dp = 40.dp

    val iconSmall: Dp = 16.dp
    val iconMedium: Dp = 24.dp
    val iconLarge: Dp = 32.dp

    /** Circular accent behind a leading icon. */
    val iconContainer: Dp = 40.dp

    /** Empty/error state hero glyph container. */
    val stateHero: Dp = 76.dp
}

/** Which canonical M3 window class the current window falls into. */
enum class M3EWindowSize { COMPACT, MEDIUM, EXPANDED }

/**
 * Window margins and content metrics for the current window.
 *
 * M3 margins grow with the window, and content stops stretching once it would exceed a
 * readable width. Wider windows should therefore show *more context* (a supporting pane, a
 * second column) rather than the same phone layout blown up.
 */
@Immutable
data class M3ELayoutMetrics(
    val windowSize: M3EWindowSize,
    /** Horizontal page margin. */
    val margin: Dp,
    /** Gap between columns or panes. */
    val gutter: Dp,
    /** Upper bound for a single column of text/content. */
    val maxContentWidth: Dp,
    /** Whether a navigation rail/drawer replaces the bottom bar. */
    val useNavigationRail: Boolean
)

private val CompactLayout = M3ELayoutMetrics(
    windowSize = M3EWindowSize.COMPACT,
    margin = 16.dp,
    gutter = 16.dp,
    maxContentWidth = Dp.Unspecified,
    useNavigationRail = false
)

private val MediumLayout = M3ELayoutMetrics(
    windowSize = M3EWindowSize.MEDIUM,
    margin = 24.dp,
    gutter = 24.dp,
    maxContentWidth = 600.dp,
    useNavigationRail = true
)

private val ExpandedLayout = M3ELayoutMetrics(
    windowSize = M3EWindowSize.EXPANDED,
    margin = 24.dp,
    gutter = 24.dp,
    maxContentWidth = 840.dp,
    useNavigationRail = true
)

@Composable
fun rememberM3ELayoutMetrics(): M3ELayoutMetrics {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return when {
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
            ExpandedLayout
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
            MediumLayout
        else -> CompactLayout
    }
}

/**
 * Clamps a page to [M3ELayoutMetrics.maxContentWidth].
 *
 * A no-op on compact windows. The caller still positions the result: put the constrained content
 * in a container aligned to the start/center, otherwise it stays glued to the left edge on a wide
 * window.
 */
@Composable
fun Modifier.m3eContentWidth(
    metrics: M3ELayoutMetrics = rememberM3ELayoutMetrics()
): Modifier {
    val maxContentWidth = metrics.maxContentWidth
    // Compact windows carry Dp.Unspecified (NaN), which means "let the page fill the window".
    return if (maxContentWidth.value.isNaN()) this else this.widthIn(max = maxContentWidth)
}
