package io.github.jayfunc.carpelines.lyrics.provider

import io.github.jayfunc.carpelines.lyrics.decrypt.KrcDecrypter
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Kugou Music provider: search via the mobile API, then resolve lyric candidates by hash and
 * decrypt the returned KRC payload (Lyricify-Lyrics-Helper's `Krc` decrypter).
 */
class KugouSource(
    private val client: HttpClient,
) : LyricsSource {

    override val provider: LyricsProvider = LyricsProvider.KUGOU

    override suspend fun search(query: LyricsQuery, limit: Int): List<LyricsSearchResult> {
        if (query.title.isBlank()) return emptyList()
        val keyword = "${query.title} ${query.artist}".trim()
        val response = runCatching {
            client.get(SEARCH_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("format", "json")
                parameter("keyword", keyword)
                parameter("page", 1)
                parameter("pagesize", 20)
                parameter("showtype", 1)
            }
        }.getOrNull() ?: return emptyList()
        if (!response.status.isSuccess()) return emptyList()

        val root = parse(response.bodyAsText()) ?: return emptyList()
        val songs = root["data"]?.jsonObject?.get("info") as? JsonArray ?: return emptyList()
        val candidates = songs.mapNotNull { element -> element.jsonObject.toCandidate(query) }
            .sortedByDescending { it.result.matchPercentage }

        val results = mutableListOf<LyricsSearchResult>()
        for (candidate in candidates.take(MAX_LYRIC_ATTEMPTS)) {
            val lyrics = fetchLyrics(candidate.hash, candidate.result.durationMs) ?: continue
            results += lyrics.copy(
                title = candidate.result.title,
                artist = candidate.result.artist,
                album = candidate.result.album,
                durationMs = candidate.result.durationMs,
                matchPercentage = candidate.result.matchPercentage,
            )
            if (results.size >= limit) break
        }
        return results
    }

    private fun JsonObject.toCandidate(query: LyricsQuery): KugouCandidate? {
        val hash = this["hash"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: return null
        val title = str("songname") ?: str("filename") ?: return null
        val artist = str("singername").orEmpty().split('、').joinToString("/")
        val album = str("album_name").orEmpty()
        val durationMs = this["duration"]?.jsonPrimitive?.longOrNull?.times(1_000L)
        val result = LyricsSearchResult(
            provider = provider,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            reference = "https://www.kugou.com/song/#hash=$hash",
        ).let { it.copy(matchPercentage = MetadataMatcher.score(query, it)) }
        return KugouCandidate(result, hash)
    }

    private suspend fun fetchLyrics(hash: String, durationMs: Long?): LyricsSearchResult? {
        val lyricsSearch = runCatching {
            client.get(LYRIC_SEARCH_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("ver", 1)
                parameter("man", "yes")
                parameter("client", "pc")
                parameter("keyword", "")
                durationMs?.let { parameter("duration", it / 1_000L) }
                parameter("hash", hash)
            }
        }.getOrNull() ?: return null
        if (!lyricsSearch.status.isSuccess()) return null

        val root = parse(lyricsSearch.bodyAsText()) ?: return null
        val candidates = root["candidates"] as? JsonArray ?: return null
        for (element in candidates) {
            val candidate = element.jsonObject
            val id = candidate.str("id") ?: continue
            val accessKey = candidate.str("accesskey") ?: continue
            val krc = download(id, accessKey, "krc")
            if (!krc.isNullOrBlank()) {
                return LyricsSearchResult(provider = provider, raw = krc)
            }
            val lrc = download(id, accessKey, "lrc")
            if (!lrc.isNullOrBlank()) {
                return LyricsSearchResult(provider = provider, raw = lrc)
            }
        }
        return null
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun download(id: String, accessKey: String, format: String): String? {
        val response = runCatching {
            client.get(DOWNLOAD_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("ver", 1)
                parameter("client", "pc")
                parameter("id", id)
                parameter("accesskey", accessKey)
                parameter("fmt", format)
                parameter("charset", "utf8")
            }
        }.getOrNull() ?: return null
        if (!response.status.isSuccess()) return null
        val content = parse(response.bodyAsText())?.str("content") ?: return null
        if (format == "krc") return KrcDecrypter.decrypt(content)

        val decoded = runCatching { Base64.decode(content) }.getOrNull()
        return decoded?.decodeToString()?.takeIf { it.contains("[") } ?: content
    }

    private fun parse(body: String): JsonObject? =
        runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private data class KugouCandidate(
        val result: LyricsSearchResult,
        val hash: String,
    )

    private companion object {
        // The CDN host only serves a valid certificate over the legacy domain; HTTPS fails TLS
        // validation, so cleartext is used (the app declares usesCleartextTraffic accordingly).
        const val SEARCH_URL = "http://mobilecdn.kugou.com/api/v3/search/song"
        const val LYRIC_SEARCH_URL = "https://lyrics.kugou.com/search"
        const val DOWNLOAD_URL = "https://lyrics.kugou.com/download"
        const val MAX_LYRIC_ATTEMPTS = 4
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }
}
