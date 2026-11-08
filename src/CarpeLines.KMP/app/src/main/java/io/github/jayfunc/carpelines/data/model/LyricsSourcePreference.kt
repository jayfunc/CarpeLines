package io.github.jayfunc.carpelines.data.model

/**
 * Derived priority hint used by the playback pipeline.
 *
 * This is no longer a user-facing preference: [io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsRepository]
 * owns the editable source order, and `LyricsSourceOrderViewModel` keeps this value in sync with
 * it. It exists only because a few local-lyrics checks still ask whether online or a particular
 * local source comes first.
 */
enum class LyricsSourcePreference(val displayName: String) {
    /**
     * Try online API first, then embedded lyrics, then local .lrc files
     */
    API_FIRST("Online First"),

    /**
     * Try embedded lyrics in metadata first, then API, then local .lrc files
     */
    EMBEDDED_FIRST("Embedded First"),

    /**
     * Try local .lrc files first, then embedded lyrics, then API
     */
    LOCAL_FIRST("Local First");

    companion object {
        fun fromOrdinal(ordinal: Int): LyricsSourcePreference {
            return values().getOrElse(ordinal) { EMBEDDED_FIRST }
        }

        fun fromName(name: String?): LyricsSourcePreference {
            return values().find { it.name == name } ?: EMBEDDED_FIRST
        }
    }
}
