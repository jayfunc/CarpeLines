package io.github.jayfunc.carpelines.lyrics.provider

import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery
import io.github.jayfunc.carpelines.lyrics.model.LyricsSearchResult

/** A remote lyrics backend. Implementations never parse: they return the raw payload. */
interface LyricsSource {
    val provider: LyricsProvider

    /** Returns candidates ordered by relevance. Errors are reported by throwing. */
    suspend fun search(query: LyricsQuery, limit: Int = 5): List<LyricsSearchResult>
}
