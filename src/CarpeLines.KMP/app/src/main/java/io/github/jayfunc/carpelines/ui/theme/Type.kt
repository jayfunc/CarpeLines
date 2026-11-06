package io.github.jayfunc.carpelines.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.jayfunc.carpelines.R

/**
 * The variable rounded sans the whole product is set in. It carries the full weight axis, so
 * M3 Expressive's emphasised roles can be produced by re-weighting the same family instead of
 * introducing a second typeface.
 */
@OptIn(ExperimentalTextApi::class)
val RoundedSans = FontFamily(
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Light.weight),
            FontVariation.Setting("ROND", RoundedSansRoundness)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Normal.weight),
            FontVariation.Setting("ROND", RoundedSansRoundness)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Medium.weight),
            FontVariation.Setting("ROND", RoundedSansRoundness)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.SemiBold.weight),
            FontVariation.Setting("ROND", RoundedSansRoundness)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Bold.weight),
            FontVariation.Setting("ROND", RoundedSansRoundness)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.ExtraBold.weight),
            FontVariation.Setting("ROND", RoundedSansRoundness)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Black.weight),
            FontVariation.Setting("ROND", RoundedSansRoundness)
        )
    ),
)

private const val RoundedSansRoundness = 100f

/**
 * M3 type scale.
 *
 * Sizes, line heights and tracking are the Material 3 values; the only product decision here is
 * the typeface. Every screen should read a **role** from `MaterialTheme.typography` rather than
 * setting `fontSize`/`fontWeight` inline, so hierarchy survives text scaling and future theme
 * changes.
 *
 * Emphasis is not a separate scale: see [EmphasizedTypography].
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * M3 Expressive emphasised type roles.
 *
 * An emphasised role has the **same metrics** as its standard counterpart and only a heavier
 * weight, so swapping one for the other never reflows a layout — it only changes emphasis. Use
 * it for the one element per screen that should win attention; if everything is emphasised,
 * nothing is.
 */
@Immutable
data class EmphasizedTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

/** Emphasising a role raises its weight by one step and leaves every metric untouched. */
fun TextStyle.emphasized(): TextStyle = copy(fontWeight = fontWeight?.emphasize())

private fun FontWeight.emphasize(): FontWeight = when (weight) {
    in 0..299 -> FontWeight.Normal
    in 300..399 -> FontWeight.Medium
    in 400..499 -> FontWeight.SemiBold
    in 500..599 -> FontWeight.Bold
    in 600..699 -> FontWeight.ExtraBold
    else -> FontWeight.Black
}

internal fun Typography.toEmphasizedTypography(): EmphasizedTypography = EmphasizedTypography(
    displayLarge = displayLarge.emphasized(),
    displayMedium = displayMedium.emphasized(),
    displaySmall = displaySmall.emphasized(),
    headlineLarge = headlineLarge.emphasized(),
    headlineMedium = headlineMedium.emphasized(),
    headlineSmall = headlineSmall.emphasized(),
    titleLarge = titleLarge.emphasized(),
    titleMedium = titleMedium.emphasized(),
    titleSmall = titleSmall.emphasized(),
    bodyLarge = bodyLarge.emphasized(),
    bodyMedium = bodyMedium.emphasized(),
    bodySmall = bodySmall.emphasized(),
    labelLarge = labelLarge.emphasized(),
    labelMedium = labelMedium.emphasized(),
    labelSmall = labelSmall.emphasized()
)

/**
 * Emphasised roles for the active theme.
 *
 * ```kotlin
 * Text(
 *     text = title,
 *     style = MaterialTheme.typography.emphasized.headlineSmall,
 * )
 * ```
 *
 * The default is derived from [Typography] rather than throwing, so previews and tests that
 * compose a component without the app theme still render instead of crashing.
 */
private val DefaultEmphasizedTypography: EmphasizedTypography by lazy {
    Typography.toEmphasizedTypography()
}

val LocalEmphasizedTypography = staticCompositionLocalOf { DefaultEmphasizedTypography }

val Typography.emphasized: EmphasizedTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalEmphasizedTypography.current
