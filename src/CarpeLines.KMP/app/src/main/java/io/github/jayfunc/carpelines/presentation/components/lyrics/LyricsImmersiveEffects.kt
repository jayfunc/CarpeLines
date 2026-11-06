package io.github.jayfunc.carpelines.presentation.components.lyrics

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Reading lyrics is a hands-off activity, so the screen must not time out while they scroll.
 * Restores whatever the window had before, so leaving the tab does not keep the display awake.
 */
@Composable
fun KeepLyricsScreenOn() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
}

/**
 * Matches the system bar icons to the album art and keeps the status bar out of the way for as
 * long as the lyrics page is on screen. The page is the artwork, so a clock and a battery icon
 * sitting on top of the cover break it in both faces — the overview as much as the lyrics-only
 * one. The navigation bar stays visible: hiding it turns the first swipe into a gesture the user
 * did not ask for.
 */
@Composable
fun LyricsSystemBarsAppearance(isLightBackground: Boolean) {
    val view = LocalView.current
    val window = remember(view) { view.context.findActivity()?.window }
    if (window == null || view.isInEditMode) return
    val controller = remember(window, view) { WindowCompat.getInsetsController(window, view) }

    DisposableEffect(controller) {
        val lightStatusBars = controller.isAppearanceLightStatusBars
        val lightNavigationBars = controller.isAppearanceLightNavigationBars
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatusBars
            controller.isAppearanceLightNavigationBars = lightNavigationBars
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    // Re-applied on every composition so the theme cannot overrule the lyrics palette, and so the
    // bar stays hidden if a transient swipe brought it back.
    SideEffect {
        controller.isAppearanceLightStatusBars = isLightBackground
        controller.isAppearanceLightNavigationBars = isLightBackground
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
