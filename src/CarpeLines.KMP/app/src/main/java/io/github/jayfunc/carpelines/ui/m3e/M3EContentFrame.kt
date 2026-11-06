package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.jayfunc.carpelines.ui.theme.M3ELayoutMetrics
import io.github.jayfunc.carpelines.ui.theme.rememberM3ELayoutMetrics

/**
 * Constrains a scrollable page to a readable column and centers it.
 *
 * M3 margins grow with the window but content stops stretching once it would exceed a readable
 * width — otherwise a tablet shows the same phone page with 4x the line length, which is the
 * most common way an "adaptive" layout still feels wrong. Compact windows are unconstrained, so
 * this is a no-op on a phone.
 *
 * Screens keep their own horizontal padding inside the frame; the frame only decides where the
 * column sits and how wide it may grow.
 */
@Composable
fun M3EContentFrame(
    modifier: Modifier = Modifier,
    metrics: M3ELayoutMetrics = rememberM3ELayoutMetrics(),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val maxContentWidth = metrics.maxContentWidth
        Box(
            modifier = Modifier
                .then(
                    // Compact windows carry Dp.Unspecified (NaN) and stay full width.
                    if (maxContentWidth.value.isNaN()) Modifier
                    else Modifier.widthIn(max = maxContentWidth)
                )
                .fillMaxSize(),
            content = content
        )
    }
}
