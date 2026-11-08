package io.github.jayfunc.carpelines.ui.glancewidget

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.unit.ColorProvider
import io.github.jayfunc.carpelines.ui.theme.generateColorSchemeFromSeed

/**
 * The semantic colours an M3E widget draws with.
 *
 * M3E surfaces are tonal: the container, its content and the accent are roles of one scheme, not
 * four hand-picked colours. The scheme is seeded from the playing cover, so every widget follows
 * the album while staying readable in the system's light or dark mode.
 */
@Immutable
internal data class WidgetColors(
    val container: ColorProvider,
    val onContainer: ColorProvider,
    val muted: ColorProvider,
    val accent: ColorProvider,
    val onAccent: ColorProvider,
    val accentContainer: ColorProvider,
    val onAccentContainer: ColorProvider,
    val secondaryContainer: ColorProvider,
    val onSecondaryContainer: ColorProvider,
)

@Composable
internal fun rememberWidgetColors(seedArgb: Int): WidgetColors {
    val context = LocalContext.current
    val system = GlanceTheme.colors
    if (seedArgb == 0) {
        return WidgetColors(
            container = system.surface,
            onContainer = system.onSurface,
            muted = system.onSurfaceVariant,
            accent = system.primary,
            onAccent = system.onPrimary,
            accentContainer = system.primaryContainer,
            onAccentContainer = system.onPrimaryContainer,
            secondaryContainer = system.secondaryContainer,
            onSecondaryContainer = system.onSecondaryContainer,
        )
    }

    val dark = (context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    val scheme = WidgetSchemeCache.scheme(seedArgb, dark)
    return WidgetColors(
        container = ColorProvider(scheme.surfaceContainerHigh),
        onContainer = ColorProvider(scheme.onSurface),
        muted = ColorProvider(scheme.onSurfaceVariant),
        accent = ColorProvider(scheme.primary),
        onAccent = ColorProvider(scheme.onPrimary),
        accentContainer = ColorProvider(scheme.primaryContainer),
        onAccentContainer = ColorProvider(scheme.onPrimaryContainer),
        secondaryContainer = ColorProvider(scheme.secondaryContainer),
        onSecondaryContainer = ColorProvider(scheme.onSecondaryContainer),
    )
}

/** Building a tonal palette is HCT work; one per cover and theme is enough for the launcher. */
private object WidgetSchemeCache {
    private val schemes = object : android.util.LruCache<Long, ColorScheme>(8) {}

    @Synchronized
    fun scheme(seedArgb: Int, dark: Boolean): ColorScheme {
        val key = seedArgb.toLong() * 2 + if (dark) 1 else 0
        schemes.get(key)?.let { return it }
        val generated = generateColorSchemeFromSeed(Color(seedArgb))
        val scheme = if (dark) generated.dark else generated.light
        schemes.put(key, scheme)
        return scheme
    }
}
