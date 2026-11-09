package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/** One destination of an M3 navigation bar or rail. */
@Immutable
data class M3ENavItem(
    val label: String,
    val icon: Painter,
    val selectedIcon: Painter = icon
)
