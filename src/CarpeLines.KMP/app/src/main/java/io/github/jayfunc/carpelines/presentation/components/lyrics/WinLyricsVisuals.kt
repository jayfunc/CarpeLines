package io.github.jayfunc.carpelines.presentation.components.lyrics

/**
 * Visual defaults mirrored from BetterLyrics.WinUI3:
 * `LyricsStyleSettings` (opacities and text sizes) and `LyricsEffectSettings` (blur).
 *
 * Keeping them in one place makes the Compose settings auditable against the WinUI renderer
 * instead of drifting into a look that is merely "similar".
 */
internal object WinLyricsVisualDefaults {
    /** WinUI uses a blur amount of 5 for the farthest visible line. */
    const val LINE_BLUR_AMOUNT_DP = 5f

    /** `UnplayedOriginalLyricsOpacity = 30`, `TranslatedLyricsOpacity = 60`, `PhoneticLyricsOpacity = 60`. */
    const val UNPLAYED_LINE_OPACITY = 0.30f
    const val TRANSLATION_OPACITY = 0.60f
    const val ROMANIZATION_OPACITY = 0.60f

    /** `LyricsLayoutHelper`: translation 0.70 of the main size, transliteration 0.55. */
    const val TRANSLATION_FONT_RATIO = 0.70f
    const val ROMANIZATION_FONT_RATIO = 0.55f
}
