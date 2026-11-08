package io.github.jayfunc.carpelines.data.model

/**
 * Where the cover shown for a playing track comes from.
 *
 * The user's order is both the priority list and the set of enabled sources: the first entry that
 * can produce a picture wins, and a source the user removed from the list is never asked.
 */
enum class CoverArtSource {
    /** Artwork the playing app publishes through its MediaSession metadata. */
    MEDIA_SESSION,

    /** The matching library song's own cover: embedded art, folder art or its cached file. */
    LOCAL_EMBEDDED,

    /** Album cover looked up online from Deezer by track title and artist. */
    DEEZER;

    companion object {
        /** Today's behaviour: the session's own artwork, then the library's, then Deezer. */
        val DefaultOrder: List<CoverArtSource> = listOf(MEDIA_SESSION, LOCAL_EMBEDDED, DEEZER)

        fun fromName(name: String?): CoverArtSource? = entries.firstOrNull { it.name == name }

        /**
         * Reads the stored, comma-separated names. Unknown names are ignored, duplicates collapse,
         * and an empty or unreadable value falls back to [DefaultOrder] so the page always has at
         * least one source to show.
         */
        fun decode(raw: String?): List<CoverArtSource> {
            if (raw.isNullOrBlank()) return DefaultOrder
            val parsed = raw.split(',')
                .mapNotNull { fromName(it.trim()) }
                .distinct()
            return parsed.ifEmpty { DefaultOrder }
        }

        fun encode(order: List<CoverArtSource>): String =
            order.distinct().joinToString(",") { it.name }
    }
}
