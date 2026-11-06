package io.github.jayfunc.carpelines.lyrics.provider

import io.github.jayfunc.carpelines.lyrics.match.MetadataMatcher
import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery
import io.github.jayfunc.carpelines.lyrics.model.LyricsSearchResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * LRCLIB source, ported from BetterLyrics' `SearchLrcLibAsync` but returning every candidate so
 * the engine can pick by score instead of blindly taking the first result.
 */
class LrcLibSource(
    private val client: HttpClient,
) : LyricsSource {

    override val provider: LyricsProvider = LyricsProvider.LRCLIB

    override suspend fun search(query: LyricsQuery, limit: Int): List<LyricsSearchResult> {
        if (query.title.isBlank()) return emptyList()

        // Title + artist is the most reliable query. Album and duration would over-filter on
        // LRCLIB, so they are only used later by MetadataMatcher for scoring.
        val items = searchRequest(query, byFreeText = false)
            .ifEmpty { searchRequest(query, byFreeText = true) }

        return items
            .mapNotNull { item -> item.toResult(query) }
            .sortedByDescending { it.matchPercentage }
            .take(limit)
    }

    private suspend fun searchRequest(query: LyricsQuery, byFreeText: Boolean): List<LrcLibItem> {
        val response = client.get("$BASE_URL/api/search") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            if (byFreeText) {
                parameter("q", "${query.title} ${query.artist}".trim())
            } else {
                parameter("track_name", query.title)
                if (query.artist.isNotBlank()) parameter("artist_name", query.artist)
            }
        }
        if (!response.status.isSuccess()) return emptyList()

        val body = response.bodyAsText()
        return runCatching {
            json.decodeFromString<List<LrcLibItem>>(body)
        }.getOrNull().orEmpty()
    }

    private fun LrcLibItem.toResult(query: LyricsQuery): LyricsSearchResult? {
        val raw = syncedLyrics?.takeIf { it.isNotBlank() }
            ?: plainLyrics?.takeIf { it.isNotBlank() }
            ?: return null
        val durationMs = durationSeconds?.let { (it * 1_000.0).toLong() }
        return LyricsSearchResult(
            provider = provider,
            raw = raw,
            title = trackName,
            artist = artistName,
            album = albumName,
            durationMs = durationMs,
            reference = "$BASE_URL/api/get/$id",
        ).let { it.copy(matchPercentage = MetadataMatcher.score(query, it)) }
    }

    @Serializable
    private data class LrcLibItem(
        val id: Long? = null,
        @SerialName("trackName") val trackName: String? = null,
        @SerialName("artistName") val artistName: String? = null,
        @SerialName("albumName") val albumName: String? = null,
        @SerialName("duration") val durationSeconds: Double? = null,
        @SerialName("syncedLyrics") val syncedLyrics: String? = null,
        @SerialName("plainLyrics") val plainLyrics: String? = null,
    )

    private companion object {
        const val BASE_URL = "https://lrclib.net"
        const val USER_AGENT = "BetterLyrics-Android (https://github.com/jayfunc/BetterLyrics)"
        val json = Json { ignoreUnknownKeys = true }
    }
}
