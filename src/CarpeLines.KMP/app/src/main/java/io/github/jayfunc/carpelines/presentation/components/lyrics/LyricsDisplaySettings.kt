package io.github.jayfunc.carpelines.presentation.components.lyrics

import android.content.Context
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
import androidx.datastore.preferences.core.edit
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
import io.github.jayfunc.carpelines.presentation.components.betterlyrics.SPECTRUM_BAR_COUNTS
import io.github.jayfunc.carpelines.presentation.components.betterlyrics.SpectrumPlacement
import io.github.jayfunc.carpelines.presentation.components.betterlyrics.SpectrumStyle
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
    /** Live audio spectrum drawn behind the lyrics; opt-in because it reads the playing audio. */
    val spectrumEnabled: Boolean = false,
    val spectrumStyle: SpectrumStyle = SpectrumStyle.CURVE,
    val spectrumPlacement: SpectrumPlacement = SpectrumPlacement.BOTTOM,
    val spectrumBarCount: Int = 32,
    val spectrumSensitivity: Float = 1f,
    val spectrumOpacity: Float = 0.9f,
    val spectrumGlowEnabled: Boolean = true,
    val spectrumBreathingEnabled: Boolean = true,
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
 * The lyric style knobs that are worth exposing. Everything else the renderer can tune
 * (edge fade measurement, gap-dot phases, follow-spring constants, blur strength, anchor maths)
 * is pinned to [LyricsRenderDefaults] and is deliberately not a preference: those values have no
 * user-facing "right answer" and only made the settings page unreadable.
 */
@Immutable
data class LyricsVisualSettings(
    /** Extra space between lyric lines, as a fraction of the lyric font size. */
    val lineSpacingFactor: Float = DEFAULT_LINE_SPACING_FACTOR,
    /** `KaraokeLyricsView.textColor`: artwork-derived, theme, or a fixed light/dark colour. */
    val textColorMode: LyricsTextColorMode = LyricsTextColorMode.DEFAULT,
    /** `ProfileGroupEffects.scale`: long syllables swell while they are held. */
    val scaleEffectEnabled: Boolean = true,
    /** `ProfileGroupEffects.glow`: long syllables bloom while they are held. */
    val glowEffectEnabled: Boolean = true,
    /** `ProfileGroupEffects.lift`: unsung syllables wait low and rise as they start. */
    val floatEffectEnabled: Boolean = true,
    /**
     * Annotation size relative to the primary lyric. Translations use it directly; the phonetic
     * and backing-vocal layers follow at the ratios the WinUI layout helper prescribes, so one
     * slider keeps the three layers in proportion.
     */
    val annotationFontRatio: Float = DEFAULT_ANNOTATION_FONT_RATIO,
    /** `IsLyricsEdgeFeatheringEffectEnabled`: top/bottom feather mask. */
    val edgeFeatheringEffectEnabled: Boolean = true,
    /** How far the two edge fades reach towards the playing line; 1 is the full split. */
    val edgeFadeReach: Float = DEFAULT_EDGE_FADE_REACH,
    /** `LyricsFontWeight` in WinUI, reduced to the three weights that read differently. */
    val fontWeight: LyricsFontWeight = LyricsFontWeight.DEFAULT,
    /** `LyricsCJKFontFamily` / `LyricsWesternFontFamily`, reduced to the families Android offers. */
    val fontFamily: LyricsFontFamily = LyricsFontFamily.DEFAULT,
    /** Where every lyric line sits horizontally. */
    val alignment: LyricsAlignment = LyricsAlignment.DEFAULT,
    /** Leading tween of the follow scroll. */
    val followScrollDurationMs: Int = DEFAULT_FOLLOW_SCROLL_DURATION_MS,
    /** Delay before auto-scroll resumes after the user dragged the list. */
    val scrollResumeDelayMs: Long = DEFAULT_AUTO_SCROLL_RESUME_DELAY_MS,
) {
    /** Translation size relative to the primary lyric. */
    val translationFontRatio: Float get() = annotationFontRatio

    /** Phonetic size, kept at the WinUI ratio against the translation size. */
    val romanizationFontRatio: Float
        get() = (annotationFontRatio * ROMANIZATION_FONT_SCALE).coerceAtMost(1f)

    /** Backing vocals sit slightly above the translation size, as in WinUI. */
    val backgroundVocalFontRatio: Float
        get() = (annotationFontRatio * BACKGROUND_VOCAL_FONT_SCALE).coerceAtMost(1f)
}

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
 * `LyricsFontWeight` from BetterLyrics, cut down to the three steps a phone screen can tell
 * apart. Older builds stored eleven values; they are mapped onto the nearest remaining step so
 * an existing preference keeps working.
 */
enum class LyricsFontWeight(val storageValue: String, val weight: FontWeight) {
    NORMAL("normal", FontWeight.Normal),
    MEDIUM("medium", FontWeight.Medium),
    BOLD("bold", FontWeight.Bold);

    companion object {
        val DEFAULT = BOLD

        fun fromStorage(value: String?): LyricsFontWeight = when (value) {
            "normal", "thin", "extra_light", "light", "semi_light" -> NORMAL
            "medium" -> MEDIUM
            "bold", "semi_bold", "extra_bold", "black", "extra_black" -> BOLD
            else -> DEFAULT
        }
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
 * `KaraokeLyricsView.textColor`. `ARTWORK` tints the words with the scheme's primary — the colour
 * the song title and artist use, which the app scheme derives from the cover — `NEUTRAL` follows
 * the page polarity without a hue, and the remaining modes pin a fixed reading colour. Stored
 * `"theme"` from older builds falls through to the default because that colour is what the default
 * now resolves to.
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

/** WinUI centres the playing line, and so does the immersive page by default. */
const val DEFAULT_PLAYING_LINE_TOP_OFFSET = 0.5f

/** BetterLyrics defaults, expressed as fractions instead of percentages. */
const val DEFAULT_LINE_SPACING_FACTOR = 0.5f

/** Translation size relative to the primary lyric; the other annotation layers derive from it. */
const val DEFAULT_ANNOTATION_FONT_RATIO = WinLyricsVisualDefaults.TRANSLATION_FONT_RATIO

/** `LyricsLayoutHelper`: transliteration 0.55 and backing vocals 0.80 against translation 0.70. */
private const val ROMANIZATION_FONT_SCALE =
    WinLyricsVisualDefaults.ROMANIZATION_FONT_RATIO / WinLyricsVisualDefaults.TRANSLATION_FONT_RATIO
private const val BACKGROUND_VOCAL_FONT_SCALE = 0.80f / WinLyricsVisualDefaults.TRANSLATION_FONT_RATIO

const val DEFAULT_EDGE_FADE_REACH = 1f
const val DEFAULT_FOLLOW_SCROLL_DURATION_MS = 650
const val DEFAULT_AUTO_SCROLL_RESUME_DELAY_MS = 2_500L

/**
 * Renderer constants that used to be sliders. They were tuned once against `KaraokeLyricsView`
 * and have no per-device meaning, so the settings page no longer asks about them.
 */
object LyricsRenderDefaults {
    const val KEEP_ALIVE_ZONE_DP = 100f
    const val GAP_DOTS_COUNT = 3
    const val GAP_DOTS_SIZE_DP = 16f
    const val GAP_DOTS_SPACING_DP = 12f
    const val GAP_DOTS_ENTER_MS = 3_000
    const val GAP_DOTS_HOLD_MS = 200
    const val GAP_DOTS_PULSE_MS = 3_000
    const val GAP_DOTS_EXIT_MS = 200
    const val FOLLOW_SPRING_STIFFNESS = 100f
    const val FOLLOW_SPRING_DAMPING = 12f
    const val FOLLOW_SPRING_COUPLING = 0.65f
    const val FOLLOW_SPRING_DISTANCE_FALLOFF = 0.25f
    const val FOLLOW_SPRING_MIN_RESPONSE = 0.35f
}

/** Matches UserPreferencesRepository's default for the immersive-lyrics timeout. */
const val DEFAULT_IMMERSIVE_LYRICS_TIMEOUT_MS = 4_000L

val AMBIENT_ENABLED_KEY = booleanPreferencesKey("betterlyrics_ambient_enabled")
val AMBIENT_STYLE_KEY = stringPreferencesKey("betterlyrics_ambient_style")

val SPECTRUM_ENABLED_KEY = booleanPreferencesKey("betterlyrics_spectrum_enabled")
val SPECTRUM_STYLE_KEY = stringPreferencesKey("betterlyrics_spectrum_style")
val SPECTRUM_PLACEMENT_KEY = stringPreferencesKey("betterlyrics_spectrum_placement")
val SPECTRUM_BAR_COUNT_KEY = intPreferencesKey("betterlyrics_spectrum_bar_count")
val SPECTRUM_SENSITIVITY_KEY = floatPreferencesKey("betterlyrics_spectrum_sensitivity")
val SPECTRUM_OPACITY_KEY = floatPreferencesKey("betterlyrics_spectrum_opacity")
val SPECTRUM_GLOW_KEY = booleanPreferencesKey("betterlyrics_spectrum_glow")
val SPECTRUM_BREATHING_KEY = booleanPreferencesKey("betterlyrics_spectrum_breathing")

val LYRICS_PLAYING_LINE_OFFSET_KEY = floatPreferencesKey("lyrics_playing_line_offset")
val IMMERSIVE_LYRICS_ENABLED_KEY = booleanPreferencesKey("immersive_lyrics_enabled")
val IMMERSIVE_LYRICS_TIMEOUT_KEY = longPreferencesKey("immersive_lyrics_timeout")

val SHOW_LYRICS_TRANSLATION_KEY = booleanPreferencesKey("show_lyrics_translation")
val SHOW_LYRICS_ROMANIZATION_KEY = booleanPreferencesKey("show_lyrics_romanization")
val ANIMATED_LYRICS_BLUR_ENABLED_KEY = booleanPreferencesKey("animated_lyrics_blur_enabled")
val ANIMATED_LYRICS_BLUR_STRENGTH_KEY = floatPreferencesKey("animated_lyrics_blur_strength")
val LYRICS_LINE_SPACING_KEY = floatPreferencesKey("lyrics_line_spacing_factor")
val LYRICS_ANNOTATION_FONT_RATIO_KEY = floatPreferencesKey("lyrics_annotation_font_ratio")

/** The pre-merge translation slider, read once so an existing preference seeds the new one. */
private val LEGACY_LYRICS_TRANSLATION_FONT_RATIO_KEY =
    floatPreferencesKey("lyrics_translation_font_ratio")

val LYRICS_EDGE_FEATHERING_KEY = booleanPreferencesKey("lyrics_edge_feathering_enabled")
val LYRICS_EDGE_FADE_REACH_KEY = floatPreferencesKey("lyrics_edge_fade_reach")
val LYRICS_FOLLOW_SCROLL_DURATION_KEY = intPreferencesKey("lyrics_follow_scroll_duration_ms")
val LYRICS_SCROLL_RESUME_DELAY_KEY = longPreferencesKey("lyrics_scroll_resume_delay_ms")
val LYRICS_FONT_WEIGHT_KEY = stringPreferencesKey("lyrics_font_weight")
val LYRICS_FONT_FAMILY_KEY = stringPreferencesKey("lyrics_font_family")
val LYRICS_ALIGNMENT_KEY = stringPreferencesKey("lyrics_alignment")
val LYRICS_TEXT_COLOR_MODE_KEY = stringPreferencesKey("lyrics_text_color_mode")
val LYRICS_SCALE_EFFECT_KEY = booleanPreferencesKey("lyrics_scale_effect_enabled")
val LYRICS_GLOW_EFFECT_KEY = booleanPreferencesKey("lyrics_glow_effect_enabled")
val LYRICS_FLOAT_EFFECT_KEY = booleanPreferencesKey("lyrics_float_effect_enabled")

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
                    preferences[ANIMATED_LYRICS_BLUR_ENABLED_KEY] ?: true,
                animatedLyricsBlurStrengthDp =
                    (preferences[ANIMATED_LYRICS_BLUR_STRENGTH_KEY]
                        ?: WinLyricsVisualDefaults.LINE_BLUR_AMOUNT_DP).coerceIn(0.1f, 8f),
                playbackSpeed = preferences[floatPreferencesKey("playback_speed")] ?: 1f,
                ambientEnabled = preferences[AMBIENT_ENABLED_KEY] ?: true,
                ambientStyle = BetterLyricsAmbientStyle.fromStorage(
                    preferences[AMBIENT_STYLE_KEY]
                ),
                spectrumEnabled = preferences[SPECTRUM_ENABLED_KEY] ?: false,
                spectrumStyle = SpectrumStyle.fromStorage(preferences[SPECTRUM_STYLE_KEY]),
                spectrumPlacement = SpectrumPlacement.fromStorage(
                    preferences[SPECTRUM_PLACEMENT_KEY]
                ),
                spectrumBarCount = (preferences[SPECTRUM_BAR_COUNT_KEY] ?: 32)
                    .takeIf { it in SPECTRUM_BAR_COUNTS } ?: 32,
                spectrumSensitivity = (preferences[SPECTRUM_SENSITIVITY_KEY] ?: 1f)
                    .coerceIn(0.5f, 2f),
                spectrumOpacity = (preferences[SPECTRUM_OPACITY_KEY] ?: 0.9f)
                    .coerceIn(0.2f, 1f),
                spectrumGlowEnabled = preferences[SPECTRUM_GLOW_KEY] ?: true,
                spectrumBreathingEnabled = preferences[SPECTRUM_BREATHING_KEY] ?: true,
                playingLineTopOffset = (preferences[LYRICS_PLAYING_LINE_OFFSET_KEY]
                    ?: DEFAULT_PLAYING_LINE_TOP_OFFSET).coerceIn(0.2f, 0.6f),
                immersiveLyricsEnabled = preferences[IMMERSIVE_LYRICS_ENABLED_KEY] ?: false,
                immersiveLyricsTimeoutMs = preferences[IMMERSIVE_LYRICS_TIMEOUT_KEY]
                    ?: DEFAULT_IMMERSIVE_LYRICS_TIMEOUT_MS,
                visual = LyricsVisualSettings(
                    lineSpacingFactor = (preferences[LYRICS_LINE_SPACING_KEY]
                        ?: DEFAULT_LINE_SPACING_FACTOR).coerceIn(0f, 1.5f),
                    annotationFontRatio = (preferences[LYRICS_ANNOTATION_FONT_RATIO_KEY]
                        ?: preferences[LEGACY_LYRICS_TRANSLATION_FONT_RATIO_KEY]
                        ?: DEFAULT_ANNOTATION_FONT_RATIO).coerceIn(0.4f, 1f),
                    edgeFeatheringEffectEnabled = preferences[LYRICS_EDGE_FEATHERING_KEY] ?: true,
                    edgeFadeReach = (preferences[LYRICS_EDGE_FADE_REACH_KEY]
                        ?: DEFAULT_EDGE_FADE_REACH).coerceIn(0f, 1f),
                    textColorMode = LyricsTextColorMode.fromStorage(
                        preferences[LYRICS_TEXT_COLOR_MODE_KEY]
                    ),
                    scaleEffectEnabled = preferences[LYRICS_SCALE_EFFECT_KEY] ?: true,
                    glowEffectEnabled = preferences[LYRICS_GLOW_EFFECT_KEY] ?: true,
                    floatEffectEnabled = preferences[LYRICS_FLOAT_EFFECT_KEY] ?: true,
                    followScrollDurationMs = (preferences[LYRICS_FOLLOW_SCROLL_DURATION_KEY]
                        ?: DEFAULT_FOLLOW_SCROLL_DURATION_MS).coerceIn(0, 1_200),
                    scrollResumeDelayMs = (preferences[LYRICS_SCROLL_RESUME_DELAY_KEY]
                        ?: DEFAULT_AUTO_SCROLL_RESUME_DELAY_MS).coerceIn(0L, 5_000L),
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
 * Clears every appearance preference owned by the lyrics page so all of them fall back to their
 * defaults. Provider settings, source order and imported lyrics live elsewhere and are untouched.
 *
 * The legacy translation-ratio key is cleared as well: it is only read as the seed for the merged
 * annotation size, so leaving it behind would resurrect the old value after a reset.
 */
suspend fun resetLyricsDisplayPreferences(context: Context) {
    context.dataStore.edit { preferences ->
        preferences.remove(LYRICS_FONT_SIZE_SP_KEY)
        preferences.remove(LYRICS_DUET_LAYOUT_KEY)
        preferences.remove(SHOW_LYRICS_BACKGROUND_VOCALS_KEY)
        preferences.remove(ANIMATED_LYRICS_BLUR_ENABLED_KEY)
        preferences.remove(ANIMATED_LYRICS_BLUR_STRENGTH_KEY)
        preferences.remove(LYRICS_LINE_SPACING_KEY)
        preferences.remove(LYRICS_ANNOTATION_FONT_RATIO_KEY)
        preferences.remove(LEGACY_LYRICS_TRANSLATION_FONT_RATIO_KEY)
        preferences.remove(LYRICS_EDGE_FEATHERING_KEY)
        preferences.remove(LYRICS_EDGE_FADE_REACH_KEY)
        preferences.remove(LYRICS_FOLLOW_SCROLL_DURATION_KEY)
        preferences.remove(LYRICS_SCROLL_RESUME_DELAY_KEY)
        preferences.remove(LYRICS_FONT_WEIGHT_KEY)
        preferences.remove(LYRICS_FONT_FAMILY_KEY)
        preferences.remove(LYRICS_ALIGNMENT_KEY)
        preferences.remove(LYRICS_TEXT_COLOR_MODE_KEY)
        preferences.remove(LYRICS_SCALE_EFFECT_KEY)
        preferences.remove(LYRICS_GLOW_EFFECT_KEY)
        preferences.remove(LYRICS_FLOAT_EFFECT_KEY)
        preferences.remove(AMBIENT_ENABLED_KEY)
        preferences.remove(AMBIENT_STYLE_KEY)
        preferences.remove(SPECTRUM_ENABLED_KEY)
        preferences.remove(SPECTRUM_STYLE_KEY)
        preferences.remove(SPECTRUM_PLACEMENT_KEY)
        preferences.remove(SPECTRUM_BAR_COUNT_KEY)
        preferences.remove(SPECTRUM_SENSITIVITY_KEY)
        preferences.remove(SPECTRUM_OPACITY_KEY)
        preferences.remove(SPECTRUM_GLOW_KEY)
        preferences.remove(SPECTRUM_BREATHING_KEY)
        preferences.remove(LYRICS_PLAYING_LINE_OFFSET_KEY)
    }
}

/**
 * Applies the user's lyrics font size while keeping the base style's line-height ratio.
 */
fun TextStyle.scaledForLyrics(fontSizeSp: Float): TextStyle {
    val ratio = if (fontSize.isSp && lineHeight.isSp && fontSize.value > 0f) {
        lineHeight.value / fontSize.value
    } else {
        DEFAULT_LYRICS_LINE_HEIGHT_RATIO
    }
    return copy(
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * ratio).sp,
    )
}
