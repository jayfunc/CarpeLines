package io.github.jayfunc.carpelines.lyrics.provider

import io.github.jayfunc.carpelines.lyrics.decrypt.QrcDecrypter
import io.github.jayfunc.carpelines.lyrics.match.MetadataMatcher
import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery
import io.github.jayfunc.carpelines.lyrics.model.LyricsSearchResult
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * QQ Music provider, following Lyricify-Lyrics-Helper's search + lyric_download flow.
 * Syllable lyrics are returned by the QRC endpoint and decrypted before parsing.
 */
class QqMusicSource(
    private val client: HttpClient,
) : LyricsSource {

    override val provider: LyricsProvider = LyricsProvider.QQ_MUSIC

    override suspend fun search(query: LyricsQuery, limit: Int): List<LyricsSearchResult> {
        if (query.title.isBlank()) return emptyList()
        val keyword = "${query.title} ${query.artist}".trim()
        val songs = searchMobile(keyword).ifEmpty { searchDesktop(keyword) }
        if (songs.isEmpty()) return emptyList()

        val candidates = songs.mapNotNull { element -> element.jsonObject.toCandidate(query) }
            .sortedByDescending { it.result.matchPercentage }

        val results = mutableListOf<LyricsSearchResult>()
        for (candidate in candidates.take(MAX_LYRIC_ATTEMPTS)) {
            val lyrics = fetchLyrics(candidate) ?: continue
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

    /**
     * Lyricify's current primary path: the Android client search endpoint. It answers anonymous
     * requests, while the legacy desktop method now returns `code 500001`.
     */
    private suspend fun searchMobile(keyword: String): List<JsonElement> {
        val requestKey = "$SEARCH_MODULE.$MOBILE_SEARCH_METHOD"
        val requestBody = buildJsonObject {
            put(
                "comm",
                buildJsonObject {
                    put("ct", "11")
                    put("v", MOBILE_CLIENT_VERSION)
                    put("tmeLoginType", "0")
                    put("tmeLoginMethod", "0")
                },
            )
            put(
                requestKey,
                buildJsonObject {
                    put("module", SEARCH_MODULE)
                    put("method", MOBILE_SEARCH_METHOD)
                    put(
                        "param",
                        buildJsonObject {
                            put("query", keyword)
                            put("highlight", 1)
                            put("searchid", "")
                            put("sub_searchid", 0)
                            put("search_type", 0)
                            put("nqc_flag", 0)
                            put("sin", 0)
                            put("ein", 30)
                            put("page_num", 1)
                            put("num_per_page", 30)
                            put("cat", 2)
                            put("grp", 1)
                            put("remoteplace", "")
                            put("from", "")
                            put("multi_zhida", 1)
                            put("sem", 0)
                            put("related_data", "")
                            put("ambi_data", "")
                        },
                    )
                },
            )
        }.toString()

        val body = postJson(requestBody, mobileClient = true) ?: return emptyList()
        val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return emptyList()
        val node = root[requestKey]?.jsonObject ?: return emptyList()
        if (node["code"]?.jsonPrimitive?.content?.toIntOrNull() != 0) return emptyList()
        val data = node["data"]?.jsonObject ?: return emptyList()
        if (data["code"]?.jsonPrimitive?.content?.toIntOrNull() !in setOf(0, null)) return emptyList()
        val bodyObject = data["body"]?.jsonObject ?: return emptyList()
        val itemSong = bodyObject["item_song"] as? JsonArray
        return itemSong?.toList().orEmpty()
    }

    /** Legacy desktop search, kept as a fallback when the mobile endpoint has no results. */
    private suspend fun searchDesktop(keyword: String): List<JsonElement> {
        val requestBody = buildJsonObject {
            put(
                "comm",
                buildJsonObject {
                    put("ct", "19")
                    put("cv", "1859")
                    put("uin", "0")
                },
            )
            put(
                "req_1",
                buildJsonObject {
                    put("method", DESKTOP_SEARCH_METHOD)
                    put("module", SEARCH_MODULE)
                    put(
                        "param",
                        buildJsonObject {
                            put("num_per_page", "20")
                            put("page_num", "1")
                            put("query", keyword)
                            put("search_type", 0)
                        },
                    )
                },
            )
        }.toString()

        val body = postJson(requestBody, mobileClient = false) ?: return emptyList()
        val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return emptyList()
        if (root["code"]?.jsonPrimitive?.content?.toIntOrNull() !in setOf(0, null)) return emptyList()
        val data = root["req_1"]?.jsonObject ?: return emptyList()
        if (data["code"]?.jsonPrimitive?.content?.toIntOrNull() !in setOf(0, null)) return emptyList()
        val list = data["data"]?.jsonObject
            ?.get("body")?.jsonObject
            ?.get("song")?.jsonObject
            ?.get("list") as? JsonArray
        return list?.toList().orEmpty()
    }

    private suspend fun postJson(requestBody: String, mobileClient: Boolean): String? {
        val response = runCatching {
            client.post(SEARCH_URL) {
                header(HttpHeaders.UserAgent, if (mobileClient) MOBILE_USER_AGENT else USER_AGENT)
                header(HttpHeaders.Referrer, "https://c.y.qq.com/")
                header(HttpHeaders.Accept, "*/*")
                setBody(TextContent(requestBody, ContentType.Application.FormUrlEncoded))
            }
        }.getOrNull() ?: return null
        if (!response.status.isSuccess()) return null
        return response.bodyAsText()
    }

    /** Strips the `<em>` highlight markers the mobile API wraps around matched text. */
    private fun String.cleanHighlight(): String = replace("<em>", "").replace("</em>", "")


    private fun JsonObject.toCandidate(query: LyricsQuery): QqCandidate? {
        val mid = str("mid") ?: return null
        val songId = str("id")?.takeIf { id -> id.all(Char::isDigit) }
        val title = str("title") ?: str("name") ?: return null
        val artist = this["singer"]?.jsonArray
            ?.mapNotNull { it.jsonObject.str("name") }
            ?.joinToString("/")
            .orEmpty()
        val album = this["album"]?.jsonObject?.let { it.str("title") ?: it.str("name") }.orEmpty()
        val durationMs = this["interval"]?.jsonPrimitive?.longOrNull?.times(1_000L)
        val result = LyricsSearchResult(
            provider = provider,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            reference = "https://y.qq.com/n/ryqq/songDetail/$mid",
        ).let { it.copy(matchPercentage = MetadataMatcher.score(query, it)) }
        return QqCandidate(result, mid, songId)
    }

    /**
     * The QRC endpoint keys on the numeric song id while the LRC fallback keys on the song mid.
     * When a result has no numeric id the QRC request is skipped and only LRC is tried.
     */
    private suspend fun fetchLyrics(candidate: QqCandidate): LyricsSearchResult? =
        candidate.songId?.let { fetchQrcLyrics(it) } ?: fetchLrcLyrics(candidate.mid)

    /** Word-timed QRC payload. Some catalogue entries answer with an empty body (VIP only). */
    private suspend fun fetchQrcLyrics(songId: String): LyricsSearchResult? {
        val response = runCatching {
            client.post(LYRIC_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Referrer, "https://y.qq.com/portal/player.html")
                setBody(
                    TextContent(
                        "version=15&miniversion=82&lrctype=4&musicid=$songId",
                        ContentType.Application.FormUrlEncoded,
                    ),
                )
            }
        }.getOrNull() ?: return null
        if (!response.status.isSuccess()) return null

        val xml = response.bodyAsText().replace("<!--", "").replace("-->", "")
        val content = extractTag(xml, "content")?.let(::decodePayload) ?: return null
        val translation = extractTag(xml, "contentts")?.let(::decodePayload)
        val romanization = extractTag(xml, "contentroma")?.let(::decodePayload)
        return LyricsSearchResult(
            provider = provider,
            raw = content,
            translationRaw = translation,
            transliterationRaw = romanization,
        )
    }

    /** Line-synced LRC fallback served by the web player endpoint, with its translation track. */
    private suspend fun fetchLrcLyrics(mid: String): LyricsSearchResult? {
        val response = runCatching {
            client.get(LRC_URL) {
                parameter("songmid", mid)
                parameter("g_tk", "5381")
                parameter("format", "json")
                parameter("inCharset", "utf8")
                parameter("outCharset", "utf-8")
                parameter("nobase64", "1")
                parameter("platform", "yqq")
                parameter("needNewCode", "0")
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Referrer, "https://y.qq.com/portal/player.html")
            }
        }.getOrNull() ?: return null
        if (!response.status.isSuccess()) return null

        val root = runCatching { Json.parseToJsonElement(response.bodyAsText()).jsonObject }.getOrNull()
            ?: return null
        if (root["code"]?.jsonPrimitive?.content?.toIntOrNull() !in setOf(0, null)) return null
        val raw = root.str("lyric") ?: return null
        if (!raw.contains("[00:") && !raw.contains("<")) return null
        return LyricsSearchResult(
            provider = provider,
            raw = raw,
            translationRaw = root.str("trans"),
        )
    }


    private fun extractTag(xml: String, tag: String): String? =
        Regex("""<$tag[^>]*>(.*?)</$tag>""", RegexOption.DOT_MATCHES_ALL)
            .find(xml)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /**
     * QQ returns either plain enhanced-LRC or a hex QRC block. A decrypted QRC may still be an
     * XML wrapper with `LyricContent`; unwrap it so the common QRC parser can read it.
     */
    private fun decodePayload(text: String): String? {
        val cleaned = text
            .removePrefix("<![CDATA[")
            .removeSuffix("]]>")
            .trim()
        if (cleaned.isEmpty()) return null
        if (cleaned.startsWith("[") || cleaned.contains("[00:")) {
            return unescapeXml(cleaned)
        }

        val decrypted = QrcDecrypter.decrypt(cleaned) ?: return unescapeXml(cleaned).takeIf { it.contains("[") }
        val lyricContent = Regex("""LyricContent\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
            .find(decrypted)
            ?.groupValues
            ?.get(1)
        return unescapeXml(lyricContent ?: decrypted)
    }

    private fun unescapeXml(text: String): String = text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.cleanHighlight()?.takeIf { it.isNotBlank() }

    private data class QqCandidate(
        val result: LyricsSearchResult,
        val mid: String,
        val songId: String?,
    )

    private companion object {
        const val SEARCH_URL = "https://u6.y.qq.com/cgi-bin/musicu.fcg"
        const val LYRIC_URL = "https://c.y.qq.com/qqmusic/fcgi-bin/lyric_download.fcg"
        const val LRC_URL = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg"
        const val SEARCH_MODULE = "music.search.SearchCgiService"
        const val MOBILE_SEARCH_METHOD = "DoSearchForQQMusicMobile"
        const val DESKTOP_SEARCH_METHOD = "DoSearchForQQMusicDesktop"

        /** Protocol version of QQ Music Android 20.8.5.8, matching [MOBILE_USER_AGENT]. */
        const val MOBILE_CLIENT_VERSION = "20080508"
        const val MAX_LYRIC_ATTEMPTS = 4
        const val MOBILE_USER_AGENT = "QQMusic $MOBILE_CLIENT_VERSION(android)"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }
}
