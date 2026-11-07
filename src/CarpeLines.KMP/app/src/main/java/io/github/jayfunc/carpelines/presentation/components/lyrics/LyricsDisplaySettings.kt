package io.github.jayfunc.carpelines.presentation.components.lyrics

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jayfunc.carpelines.data.preferences.DEFAULT_LYRICS_DUET_LAYOUT_ENABLED
import io.github.jayfunc.carpelines.data.preferences.DEFAULT_LYRICS_FONT_SIZE_SP
import io.github.jayfunc.carpelines.data.preferences.DEFAULT_SHOW_LYRICS_BACKGROUND_VOCALS
import io.github.jayfunc.carpelines.data.preferences.LYRICS_DUET_LAYOUT_KEY
import io.github.jayfunc.carpelines.data.preferences.LYRICS_FONT_SIZE_SP_KEY
import io.github.jayfunc.carpelines.data.preferences.SHOW_LYRICS_BACKGROUND_VOCALS_KEY
import io.github.jayfunc.carpelines.data.preferences.dataStore
import io.github.jayfunc.carpelines.data.preferences.sanitizeLyricsFontSize
import io.github.jayfunc.carpelines.presentation.components.betterlyrics.BetterLyricsAmbientStyle
import kotlinx.coroutines.flow.map

/** The lyric presentation preferences shared by the lyrics page and its settings. */
@Immutable
data class LyricsDisplaySettings(
    val fontSizeSp: Float = DEFAULT_LYRICS_FONT_SIZE_SP,
    val showTranslation: Boolean = true,
    val showRomanization: Boolean = true,
    val duetLayoutEnabled: Boolean = DEFAULT_LYRICS_DUET_LAYOUT_ENABLED,
    val showBackgroundVocals: Boolean = DEFAULT_SHOW_LYRICS_BACKGROUND_VOCALS,
    val animatedLyricsBlurEnabled: Boolean = true,
    val animatedLyricsBlurStrengthDp: Float = WinLyricsVisualDefaults.LINE_BLUR_AMOUNT_DP,
    val playbackSpeed: Float = 1f,
    val ambientEnabled: Boolean = true,
    val ambientStyle: BetterLyricsAmbientStyle = BetterLyricsAmbientStyle.FLUID,
    /** Where the playing line sits in the viewport, 0 = top, 0.5 = middle. */
    val playingLineTopOffset: Float = DEFAULT_PLAYING_LINE_TOP_OFFSET,
    /** Whether the lyrics page hides its player chrome automatically while playing. */
    val immersiveLyricsEnabled: Boolean = false,
    /** Delay before the player chrome hides itself, in milliseconds. */
    val immersiveLyricsTimeoutMs: Long = DEFAULT_IMMERSIVE_LYRICS_TIMEOUT_MS,
    /** Style knobs that map onto accompanist-lyrics-ui text styles and spacing. */
    val visual: LyricsVisualSettings = LyricsVisualSettings(),
)

/**
 * Style knobs shared by the immersive page and the settings preview. They map onto
 * `KaraokeLyricsView` text styles, list spacing, edge fades, gap dots and follow motion.
 */
@Immutable
data class LyricsVisualSettings(
    /** `LyricsLineOverallSpacingFactor`, mapped onto the list's item spacing. */
    val lineSpacingFactor: Float = DEFAULT_LINE_SPACING_FACTOR,
    /** Multiplier over the base style's line-height ratio. */
    val lineHeightScale: Float = DEFAULT_LYRICS_LINE_HEIGHT_SCALE,
    /** `KaraokeLyricsView.textColor`: artwork-derived, theme, or a fixed light/dark colour. */
    val textColorMode: LyricsTextColorMode = LyricsTextColorMode.DEFAULT,
    /** `KaraokeLyricsView.renderProfiles`: script-aware syllables or explicit word units. */
    val segmentation: LyricsSegmentation = LyricsSegmentation.DEFAULT,
    /** `ProfileGroupEffects.scale`: long syllables swell while they are held. */
    val scaleEffectEnabled: Boolean = true,
    /** `ProfileGroupEffects.glow`: long syllables bloom while they are held. */
    val glowEffectEnabled: Boolean = true,
    /** `ProfileGroupEffects.lift`: unsung syllables wait low and rise as they start. */
    val floatEffectEnabled: Boolean = true,
    /** `LyricsLayoutHelper`: translation 0.70 of the primary size, transliteration 0.55. */
    val translationFontRatio: Float = WinLyricsVisualDefaults.TRANSLATION_FONT_RATIO,
    val romanizationFontRatio: Float = WinLyricsVisualDefaults.ROMANIZATION_FONT_RATIO,
    /** `accompanimentLineTextStyle` in the karaoke view, relative to the primary size. */
    val backgroundVocalFontRatio: Float = DEFAULT_BACKGROUND_VOCAL_FONT_RATIO,
    /** `IsLyricsEdgeFeatheringEffectEnabled`: top/bottom feather mask. */
    val edgeFeatheringEffectEnabled: Boolean = true,
    /** How far the two edge fades reach towards the playing line; 1 is the full split. */
    val edgeFadeReach: Float = DEFAULT_EDGE_FADE_REACH,
    /** `LyricsFade`: how each edge mask is measured. `AUTO` splits [edgeFadeReach] at the playing line. */
    val edgeFadeMode: LyricsFadeMode = LyricsFadeMode.DEFAULT,
    /** `LyricsFade.Fraction` lengths, used when [edgeFadeMode] is `FRACTION`. */
    val topFadeFraction: Float = DEFAULT_TOP_FADE_FRACTION,
    val bottomFadeFraction: Float = DEFAULT_BOTTOM_FADE_FRACTION,
    /** `LyricsFade.Fixed` / `LyricsFade.ToAnchor` lengths, used by the matching modes. */
    val topFadeDp: Float = DEFAULT_TOP_FADE_DP,
    val bottomFadeDp: Float = DEFAULT_BOTTOM_FADE_DP,
    /** `KaraokeLyricsView.keepAliveZone`: off-screen distance kept composed and ready to draw. */
    val keepAliveZoneDp: Float = DEFAULT_KEEP_ALIVE_ZONE_DP,
    /** `KaraokeLyricsView.blendMode`: how the lyrics composite over the background. */
    val blendMode: LyricsBlendMode = LyricsBlendMode.DEFAULT,
    /** `LyricsAnchor`: where the playing line sits. `FIXED` uses [anchorOffsetDp]. */
    val anchorMode: LyricsAnchorMode = LyricsAnchorMode.DEFAULT,
    val anchorOffsetDp: Float = DEFAULT_ANCHOR_OFFSET_DP,
    /** `KaraokeBreathingDotsDefaults.number`, `size` and `margin`. */
    val gapDotsCount: Int = DEFAULT_GAP_DOTS_COUNT,
    val gapDotsSizeDp: Float = DEFAULT_GAP_DOTS_SIZE_DP,
    val gapDotsSpacingDp: Float = DEFAULT_GAP_DOTS_SPACING_DP,
    /** `KaraokeBreathingDotsDefaults` phase budgets, scaled down to the break length. */
    val gapDotsEnterDurationMs: Int = DEFAULT_GAP_DOTS_ENTER_MS,
    val gapDotsHoldDurationMs: Int = DEFAULT_GAP_DOTS_HOLD_MS,
    val gapDotsPulseDurationMs: Int = DEFAULT_GAP_DOTS_PULSE_MS,
    val gapDotsExitDurationMs: Int = DEFAULT_GAP_DOTS_EXIT_MS,
    /** `KaraokeBreathingDotsDefaults.breathingDotsColor`: artwork accent instead of the lyric colour. */
    val gapDotsUseAccentColor: Boolean = false,
    /** `KaraokeLyricsView` follow motion: leading tween plus the trailing spring chain. */
    val followScrollDurationMs: Int = DEFAULT_FOLLOW_SCROLL_DURATION_MS,
    val scrollResumeDelayMs: Long = DEFAULT_AUTO_SCROLL_RESUME_DELAY_MS,
    val scrollEasing: LyricsScrollEasing = LyricsScrollEasing.DEFAULT,
    val followSpringStiffness: Float = DEFAULT_FOLLOW_SPRING_STIFFNESS,
    val followSpringDamping: Float = DEFAULT_FOLLOW_SPRING_DAMPING,
    val followSpringCoupling: Float = DEFAULT_FOLLOW_SPRING_COUPLING,
    val followSpringDistanceFalloff: Float = DEFAULT_FOLLOW_SPRING_DISTANCE_FALLOFF,
    val followSpringMinResponse: Float = DEFAULT_FOLLOW_SPRING_MIN_RESPONSE,
    /** `LyricsFontWeight` in WinUI: thin through extra black. */
    val fontWeight: LyricsFontWeight = LyricsFontWeight.DEFAULT,
    /** `LyricsCJKFontFamily` / `LyricsWesternFontFamily`, reduced to the families Android offers. */
    val fontFamily: LyricsFontFamily = LyricsFontFamily.DEFAULT,
    /** Where every lyric line sits horizontally. */
    val alignment: LyricsAlignment = LyricsAlignment.DEFAULT,
)

/**
 * The horizontal alignment of a lyric line. Only leading and trailing exist: the karaoke
 * renderer lays each line out against one of the two edges (see `KaraokeAlignment`), so a
 * centred option could not be honoured for synced lyrics and is deliberately absent.
 */
enum class LyricsAlignment(val storageValue: String) {
    LEFT("left"),
    RIGHT("right");

    fun toTextAlign(): TextAlign = when (this) {
        LEFT -> TextAlign.Left
        RIGHT -> TextAlign.Right
    }

    companion object {
        val DEFAULT = LEFT

        fun fromStorage(value: String?): LyricsAlignment =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/**
 * `LyricsFontWeight` from BetterLyrics. Android exposes the same numeric weight axis, including
 * the two steps Windows names but Compose does not (semi-light at 350, extra-black at 950).
 */
enum class LyricsFontWeight(val storageValue: String, val weight: FontWeight) {
    THIN("thin", FontWeight.Thin),
    EXTRA_LIGHT("extra_light", FontWeight.ExtraLight),
    LIGHT("light", FontWeight.Light),
    SEMI_LIGHT("semi_light", FontWeight(350)),
    NORMAL("normal", FontWeight.Normal),
    MEDIUM("medium", FontWeight.Medium),
    SEMI_BOLD("semi_bold", FontWeight.SemiBold),
    BOLD("bold", FontWeight.Bold),
    EXTRA_BOLD("extra_bold", FontWeight.ExtraBold),
    BLACK("black", FontWeight.Black),
    EXTRA_BLACK("extra_black", FontWeight(950));

    companion object {
        val DEFAULT = BOLD

        fun fromStorage(value: String?): LyricsFontWeight =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/**
 * The family choices Android can resolve for any text without shipping a font file. `DEFAULT`
 * keeps whatever family the app theme already chose, so the page continues to follow the
 * product typeface until the user opts into something else.
 */
enum class LyricsFontFamily(val storageValue: String) {
    DEFAULT("default"),
    SANS_SERIF("sans_serif"),
    SERIF("serif"),
    MONOSPACE("monospace"),
    CURSIVE("cursive");

    fun resolve(fallback: FontFamily?): FontFamily = when (this) {
        DEFAULT -> fallback ?: FontFamily.Default
        SANS_SERIF -> FontFamily.SansSerif
        SERIF -> FontFamily.Serif
        MONOSPACE -> FontFamily.Monospace
        CURSIVE -> FontFamily.Cursive
    }

    companion object {
        fun fromStorage(value: String?): LyricsFontFamily =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/**
 * `KaraokeLyricsView.textColor`. `ARTWORK` tints the words with the song's harmonised palette
 * (the same seed the app scheme uses), `NEUTRAL` follows the page polarity without a hue, and the
 * remaining modes pin a fixed reading colour. Stored `"theme"` from older builds falls through to
 * the default because the app theme is itself cover-derived.
 */
enum class LyricsTextColorMode(val storageValue: String) {
    ARTWORK("artwork"),
    NEUTRAL("neutral"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        val DEFAULT = ARTWORK

        fun fromStorage(value: String?): LyricsTextColorMode =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/**
 * `KaraokeLyricsView.renderProfiles`. `AUTO` keeps the library's script-aware profiles, which
 * animate CJK by grapheme and Latin by syllable. `WORD` replaces them with `WordLevelLyricsProfile`
 * so every script animates in whole source syllables.
 */
enum class LyricsSegmentation(val storageValue: String) {
    AUTO("auto"),
    WORD("word");

    companion object {
        val DEFAULT = AUTO

        fun fromStorage(value: String?): LyricsSegmentation =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/** `LyricsAnchor`: the playing line is placed by viewport fraction or by a fixed top offset. */
enum class LyricsAnchorMode(val storageValue: String) {
    FRACTION("fraction"),
    FIXED("fixed");

    companion object {
        val DEFAULT = FRACTION

        fun fromStorage(value: String?): LyricsAnchorMode =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/**
 * `LyricsFade`. `AUTO` keeps the split-at-the-playing-line behaviour driven by `edgeFadeReach`;
 * the other three map directly onto the library's `Fraction`, `Fixed` and `ToAnchor` variants.
 */
enum class LyricsFadeMode(val storageValue: String) {
    AUTO("auto"),
    FRACTION("fraction"),
    FIXED("fixed"),
    TO_ANCHOR("to_anchor");

    val usesFraction: Boolean get() = this == FRACTION
    val usesDp: Boolean get() = this == FIXED || this == TO_ANCHOR

    companion object {
        val DEFAULT = AUTO

        fun fromStorage(value: String?): LyricsFadeMode =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/** `KaraokeLyricsView.blendMode`, limited to the compositing modes that read well over artwork. */
enum class LyricsBlendMode(val storageValue: String) {
    NORMAL("normal"),
    PLUS("plus"),
    SCREEN("screen");

    companion object {
        val DEFAULT = NORMAL

        fun fromStorage(value: String?): LyricsBlendMode =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/** The easing applied to the leading item of the follow scroll. */
enum class LyricsScrollEasing(val storageValue: String) {
    STANDARD("standard"),
    LINEAR("linear"),
    EASE_IN_OUT("ease_in_out"),
    DECELERATE("decelerate");

    fun resolve(): Easing = when (this) {
        STANDARD -> FastOutSlowInEasing
        LINEAR -> LinearEasing
        EASE_IN_OUT -> CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
        DECELERATE -> LinearOutSlowInEasing
    }

    companion object {
        val DEFAULT = STANDARD

        fun fromStorage(value: String?): LyricsScrollEasing =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/** WinUI centres the playing line, and so does the immersive page by default. */
const val DEFAULT_PLAYING_LINE_TOP_OFFSET = 0.5f

/** BetterLyrics defaults, expressed as fractions instead of percentages. */
const val DEFAULT_LINE_SPACING_FACTOR = 0.5f

/** Multiplier over the base typography's line-height ratio. */
const val DEFAULT_LYRICS_LINE_HEIGHT_SCALE = 1f

/** `KaraokeLyricsView` defaults for backing vocals, gap dots and follow motion. */
const val DEFAULT_BACKGROUND_VOCAL_FONT_RATIO = 0.8f
const val DEFAULT_GAP_DOTS_COUNT = 3
const val DEFAULT_GAP_DOTS_SIZE_DP = 16f
const val DEFAULT_GAP_DOTS_SPACING_DP = 12f
const val DEFAULT_EDGE_FADE_REACH = 1f
const val DEFAULT_FOLLOW_SCROLL_DURATION_MS = 650
const val DEFAULT_AUTO_SCROLL_RESUME_DELAY_MS = 2_500L
const val DEFAULT_FOLLOW_SPRING_STIFFNESS = 100f
const val DEFAULT_FOLLOW_SPRING_DAMPING = 12f
const val DEFAULT_FOLLOW_SPRING_COUPLING = 0.65f
const val DEFAULT_FOLLOW_SPRING_DISTANCE_FALLOFF = 0.25f
const val DEFAULT_FOLLOW_SPRING_MIN_RESPONSE = 0.35f

/** `KaraokeLyricsView` defaults for the parts the settings page can override. */
const val DEFAULT_TOP_FADE_FRACTION = 0.5f
const val DEFAULT_BOTTOM_FADE_FRACTION = 0.5f
const val DEFAULT_TOP_FADE_DP = 16f
const val DEFAULT_BOTTOM_FADE_DP = 64f
const val DEFAULT_KEEP_ALIVE_ZONE_DP = 100f
const val DEFAULT_ANCHOR_OFFSET_DP = 64f

/** `KaraokeBreathingDotsDefaults` phase budgets. */
const val DEFAULT_GAP_DOTS_ENTER_MS = 3_000
const val DEFAULT_GAP_DOTS_HOLD_MS = 200
const val DEFAULT_GAP_DOTS_PULSE_MS = 3_000
const val DEFAULT_GAP_DOTS_EXIT_MS = 200

/** Matches UserPreferencesRepository's default for the immersive-lyrics timeout. */
const val DEFAULT_IMMERSIVE_LYRICS_TIMEOUT_MS = 4_000L

val AMBIENT_ENABLED_KEY = booleanPreferencesKey("betterlyrics_ambient_enabled")
val AMBIENT_STYLE_KEY = stringPreferencesKey("betterlyrics_ambient_style")

val LYRICS_PLAYING_LINE_OFFSET_KEY = floatPreferencesKey("lyrics_playing_line_offset")
val IMMERSIVE_LYRICS_ENABLED_KEY = booleanPreferencesKey("immersive_lyrics_enabled")
val IMMERSIVE_LYRICS_TIMEOUT_KEY = longPreferencesKey("immersive_lyrics_timeout")

val SHOW_LYRICS_TRANSLATION_KEY = booleanPreferencesKey("show_lyrics_translation")
val SHOW_LYRICS_ROMANIZATION_KEY = booleanPreferencesKey("show_lyrics_romanization")
val LYRICS_LINE_SPACING_KEY = floatPreferencesKey("lyrics_line_spacing_factor")
val LYRICS_LINE_HEIGHT_SCALE_KEY = floatPreferencesKey("lyrics_line_height_scale")
val LYRICS_TRANSLATION_FONT_RATIO_KEY = floatPreferencesKey("lyrics_translation_font_ratio")
val LYRICS_ROMANIZATION_FONT_RATIO_KEY = floatPreferencesKey("lyrics_romanization_font_ratio")
val LYRICS_BACKGROUND_VOCAL_FONT_RATIO_KEY =
    floatPreferencesKey("lyrics_background_vocal_font_ratio")
val LYRICS_EDGE_FEATHERING_KEY = booleanPreferencesKey("lyrics_edge_feathering_enabled")
val LYRICS_EDGE_FADE_REACH_KEY = floatPreferencesKey("lyrics_edge_fade_reach")
val LYRICS_GAP_DOTS_COUNT_KEY = intPreferencesKey("lyrics_gap_dots_count")
val LYRICS_GAP_DOTS_SIZE_KEY = floatPreferencesKey("lyrics_gap_dots_size_dp")
val LYRICS_GAP_DOTS_SPACING_KEY = floatPreferencesKey("lyrics_gap_dots_spacing_dp")
val LYRICS_FOLLOW_SCROLL_DURATION_KEY = intPreferencesKey("lyrics_follow_scroll_duration_ms")
val LYRICS_SCROLL_RESUME_DELAY_KEY = longPreferencesKey("lyrics_scroll_resume_delay_ms")
val LYRICS_FOLLOW_SPRING_STIFFNESS_KEY = floatPreferencesKey("lyrics_follow_spring_stiffness")
val LYRICS_FOLLOW_SPRING_DAMPING_KEY = floatPreferencesKey("lyrics_follow_spring_damping")
val LYRICS_FONT_WEIGHT_KEY = stringPreferencesKey("lyrics_font_weight")
val LYRICS_FONT_FAMILY_KEY = stringPreferencesKey("lyrics_font_family")
val LYRICS_ALIGNMENT_KEY = stringPreferencesKey("lyrics_alignment")
val LYRICS_SEGMENTATION_KEY = stringPreferencesKey("lyrics_segmentation")
val LYRICS_TEXT_COLOR_MODE_KEY = stringPreferencesKey("lyrics_text_color_mode")
val LYRICS_SCALE_EFFECT_KEY = booleanPreferencesKey("lyrics_scale_effect_enabled")
val LYRICS_GLOW_EFFECT_KEY = booleanPreferencesKey("lyrics_glow_effect_enabled")
val LYRICS_FLOAT_EFFECT_KEY = booleanPreferencesKey("lyrics_float_effect_enabled")
val LYRICS_EDGE_FADE_MODE_KEY = stringPreferencesKey("lyrics_edge_fade_mode")
val LYRICS_TOP_FADE_FRACTION_KEY = floatPreferencesKey("lyrics_top_fade_fraction")
val LYRICS_BOTTOM_FADE_FRACTION_KEY = floatPreferencesKey("lyrics_bottom_fade_fraction")
val LYRICS_TOP_FADE_DP_KEY = floatPreferencesKey("lyrics_top_fade_dp")
val LYRICS_BOTTOM_FADE_DP_KEY = floatPreferencesKey("lyrics_bottom_fade_dp")
val LYRICS_KEEP_ALIVE_ZONE_KEY = floatPreferencesKey("lyrics_keep_alive_zone_dp")
val LYRICS_BLEND_MODE_KEY = stringPreferencesKey("lyrics_blend_mode")
val LYRICS_ANCHOR_MODE_KEY = stringPreferencesKey("lyrics_anchor_mode")
val LYRICS_ANCHOR_OFFSET_DP_KEY = floatPreferencesKey("lyrics_anchor_offset_dp")
val LYRICS_GAP_DOTS_ENTER_MS_KEY = intPreferencesKey("lyrics_gap_dots_enter_ms")
val LYRICS_GAP_DOTS_HOLD_MS_KEY = intPreferencesKey("lyrics_gap_dots_hold_ms")
val LYRICS_GAP_DOTS_PULSE_MS_KEY = intPreferencesKey("lyrics_gap_dots_pulse_ms")
val LYRICS_GAP_DOTS_EXIT_MS_KEY = intPreferencesKey("lyrics_gap_dots_exit_ms")
val LYRICS_GAP_DOTS_ACCENT_COLOR_KEY = booleanPreferencesKey("lyrics_gap_dots_accent_color")
val LYRICS_SCROLL_EASING_KEY = stringPreferencesKey("lyrics_scroll_easing")
val LYRICS_FOLLOW_SPRING_COUPLING_KEY = floatPreferencesKey("lyrics_follow_spring_coupling")
val LYRICS_FOLLOW_SPRING_DISTANCE_FALLOFF_KEY =
    floatPreferencesKey("lyrics_follow_spring_distance_falloff")
val LYRICS_FOLLOW_SPRING_MIN_RESPONSE_KEY =
    floatPreferencesKey("lyrics_follow_spring_min_response")

private const val DEFAULT_LYRICS_LINE_HEIGHT_RATIO = 1.18f

@Composable
fun rememberLyricsDisplaySettings(): LyricsDisplaySettings {
    val context = LocalContext.current
    val flow = remember(context) {
        context.dataStore.data.map { preferences ->
            LyricsDisplaySettings(
                fontSizeSp = sanitizeLyricsFontSize(
                    preferences[LYRICS_FONT_SIZE_SP_KEY] ?: DEFAULT_LYRICS_FONT_SIZE_SP
                ),
                showTranslation = preferences[SHOW_LYRICS_TRANSLATION_KEY] ?: true,
                showRomanization = preferences[SHOW_LYRICS_ROMANIZATION_KEY] ?: true,
                duetLayoutEnabled = preferences[LYRICS_DUET_LAYOUT_KEY]
                    ?: DEFAULT_LYRICS_DUET_LAYOUT_ENABLED,
                showBackgroundVocals = preferences[SHOW_LYRICS_BACKGROUND_VOCALS_KEY]
                    ?: DEFAULT_SHOW_LYRICS_BACKGROUND_VOCALS,
                animatedLyricsBlurEnabled =
                    preferences[booleanPreferencesKey("animated_lyrics_blur_enabled")] ?: true,
                animatedLyricsBlurStrengthDp =
                    (preferences[floatPreferencesKey("animated_lyrics_blur_strength")]
                        ?: WinLyricsVisualDefaults.LINE_BLUR_AMOUNT_DP).coerceIn(0.1f, 8f),
                playbackSpeed = preferences[floatPreferencesKey("playback_speed")] ?: 1f,
                ambientEnabled = preferences[AMBIENT_ENABLED_KEY] ?: true,
                ambientStyle = BetterLyricsAmbientStyle.fromStorage(
                    preferences[AMBIENT_STYLE_KEY]
                ),
                playingLineTopOffset = (preferences[LYRICS_PLAYING_LINE_OFFSET_KEY]
                    ?: DEFAULT_PLAYING_LINE_TOP_OFFSET).coerceIn(0.2f, 0.6f),
                immersiveLyricsEnabled = preferences[IMMERSIVE_LYRICS_ENABLED_KEY] ?: false,
                immersiveLyricsTimeoutMs = preferences[IMMERSIVE_LYRICS_TIMEOUT_KEY]
                    ?: DEFAULT_IMMERSIVE_LYRICS_TIMEOUT_MS,
                visual = LyricsVisualSettings(
                    lineSpacingFactor = (preferences[LYRICS_LINE_SPACING_KEY]
                        ?: DEFAULT_LINE_SPACING_FACTOR).coerceIn(0f, 1.5f),
                    lineHeightScale = (preferences[LYRICS_LINE_HEIGHT_SCALE_KEY]
                        ?: DEFAULT_LYRICS_LINE_HEIGHT_SCALE).coerceIn(0.8f, 1.6f),
                    translationFontRatio = (preferences[LYRICS_TRANSLATION_FONT_RATIO_KEY]
                        ?: WinLyricsVisualDefaults.TRANSLATION_FONT_RATIO).coerceIn(0.4f, 1f),
                    romanizationFontRatio = (preferences[LYRICS_ROMANIZATION_FONT_RATIO_KEY]
                        ?: WinLyricsVisualDefaults.ROMANIZATION_FONT_RATIO).coerceIn(0.4f, 1f),
                    backgroundVocalFontRatio = (preferences[LYRICS_BACKGROUND_VOCAL_FONT_RATIO_KEY]
                        ?: DEFAULT_BACKGROUND_VOCAL_FONT_RATIO).coerceIn(0.4f, 1f),
                    edgeFeatheringEffectEnabled = preferences[LYRICS_EDGE_FEATHERING_KEY] ?: true,
                    edgeFadeReach = (preferences[LYRICS_EDGE_FADE_REACH_KEY]
                        ?: DEFAULT_EDGE_FADE_REACH).coerceIn(0f, 1f),
                    edgeFadeMode = LyricsFadeMode.fromStorage(
                        preferences[LYRICS_EDGE_FADE_MODE_KEY]
                    ),
                    topFadeFraction = (preferences[LYRICS_TOP_FADE_FRACTION_KEY]
                        ?: DEFAULT_TOP_FADE_FRACTION).coerceIn(0f, 1f),
                    bottomFadeFraction = (preferences[LYRICS_BOTTOM_FADE_FRACTION_KEY]
                        ?: DEFAULT_BOTTOM_FADE_FRACTION).coerceIn(0f, 1f),
                    topFadeDp = (preferences[LYRICS_TOP_FADE_DP_KEY]
                        ?: DEFAULT_TOP_FADE_DP).coerceIn(0f, 240f),
                    bottomFadeDp = (preferences[LYRICS_BOTTOM_FADE_DP_KEY]
                        ?: DEFAULT_BOTTOM_FADE_DP).coerceIn(0f, 240f),
                    keepAliveZoneDp = (preferences[LYRICS_KEEP_ALIVE_ZONE_KEY]
                        ?: DEFAULT_KEEP_ALIVE_ZONE_DP).coerceIn(0f, 240f),
                    blendMode = LyricsBlendMode.fromStorage(
                        preferences[LYRICS_BLEND_MODE_KEY]
                    ),
                    anchorMode = LyricsAnchorMode.fromStorage(
                        preferences[LYRICS_ANCHOR_MODE_KEY]
                    ),
                    anchorOffsetDp = (preferences[LYRICS_ANCHOR_OFFSET_DP_KEY]
                        ?: DEFAULT_ANCHOR_OFFSET_DP).coerceIn(0f, 240f),
                    segmentation = LyricsSegmentation.fromStorage(
                        preferences[LYRICS_SEGMENTATION_KEY]
                    ),
                    textColorMode = LyricsTextColorMode.fromStorage(
                        preferences[LYRICS_TEXT_COLOR_MODE_KEY]
                    ),
                    scaleEffectEnabled = preferences[LYRICS_SCALE_EFFECT_KEY] ?: true,
                    glowEffectEnabled = preferences[LYRICS_GLOW_EFFECT_KEY] ?: true,
                    floatEffectEnabled = preferences[LYRICS_FLOAT_EFFECT_KEY] ?: true,
                    gapDotsCount = (preferences[LYRICS_GAP_DOTS_COUNT_KEY]
                        ?: DEFAULT_GAP_DOTS_COUNT).coerceIn(1, 5),
                    gapDotsSizeDp = (preferences[LYRICS_GAP_DOTS_SIZE_KEY]
                        ?: DEFAULT_GAP_DOTS_SIZE_DP).coerceIn(8f, 24f),
                    gapDotsSpacingDp = (preferences[LYRICS_GAP_DOTS_SPACING_KEY]
                        ?: DEFAULT_GAP_DOTS_SPACING_DP).coerceIn(4f, 24f),
                    gapDotsEnterDurationMs = (preferences[LYRICS_GAP_DOTS_ENTER_MS_KEY]
                        ?: DEFAULT_GAP_DOTS_ENTER_MS).coerceIn(0, 5_000),
                    gapDotsHoldDurationMs = (preferences[LYRICS_GAP_DOTS_HOLD_MS_KEY]
                        ?: DEFAULT_GAP_DOTS_HOLD_MS).coerceIn(0, 1_500),
                    gapDotsPulseDurationMs = (preferences[LYRICS_GAP_DOTS_PULSE_MS_KEY]
                        ?: DEFAULT_GAP_DOTS_PULSE_MS).coerceIn(0, 6_000),
                    gapDotsExitDurationMs = (preferences[LYRICS_GAP_DOTS_EXIT_MS_KEY]
                        ?: DEFAULT_GAP_DOTS_EXIT_MS).coerceIn(0, 1_000),
                    gapDotsUseAccentColor = preferences[LYRICS_GAP_DOTS_ACCENT_COLOR_KEY] ?: false,
                    followScrollDurationMs = (preferences[LYRICS_FOLLOW_SCROLL_DURATION_KEY]
                        ?: DEFAULT_FOLLOW_SCROLL_DURATION_MS).coerceIn(0, 1_200),
                    scrollResumeDelayMs = (preferences[LYRICS_SCROLL_RESUME_DELAY_KEY]
                        ?: DEFAULT_AUTO_SCROLL_RESUME_DELAY_MS).coerceIn(0L, 5_000L),
                    scrollEasing = LyricsScrollEasing.fromStorage(
                        preferences[LYRICS_SCROLL_EASING_KEY]
                    ),
                    followSpringStiffness = (preferences[LYRICS_FOLLOW_SPRING_STIFFNESS_KEY]
                        ?: DEFAULT_FOLLOW_SPRING_STIFFNESS).coerceIn(20f, 400f),
                    followSpringDamping = (preferences[LYRICS_FOLLOW_SPRING_DAMPING_KEY]
                        ?: DEFAULT_FOLLOW_SPRING_DAMPING).coerceIn(4f, 40f),
                    followSpringCoupling = (preferences[LYRICS_FOLLOW_SPRING_COUPLING_KEY]
                        ?: DEFAULT_FOLLOW_SPRING_COUPLING).coerceIn(0f, 0.95f),
                    followSpringDistanceFalloff =
                        (preferences[LYRICS_FOLLOW_SPRING_DISTANCE_FALLOFF_KEY]
                            ?: DEFAULT_FOLLOW_SPRING_DISTANCE_FALLOFF).coerceIn(0f, 1f),
                    followSpringMinResponse = (preferences[LYRICS_FOLLOW_SPRING_MIN_RESPONSE_KEY]
                        ?: DEFAULT_FOLLOW_SPRING_MIN_RESPONSE).coerceIn(0.05f, 1f),
                    fontWeight = LyricsFontWeight.fromStorage(
                        preferences[LYRICS_FONT_WEIGHT_KEY]
                    ),
                    fontFamily = LyricsFontFamily.fromStorage(
                        preferences[LYRICS_FONT_FAMILY_KEY]
                    ),
                    alignment = LyricsAlignment.fromStorage(
                        preferences[LYRICS_ALIGNMENT_KEY]
                    ),
                ),
            )
        }
    }
    val settings by flow.collectAsStateWithLifecycle(initialValue = LyricsDisplaySettings())
    return settings
}

/**
 * Applies the user's lyrics font size while keeping the base style's line-height ratio, scaled by
 * the optional line-height multiplier.
 */
fun TextStyle.scaledForLyrics(
    fontSizeSp: Float,
    lineHeightScale: Float = DEFAULT_LYRICS_LINE_HEIGHT_SCALE,
): TextStyle {
    val ratio = if (fontSize.isSp && lineHeight.isSp && fontSize.value > 0f) {
        lineHeight.value / fontSize.value
    } else {
        DEFAULT_LYRICS_LINE_HEIGHT_RATIO
    }
    return copy(
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * ratio * lineHeightScale).sp,
    )
}
