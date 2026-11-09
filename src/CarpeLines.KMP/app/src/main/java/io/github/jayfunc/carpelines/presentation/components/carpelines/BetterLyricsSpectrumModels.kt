package io.github.jayfunc.carpelines.presentation.components.betterlyrics

/** The two drawing shapes BetterLyrics' spectrum renderer supports. */
enum class SpectrumStyle {
    CURVE,
    BAR;

    companion object {
        fun fromStorage(value: String?): SpectrumStyle =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CURVE
    }
}

/** Where the spectrum band sits on the lyrics page. */
enum class SpectrumPlacement {
    BOTTOM,
    TOP,

    /** A frame around the album artwork, whichever size it is currently drawn at. */
    AROUND_ALBUM_ART;

    companion object {
        fun fromStorage(value: String?): SpectrumPlacement =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BOTTOM
    }
}

/** The bar counts offered by the settings page. */
val SPECTRUM_BAR_COUNTS: List<Int> = listOf(24, 32, 48)
