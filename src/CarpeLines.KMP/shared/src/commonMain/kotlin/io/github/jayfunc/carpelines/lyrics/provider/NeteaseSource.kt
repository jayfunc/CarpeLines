package io.github.jayfunc.carpelines.lyrics.provider

import io.github.jayfunc.carpelines.lyrics.match.MetadataMatcher
import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery
import io.github.jayfunc.carpelines.lyrics.model.LyricsSearchResult
import io.github.jayfunc.carpelines.lyrics.platform.aesEcbPkcs7EncryptHex
import io.github.jayfunc.carpelines.lyrics.platform.epochMillis
import io.github.jayfunc.carpelines.lyrics.platform.md5Hex
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlin.math.floor
import kotlin.random.Random

/**
 * NetEase Cloud Music provider using the EAPI endpoints from Lyricify-Lyrics-Helper.
 * A plain `/api` endpoint is kept as a fallback for networks where EAPI is blocked.
 */
class NeteaseSource(
    private val client: HttpClient,
) : LyricsSource {

    override val provider: LyricsProvider = LyricsProvider.NETEASE

    override suspend fun search(query: LyricsQuery, limit: Int): List<LyricsSearchResult> {
        if (query.title.isBlank()) return emptyList()
        val keyword = "${query.title} ${query.artist}".trim()
        val songs = searchEapi(keyword) ?: searchPlain(keyword) ?: return emptyList()
        val candidates = songs.mapNotNull { it.jsonObject.toCandidate(query) }
            .sortedByDescending { it.result.matchPercentage }

        val results = mutableListOf<LyricsSearchResult>()
        for (candidate in candidates.take(MAX_LYRIC_ATTEMPTS)) {
            val lyrics = fetchLyrics(candidate.id) ?: continue
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

    private suspend fun searchEapi(keyword: String): JsonArray? {
        val header = eapiHeader()
        val payload = buildJsonObject {
            put("header", header.toString())
            put("s", keyword)
            put("type", 1)
            put("limit", 30)
            put("offset", 0)
            put("total", true)
        }
        val response = eapiPost(CLOUD_SEARCH_PATH, payload, header) ?: return null
        if (!response.status.isSuccess()) return null
        val root = parse(response.bodyAsText()) ?: return null
        return root["result"]?.jsonObject?.get("songs") as? JsonArray
    }

    private suspend fun searchPlain(keyword: String): JsonArray? {
        val response = runCatching {
            client.get("https://music.163.com/api/search/get/web") {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Referrer, "https://music.163.com/")
                parameter("csrf_token", "")
                parameter("s", keyword)
                parameter("type", 1)
                parameter("offset", 0)
                parameter("total", "true")
                parameter("limit", 20)
            }
        }.getOrNull() ?: return null
        if (!response.status.isSuccess()) return null
        val root = parse(response.bodyAsText()) ?: return null
        return root["result"]?.jsonObject?.get("songs") as? JsonArray
    }

    private fun JsonObject.toCandidate(query: LyricsQuery): NeteaseCandidate? {
        val id = str("id") ?: return null
        val title = str("name") ?: return null
        val artist = this["artists"]?.jsonArray
            ?.mapNotNull { it.jsonObject.str("name") }
            ?.joinToString("/")
            .orEmpty()
        val album = this["album"]?.jsonObject?.str("name").orEmpty()
        val durationMs = this["duration"]?.jsonPrimitive?.longOrNull
        val result = LyricsSearchResult(
            provider = provider,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            reference = "https://music.163.com/song?id=$id",
        ).let { it.copy(matchPercentage = MetadataMatcher.score(query, it)) }
        return NeteaseCandidate(result, id)
    }

    private suspend fun fetchLyrics(songId: String): LyricsSearchResult? {
        val eapiLyrics = fetchEapiLyrics(songId)
        if (eapiLyrics != null) return eapiLyrics
        return fetchPlainLyrics(songId)
    }

    private suspend fun fetchEapiLyrics(songId: String): LyricsSearchResult? {
        val header = eapiHeader()
        val payload = buildJsonObject {
            put("header", header.toString())
            put("id", songId)
            put("cp", false)
            put("lv", 0)
            put("kv", 0)
            put("tv", 0)
            put("rv", 0)
            put("yv", 0)
            put("ytv", 0)
            put("yrv", 0)
            put("csrf_token", "")
        }
        val response = eapiPost(LYRIC_V1_PATH, payload, header) ?: return null
        if (!response.status.isSuccess()) return null
        return parseLyricsResponse(response.bodyAsText())
    }

    private suspend fun fetchPlainLyrics(songId: String): LyricsSearchResult? {
        val response = runCatching {
            client.get("https://music.163.com/api/song/lyric") {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Referrer, "https://music.163.com/")
                parameter("id", songId)
                parameter("lv", -1)
                parameter("kv", -1)
                parameter("tv", -1)
                parameter("rv", -1)
            }
        }.getOrNull() ?: return null
        if (!response.status.isSuccess()) return null
        return parseLyricsResponse(response.bodyAsText())
    }

    private fun parseLyricsResponse(body: String): LyricsSearchResult? {
        val root = parse(body) ?: return null
        val lrc = root.lyricField("lrc")
        if (lrc.isNullOrBlank()) return null
        return LyricsSearchResult(
            provider = provider,
            raw = lrc,
            translationRaw = root.lyricField("tlyric"),
            transliterationRaw = root.lyricField("romalrc") ?: root.lyricField("yromalrc"),
        )
    }

    private fun JsonObject.lyricField(key: String): String? =
        this[key]?.jsonObject?.str("lyric")

    private suspend fun eapiPost(path: String, payload: JsonObject, header: JsonObject) = runCatching {
        val params = eapiParams(path, payload)
        val cookie = eapiCookieHeader(header)
        client.post("https://interface.music.163.com/eapi/$path") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Referrer, "https://music.163.com/")
            header(HttpHeaders.Cookie, cookie)
            setBody(TextContent("params=$params", ContentType.Application.FormUrlEncoded))
        }
    }.getOrNull()

    /** Mirrors Lyricify's `EApi` signature: MD5(path + body) inside an AES-ECB envelope. */
    private fun eapiParams(path: String, payload: JsonObject): String {
        val apiPath = "/api/$path"
        val text = payload.toString()
        val digest = md5Hex(
            "nobody${apiPath}use${text}md5forencrypt".encodeToByteArray()
        )
        val plain = "$apiPath-36cd479b6b5-$text-36cd479b6b5-$digest"
        return aesEcbPkcs7EncryptHex(plain.encodeToByteArray(), EAPI_KEY)
    }

    private fun eapiHeader(): JsonObject {
        val now = epochMillis()
        val random = floor(Random.nextDouble() * 1000.0).toInt().toString().padStart(4, '0')
        return buildJsonObject {
            put("__csrf", "")
            put("appver", "8.0.0")
            put("buildver", (now / 1000L).toString())
            put("channel", "")
            put("deviceId", "")
            put("mobilename", "")
            put("resolution", "1920x1080")
            put("os", "android")
            put("osver", "")
            put("requestId", "${now}_$random")
            put("versioncode", "140")
            put("MUSIC_U", "")
        }
    }

    private fun eapiCookieHeader(header: JsonObject): String =
        header.entries.joinToString("; ") { (key, value) ->
            "$key=${value.jsonPrimitive.contentOrNull.orEmpty()}"
        }

    private fun parse(body: String): JsonObject? =
        runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private data class NeteaseCandidate(
        val result: LyricsSearchResult,
        val id: String,
    )

    private companion object {
        const val CLOUD_SEARCH_PATH = "cloudsearch/pc"
        const val LYRIC_V1_PATH = "song/lyric/v1"
        const val MAX_LYRIC_ATTEMPTS = 4
        val EAPI_KEY = "e82ckenh8dichen8".encodeToByteArray()
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 9; PCT-AL10) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/70.0.3538.64 HuaweiBrowser/10.0.3.311 Mobile Safari/537.36"
    }
}
