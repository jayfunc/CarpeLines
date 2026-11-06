package io.github.jayfunc.carpelines.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.jayfunc.carpelines.presentation.viewmodel.ColorSchemePair
import androidx.core.graphics.ColorUtils

val LocalCarpeLinesDarkTheme = staticCompositionLocalOf { false }
val LocalShowScrollbar = staticCompositionLocalOf { true }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
@Composable
fun CarpeLinesStatusBarStyle(
    color: Color,
    useDarkIcons: Boolean = ColorUtils.calculateLuminance(color.toArgb()) > 0.55,
    navigationColor: Color? = null,
    useDarkNavigationIcons: Boolean = navigationColor
        ?.let { ColorUtils.calculateLuminance(it.toArgb()) > 0.55 }
        ?: useDarkIcons
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val updateNavigationBar = navigationColor != null
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = useDarkIcons

            if (updateNavigationBar) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                isAppearanceLightNavigationBars = useDarkNavigationIcons
            }
        }
    }
}

val DarkColorScheme = darkColorScheme(
    primary = CarpeLinesPurplePrimary,
    secondary = CarpeLinesPink,
    tertiary = CarpeLinesOrange,
    background = CarpeLinesPurpleDark,
    surface = CarpeLinesSurface,
    onPrimary = CarpeLinesWhite,
    onSecondary = CarpeLinesWhite,
    onTertiary = CarpeLinesWhite,
    onBackground = CarpeLinesWhite,
    onSurface = CarpeLinesLightPurple,
    error = Color(0xFFFF5252),
    onError = CarpeLinesWhite
)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = CarpeLinesWhite,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = CarpeLinesPink,
    onSecondary = CarpeLinesWhite,
    secondaryContainer = CarpeLinesPink.copy(alpha = 0.15f),
    onSecondaryContainer = CarpeLinesPink.copy(alpha = 0.85f),
    tertiary = CarpeLinesOrange,
    onTertiary = CarpeLinesBlack,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.6f),
    surfaceTint = LightPrimary,
    error = Color(0xFFD32F2F),
    onError = CarpeLinesWhite
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CarpeLinesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemePairOverride: ColorSchemePair? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val targetColorScheme = when {
        colorSchemePairOverride == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            remember(context, darkTheme) {
                try {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } catch (e: Exception) {
                    if (darkTheme) DarkColorScheme else LightColorScheme
                }
            }
        }
        colorSchemePairOverride != null -> {
            if (darkTheme) colorSchemePairOverride.dark else colorSchemePairOverride.light
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    // A new album palette used to replace all ~34 roles in a single frame. Blending towards the
    // target keeps the app from flashing white/black between songs and during theme changes.
    val finalColorScheme = rememberAnimatedColorScheme(targetColorScheme)

    CarpeLinesStatusBarStyle(
        // Transparent bars: use the settled palette so the icon brightness does not flicker
        // while the colours are still interpolating.
        color = targetColorScheme.background,
        navigationColor = targetColorScheme.background
    )

    val emphasizedTypography = remember { Typography.toEmphasizedTypography() }

    CompositionLocalProvider(
        LocalCarpeLinesDarkTheme provides darkTheme,
        LocalEmphasizedTypography provides emphasizedTypography,
        LocalReducedMotion provides rememberSystemReducedMotion()
    ) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            // Expressive motion is the M3E default. Swapping this one value moves the whole app
            // between the standard and expressive motion schemes.
            motionScheme = MotionScheme.expressive(),
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
