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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Apple Music lyrics source, a Kotlin port of BetterLyrics' `SearchAppleMusicAsync`.
 *
 * Apple exposes syllable-timed TTML through the private AMP API. Access needs a `media-user-token`
 * copied by the user from a signed-in browser session (Settings → Apple Music), and a web-player
 * bearer token that is scraped from the public browse page, exactly as the Windows app does.
 */
class AppleMusicSource(
    private val client: HttpClient,
    private val mediaUserTokenProvider: () -> String? = { null },
) : LyricsSource {

    override val provider: LyricsProvider = LyricsProvider.APPLE_MUSIC

    private var accessToken: String? = null
    private var storefront: String? = null
    private var language: String? = null
    private var initialized = false

    override suspend fun search(query: LyricsQuery, limit: Int): List<LyricsSearchResult> {
        if (query.title.isBlank()) return emptyList()
        if (!ensureInitialized()) return emptyList()
        val storefront = storefront ?: return emptyList()
        val language = language.orEmpty()

        val searchResponse = client.get("$AMP_API/v1/catalog/$storefront/search") {
            appleHeaders()
            parameter("term", "${query.artist} ${query.title}".trim())
            parameter("types", "songs")
            parameter("limit", limit.coerceIn(1, 10).toString())
            if (language.isNotBlank()) parameter("l", language)
        }
        if (!searchResponse.status.isSuccess()) return emptyList()

        val root = parseJson(searchResponse.bodyAsText()) ?: return emptyList()
        val songs = root["results"]?.jsonObject
            ?.get("songs")?.jsonObject
            ?.get("data")?.jsonArray
            ?: return emptyList()

        val results = mutableListOf<LyricsSearchResult>()
        for (element in songs) {
            if (results.size >= limit) break
            val song = element.jsonObject
            val id = song["id"]?.jsonPrimitive?.contentOrNull ?: continue
            val attributes = song["attributes"]?.jsonObject ?: continue

            val title = attributes.str("name") ?: continue
            val artist = attributes.str("artistName")
            val album = attributes.str("albumName")
            val durationMs = attributes["durationInMillis"]?.jsonPrimitive?.longOrNull

            val raw = fetchLyrics(id, language)
            val result = LyricsSearchResult(
                provider = provider,
                raw = raw,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                reference = "https://music.apple.com/song/$id",
            )
            results += result.copy(matchPercentage = MetadataMatcher.score(query, result))
        }

        return results.sortedByDescending { it.matchPercentage }
    }

    private suspend fun ensureInitialized(): Boolean {
        if (initialized) return accessToken != null && storefront != null

        val mediaUserToken = mediaUserTokenProvider()?.trim()?.takeIf { it.isNotEmpty() }
            ?: return false

        val token = scrapeAccessToken()
        accessToken = token
        if (token == null) return false

        val storefrontResponse = client.get("$AMP_API/v1/me/storefront") {
            appleHeaders(mediaUserToken)
        }
        if (!storefrontResponse.status.isSuccess()) {
            accessToken = null
            return false
        }

        val storefrontJson = parseJson(storefrontResponse.bodyAsText())
        val first = (storefrontJson?.get("data") as? JsonArray)
            ?.firstOrNull()?.jsonObject
            ?: return false
        storefront = first["id"]?.jsonPrimitive?.contentOrNull
        language = first["attributes"]?.jsonObject?.str("defaultLanguageTag")

        initialized = storefront != null
        return initialized
    }

    /** Finds the best `AMPWebPlay` JWT advertised by the web player's index bundles. */
    private suspend fun scrapeAccessToken(): String? {
        val browse = client.get("https://music.apple.com/us/browse") {
            appleHeaders()
        }
        if (!browse.status.isSuccess()) return null
        val html = browse.bodyAsText()

        val assets = ASSET_URL.findAll(html)
            .map { match ->
                val raw = match.groupValues[1]
                when {
                    raw.startsWith("https://") -> raw
                    raw.startsWith("/") -> "https://music.apple.com$raw"
                    else -> "https://music.apple.com/$raw"
                }
            }
            .distinct()
            .take(MAX_SCRAPED_BUNDLES)
            .toList()

        for (asset in assets) {
            val response = client.get(asset) { appleHeaders() }
            if (!response.status.isSuccess()) continue
            val script = response.bodyAsText()
            val candidate = JWT_TOKEN.findAll(script)
                .map { it.value }
                .distinct()
                .map { it to scoreToken(it) }
                .filter { it.second >= 0 }
                .maxByOrNull { it.second }
                ?.first
            if (candidate != null) return candidate
        }
        return null
    }

    /** Higher is better; -1 means the token is not a web-play token. */
    private fun scoreToken(token: String): Int {
        val parts = token.split('.')
        if (parts.size < 2) return -1
        val header = decodeJwtPart(parts[0]) ?: return -1
        val payload = decodeJwtPart(parts[1]) ?: return -1

        var score = 0
        if ("WebPlayKid" in header) score += 100
        if ("AMPWebPlay" in payload) score += 100
        if ("root_https_origin" in payload) score += 10
        return if (score > 0) score else -1
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeJwtPart(part: String): String? = runCatching {
        Base64.UrlSafe
            .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
            .decode(part)
            .decodeToString()
    }.getOrNull()

    private suspend fun fetchLyrics(songId: String, language: String): String? {
        val storefront = storefront ?: return null
        val response = client.get("$AMP_API/v1/catalog/$storefront/songs/$songId") {
            appleHeaders()
            parameter("include[songs]", "lyrics,syllable-lyrics")
            if (language.isNotBlank()) parameter("l", language)
            parameter("extend", "ttmlLocalizations")
        }
        if (!response.status.isSuccess()) return null

        val root = parseJson(response.bodyAsText()) ?: return null
        val song = root["data"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        val relationships = song["relationships"]?.jsonObject ?: return null

        listOf("syllable-lyrics", "lyrics").forEach { key ->
            val ttml = relationships[key]?.jsonObject
                ?.get("data")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("attributes")?.jsonObject
                ?.str("ttml")
            if (!ttml.isNullOrBlank()) return ttml
        }
        return null
    }

    private fun io.ktor.client.request.HttpRequestBuilder.appleHeaders(
        mediaUserToken: String? = mediaUserTokenProvider()?.trim()?.takeIf { it.isNotEmpty() },
    ) {
        header(HttpHeaders.UserAgent, USER_AGENT)
        header(HttpHeaders.Accept, "application/json")
        header(HttpHeaders.Origin, "https://music.apple.com")
        header(HttpHeaders.Referrer, "https://music.apple.com/")
        header("media-user-token", mediaUserToken ?: "")
        accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        language?.takeIf { it.isNotBlank() }?.let {
            header(HttpHeaders.AcceptLanguage, "$it,en;q=0.9")
        }
    }

    private fun parseJson(raw: String): JsonObject? = runCatching {
        Json.parseToJsonElement(raw).jsonObject
    }.getOrNull()

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private companion object {
        const val AMP_API = "https://amp-api.music.apple.com"
        const val MAX_SCRAPED_BUNDLES = 6
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36"

        val JWT_TOKEN = Regex("""eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")
        val ASSET_URL = Regex(
            """["']((?:https://music\.apple\.com)?/?assets/index(?!-legacy)[^"'<>\s]*?\.js)["']""",
            RegexOption.IGNORE_CASE,
        )
    }
}
